package com.iot.weather.stream

import java.util.Date

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import com.iot.mq.KafkaTopics.DATA_TOPIC
import com.iot.util.DoubleStatsCounter
import com.iot.weather.WeatherDomain._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.json4s._
import org.json4s.jackson.JsonMethods._

object WeatherStreamingProcessor extends App {
  implicit val formats = DefaultFormats

  val batchDuration = Seconds(60)

  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName(getClass.getSimpleName)
    .set("spark.cassandra.connection.host", "127.0.0.1")

  val sc = new SparkContext(conf)
  val ssc = new StreamingContext(sc, batchDuration)

  type Key = String
  val (zkQuorum, groupId) = ("localhost:2181", "weather-consumer")

  val stats = "stats" // type

  // json fields
  val (temperature, pressure) = ("temperature", "pressure")

  CassandraConnector(conf).withSessionDo { session =>
    session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace " +
      s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    session.execute(s"CREATE TYPE $keyspace.$stats ($count BIGINT, $avg DOUBLE, $stdDev DOUBLE)")
    session.execute(
      s"""CREATE TABLE IF NOT EXISTS $keyspace.$table
      ($deviceId TEXT, $timestamp TIMESTAMP,
      $temperatureStats FROZEN<stats>,
      $pressureStats FROZEN<stats>,
      PRIMARY KEY ($deviceId, $timestamp))""")
    session.execute(s"TRUNCATE $keyspace.$table")
  }

  val stream = KafkaUtils.createStream(ssc, zkQuorum, groupId, Map(DATA_TOPIC -> 1), StorageLevel.MEMORY_ONLY)

  stream
    .map(parseKafkaRecord)
    .map(toAggregateRecord)
    .reduceByKeyAndWindow((r1: AggregateWeatherDataRecord, r2: AggregateWeatherDataRecord) => r1.merge(r2),
      batchDuration, batchDuration)
    .map(toDatabaseRecord)
    .saveToCassandra(keyspace, table)

  ssc.start()

  def parseKafkaRecord(t: (String, String)): (Key, WeatherDataRecord) = {
    val json = parse(t._2)
    (t._1, WeatherDataRecord((json \ temperature).extract[Double], (json \ pressure).extract[Double]))
  }

  def toAggregateRecord(t: (Key, WeatherDataRecord)): (Key, AggregateWeatherDataRecord) =
    (t._1, AggregateWeatherDataRecord(DoubleStatsCounter(t._2.temperature), DoubleStatsCounter(t._2.pressure)))

  def toDatabaseRecord(t: (Key, AggregateWeatherDataRecord)) =
    WeatherDatabaseRecord(t._1, new Date(), t._2.temperatureStats.stats, t._2.pressureStats.stats)
}
