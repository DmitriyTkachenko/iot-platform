package com.iot

import java.util.Date

object WeatherDomain {
  case class AggregateWeatherDataRecord(deviceId: String,
                                        temperatureStats: DoubleStatsCounter,
                                        pressureStats: DoubleStatsCounter) {
    def merge(anotherRecord: AggregateWeatherDataRecord) = {
      temperatureStats.merge(anotherRecord.temperatureStats)
      pressureStats.merge(anotherRecord.pressureStats)
      this
    }
  }

  case class WeatherDatabaseRecord(deviceId: String, timestamp: Date,
                                   temperatureStats: DoubleStats, pressureStats: DoubleStats)

  case class WeatherDataRecord(deviceId: String, temperature: Double, pressure: Double)
}
