package com.example.billtracker.domain.calculator

import com.example.billtracker.domain.model.UtilityType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date

enum class ReadingStatus(val displayName: String) {
    ACTUAL("ACTUAL"),
    ESTIMATED("ESTIMATED"),
    FORECAST("FORECAST"),
    MISSING("MISSING")
}

data class InputReading(
    val id: Long = 0,
    val timestamp: Long,
    val value: String,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val notes: String? = null
)

data class InterpolatedDailyReading(
    val dateMillis: Long,
    val readingValue: BigDecimal?,
    val status: ReadingStatus,
    val dailyConsumption: BigDecimal?,
    val hasTime: Boolean = false,
    val actualReadingId: Long? = null,
    val notes: String? = null
)

object DailyInterpolationEngine {

    private const val MILLIS_PER_DAY = 86_400_000L

    private fun parseBigDecimal(input: String?): BigDecimal? {
        if (input.isNullOrBlank()) return null
        return try {
            val cleaned = input.trim().replace(",", "")
            val decimal = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            if (decimal < BigDecimal.ZERO) null else decimal
        } catch (_: Exception) {
            null
        }
    }

    private fun startOfDayMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            time = Date(timestamp)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun calendarDaysBetween(startMillis: Long, endMillis: Long): Int {
        val startDay = startOfDayMillis(startMillis)
        val endDay = startOfDayMillis(endMillis)
        val diff = endDay - startDay
        val days = (diff / MILLIS_PER_DAY).toInt()
        return if (days <= 0) 0 else days
    }

