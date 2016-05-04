package com.iot.weather

import java.util.Date

import com.iot.util.{DoubleStats, DoubleStatsCounter}

object WeatherDomain {
  // db
  val (keyspace, table) = ("iot", "weather")

  // columns
  val (deviceId, timestamp, temperatureStats, pressureStats) = ("device_id", "timestamp", "temperature_stats", "pressure_stats")
  val (count, avg, stdDev) = ("count", "avg", "std_dev")

  case class AggregateWeatherDataRecord(temperatureStats: DoubleStatsCounter,
                                        pressureStats: DoubleStatsCounter) {
    def merge(anotherRecord: AggregateWeatherDataRecord) = {
      temperatureStats.merge(anotherRecord.temperatureStats)
      pressureStats.merge(anotherRecord.pressureStats)
      this
    }
  }

  case class WeatherDatabaseRecord(deviceId: String, timestamp: Date,
                                   temperatureStats: DoubleStats, pressureStats: DoubleStats)

  case class WeatherDataRecord(temperature: Double, pressure: Double)
}
