package com.iot.weather.http

import com.datastax.spark.connector._
import com.iot.http.HttpUtils.{extractQueryParameter, sendServerError}
import com.iot.weather.WeatherDomain._
import io.undertow.server.{HttpHandler, HttpServerExchange}
import org.apache.spark.{SparkConf, SparkContext}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ReportHttpHandler extends HttpHandler {
  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName(getClass.getSimpleName)
    .set("spark.cassandra.connection.host", "127.0.0.1")

  val sc = new SparkContext(conf)

  implicit val formats = Serialization.formats(NoTypeHints)

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    var weatherTable = sc.cassandraTable[WeatherDatabaseRecord](keyspace, table)
    Option(extractQueryParameter("id", exchange.getQueryParameters))
      .foreach(id => weatherTable = weatherTable.where(s"$deviceId = ?", id))
    exchange.dispatch
    val rowsFuture = weatherTable.collectAsync()
    rowsFuture.onComplete {
      case Success(rows) => exchange.getResponseSender.send(write(rows))
      case Failure(e) => sendServerError(exchange)
    }
  }
}
