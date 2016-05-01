package com.iot

import java.util.Date

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import com.iot.KafkaTopics.DATA_TOPIC
import com.iot.WeatherDomain.{AggregateWeatherDataRecord, WeatherDataRecord, WeatherDatabaseRecord}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object WeatherStreamingProcessor extends App {
  implicit val formats = DefaultFormats

  val batchDuration = Seconds(60)

  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName(getClass.getSimpleName)
    .set("spark.cassandra.connection.host", "127.0.0.1")

  val sc = new SparkContext(conf)
  val ssc = new StreamingContext(sc, batchDuration)

  val (keyspace, table) = ("iot", "weather")
  val (zkQuorum, groupId) = ("localhost:2181", "weather-consumer")

  // database
  val (deviceId, timestamp, temperatureStats, pressureStats) =
    ("device_id", "timestamp", "temperature_stats", "pressure_stats") // columns
  val stats = "stats" // type

  // json fields
  val (temperature, pressure) = ("temperature", "pressure")

  CassandraConnector(conf).withSessionDo { session =>
    session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace " +
      s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    session.execute(s"CREATE TYPE $keyspace.$stats (count BIGINT, avg DOUBLE, std_dev DOUBLE)")
    session.execute(s"""CREATE TABLE IF NOT EXISTS $keyspace.$table
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
    .reduceByWindow((r1, r2) => r1.merge(r2), batchDuration, batchDuration)
    .map(toDatabaseRecord)
    .saveToCassandra(keyspace, table)

  ssc.start()

  def parseKafkaRecord(t: (String, String)): WeatherDataRecord = {
    val json = parse(t._2)
    WeatherDataRecord(t._1, (json \ temperature).extract[Double], (json \ pressure).extract[Double])
  }

  def toAggregateRecord(r: WeatherDataRecord) =
    AggregateWeatherDataRecord(r.deviceId, DoubleStatsCounter(r.temperature), DoubleStatsCounter(r.pressure))

  def toDatabaseRecord(r: AggregateWeatherDataRecord) =
    WeatherDatabaseRecord(r.deviceId, new Date(), r.temperatureStats.stats, r.pressureStats.stats)
}