    /**
     * Interpolates daily cumulative readings safely without throwing Division-by-Zero or ArithmeticExceptions.
     */
    fun interpolateReadings(
        actualReadings: List<InputReading>,
        forecastDays: Int = 0,
        utilityType: UtilityType = UtilityType.ELECTRICITY
    ): List<InterpolatedDailyReading> {
        return try {
            if (actualReadings.isEmpty()) return emptyList()

            val sorted = actualReadings
                .mapNotNull { r ->
                    val bd = parseBigDecimal(r.value)
                    if (bd != null) r to bd else null
                }
                .sortedBy { it.first.timestamp }

            if (sorted.isEmpty()) return emptyList()

            val result = mutableListOf<InterpolatedDailyReading>()

            // Group actual readings by calendar day
            val actualByDay = mutableMapOf<Long, Pair<InputReading, BigDecimal>>()
            sorted.forEach { (reading, value) ->
                val dayStart = startOfDayMillis(reading.timestamp)
                actualByDay[dayStart] = reading to value
            }

            val sortedDays = actualByDay.keys.sorted()
            if (sortedDays.isEmpty()) return emptyList()

            val firstDay = sortedDays.first()
            val lastActualDay = sortedDays.last()

            var currentCal = Calendar.getInstance().apply { timeInMillis = firstDay }
            val endDay = if (forecastDays > 0) {
                startOfDayMillis(lastActualDay + (forecastDays * MILLIS_PER_DAY))
            } else {
                lastActualDay
            }

            var prevValue: BigDecimal? = null

            while (currentCal.timeInMillis <= endDay) {
                val currentDay = currentCal.timeInMillis

                if (currentDay <= lastActualDay) {
                    if (actualByDay.containsKey(currentDay)) {
                        val (actualReading, actualVal) = actualByDay[currentDay]!!
                        val consumption = if (prevValue != null) {
                            if (actualReading.isMeterReset || actualVal < prevValue) {
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            } else {
                                actualVal.subtract(prevValue).setScale(2, RoundingMode.HALF_UP)
                            }
                        } else {
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        }

                        result.add(
                            InterpolatedDailyReading(
                                dateMillis = actualReading.timestamp,
                                readingValue = actualVal,
                                status = ReadingStatus.ACTUAL,
                                dailyConsumption = consumption,
                                hasTime = actualReading.hasTime,
                                actualReadingId = actualReading.id,
                                notes = actualReading.notes
                            )
                        )
                        prevValue = actualVal
                    } else {
                        val prevActualDay = sortedDays.lastOrNull { it < currentDay }
                        val nextActualDay = sortedDays.firstOrNull { it > currentDay }

                        if (prevActualDay != null && nextActualDay != null) {
                            val (_, prevVal) = actualByDay[prevActualDay]!!
                            val (nextReading, nextVal) = actualByDay[nextActualDay]!!

                            if (nextReading.isMeterReset || nextVal < prevVal) {
                                result.add(
                                    InterpolatedDailyReading(
                                        dateMillis = currentDay,
                                        readingValue = null,
                                        status = ReadingStatus.MISSING,
                                        dailyConsumption = null
                                    )
                                )
                            } else {
                                val totalDays = calendarDaysBetween(prevActualDay, nextActualDay)
                                val elapsedDays = calendarDaysBetween(prevActualDay, currentDay)

                                if (totalDays > 0) {
                                    val totalDiff = nextVal.subtract(prevVal)
                                    val fraction = BigDecimal(elapsedDays).divide(BigDecimal(totalDays), 6, RoundingMode.HALF_UP)
                                    val interpolatedVal = prevVal.add(totalDiff.multiply(fraction)).setScale(2, RoundingMode.HALF_UP)
                                    val dailyCons = totalDiff.divide(BigDecimal(totalDays), 2, RoundingMode.HALF_UP)

                                    result.add(
                                        InterpolatedDailyReading(
                                            dateMillis = currentDay,
                                            readingValue = interpolatedVal,
                                            status = ReadingStatus.ESTIMATED,
                                            dailyConsumption = dailyCons
                                        )
                                    )
                                    prevValue = interpolatedVal
                                } else {
                                    result.add(
                                        InterpolatedDailyReading(
                                            dateMillis = currentDay,
                                            readingValue = null,
                                            status = ReadingStatus.MISSING,
                                            dailyConsumption = null
                                        )
                                    )
                                }
                            }
                        } else {
                            result.add(
                                InterpolatedDailyReading(
                                    dateMillis = currentDay,
                                    readingValue = null,
                                    status = ReadingStatus.MISSING,
                                    dailyConsumption = null
                                )
                            )
                        }
                    }
                } else {
                    // Beyond latest actual reading -> FORECAST
                    val lastActualVal = actualByDay[lastActualDay]?.second
                    if (lastActualVal != null && sortedDays.size >= 2) {
                        val firstActualVal = actualByDay[firstDay]?.second ?: lastActualVal
                        val totalObservedDays = calendarDaysBetween(firstDay, lastActualDay)

                        if (totalObservedDays > 0 && lastActualVal >= firstActualVal) {
                            val totalUsage = lastActualVal.subtract(firstActualVal)
                            val avgDailyUsage = totalUsage.divide(BigDecimal(totalObservedDays), 4, RoundingMode.HALF_UP)

                            val daysBeyond = calendarDaysBetween(lastActualDay, currentDay)
                            val projectedVal = lastActualVal.add(avgDailyUsage.multiply(BigDecimal(daysBeyond))).setScale(2, RoundingMode.HALF_UP)
                            val projectedDailyCons = avgDailyUsage.setScale(2, RoundingMode.HALF_UP)

                            result.add(
                                InterpolatedDailyReading(
                                    dateMillis = currentDay,
                                    readingValue = projectedVal,
                                    status = ReadingStatus.FORECAST,
                                    dailyConsumption = projectedDailyCons
                                )
                            )
                        } else {
                            result.add(
                                InterpolatedDailyReading(
                                    dateMillis = currentDay,
                                    readingValue = null,
                                    status = ReadingStatus.MISSING,
                                    dailyConsumption = null
                                )
                            )
                        }
                    } else {
                        result.add(
                            InterpolatedDailyReading(
                                dateMillis = currentDay,
                                readingValue = null,
                                status = ReadingStatus.MISSING,
                                dailyConsumption = null
                            )
                        )
                    }
                }

                currentCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
