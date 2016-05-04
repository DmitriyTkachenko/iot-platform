package com.iot.util

import scala.math.sqrt

case class DoubleStats(count: Long, avg: Double, stdDev: Double)

case class DoubleStatsCounter(var n: Long = 0, var sum: Double = 0.0, var sumOfSquares: Double = 0.0) {
  def add(number: Double): DoubleStatsCounter = {
    n += 1
    sum += number
    sumOfSquares += number * number
    this
  }

  def merge(anotherCounter: DoubleStatsCounter): DoubleStatsCounter = {
    n += anotherCounter.n
    sum += anotherCounter.sum
    sumOfSquares += anotherCounter.sumOfSquares
    this
  }

  def avg = sum / n

  def stdDev = sqrt(sumOfSquares / n - avg * avg)

  def stats = DoubleStats(n, avg, stdDev)
}

object DoubleStatsCounter {
  def apply(number: Double): DoubleStatsCounter = new DoubleStatsCounter().add(number)
}
