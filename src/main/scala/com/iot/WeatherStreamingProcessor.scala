package com.iot

import java.util.Date

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object WeatherStreamingProcessor extends App {
  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName(getClass.getSimpleName)
    .set("spark.cassandra.connection.host", "127.0.0.1")

  val sc = new SparkContext(conf)
  val ssc =  new StreamingContext(sc, Seconds(60))

  val (keyspace, table) = ("iot", "weather")
  val (zkQuorum, groupId) = ("localhost:2181", "weather-consumer")
  val (deviceId, timestamp) = ("device_id", "timestamp") // db fields

  CassandraConnector(conf).withSessionDo { session =>
    session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace " +
      s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.$table " +
      s"($deviceId TEXT, $timestamp TIMESTAMP, data TEXT, PRIMARY KEY ($deviceId, $timestamp))")
    session.execute(s"TRUNCATE $keyspace.$table")
  }

  val stream = KafkaUtils.createStream(ssc, zkQuorum, groupId, Map(KafkaTopics.DATA_TOPIC -> 1),
    StorageLevel.MEMORY_ONLY)

  stream.map(t => (t._1, new Date(), t._2)).saveToCassandra(keyspace, table)

  ssc.start()
}
