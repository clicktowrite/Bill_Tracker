package com.example.billtracker

import com.example.billtracker.domain.calculator.DailyInterpolationEngine
import com.example.billtracker.domain.calculator.InputReading
import com.example.billtracker.domain.calculator.ReadingStatus
import com.example.billtracker.domain.model.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class DailyInterpolationEngineTest {

    private val millisPerDay = 86_400_000L

    @Test
    fun interpolationExample_matchesExpectedValues() {
        // Oct 1, 2023 00:00:00 GMT
        val oct1 = 1696118400000L
        val oct5 = oct1 + (4 * millisPerDay) // Oct 5

        // Oct 1: 1000 kWh (ACTUAL)
        // Oct 5: 1012 kWh (ACTUAL)
        val actualReadings = listOf(
            InputReading(id = 1, timestamp = oct1, value = "1000.0", hasTime = false),
            InputReading(id = 2, timestamp = oct5, value = "1012.0", hasTime = false)
        )

        val result = DailyInterpolationEngine.interpolateReadings(
            actualReadings = actualReadings,
            forecastDays = 0,
            utilityType = UtilityType.ELECTRICITY
        )

        assertEquals(5, result.size)

        // Oct 1 -> ACTUAL 1000
        assertEquals(ReadingStatus.ACTUAL, result[0].status)
        assertEquals(BigDecimal("1000.00"), result[0].readingValue)
        assertEquals(1L, result[0].actualReadingId)

        // Oct 2 -> ESTIMATED 1003, daily consumption = 3
        assertEquals(ReadingStatus.ESTIMATED, result[1].status)
        assertEquals(BigDecimal("1003.00"), result[1].readingValue)
        assertEquals(BigDecimal("3.00"), result[1].dailyConsumption)
        assertNull(result[1].actualReadingId)

        // Oct 3 -> ESTIMATED 1006, daily consumption = 3
        assertEquals(ReadingStatus.ESTIMATED, result[2].status)
        assertEquals(BigDecimal("1006.00"), result[2].readingValue)
        assertEquals(BigDecimal("3.00"), result[2].dailyConsumption)
        assertNull(result[2].actualReadingId)

        // Oct 4 -> ESTIMATED 1009, daily consumption = 3
        assertEquals(ReadingStatus.ESTIMATED, result[3].status)
        assertEquals(BigDecimal("1009.00"), result[3].readingValue)
        assertEquals(BigDecimal("3.00"), result[3].dailyConsumption)
        assertNull(result[3].actualReadingId)

        // Oct 5 -> ACTUAL 1012
        assertEquals(ReadingStatus.ACTUAL, result[4].status)
        assertEquals(BigDecimal("1012.00"), result[4].readingValue)
        assertEquals(BigDecimal("3.00"), result[4].dailyConsumption)
        assertEquals(2L, result[4].actualReadingId)

        // Verify total consumption across interval = 12 kWh
        val totalConsumption = result.mapNotNull { it.dailyConsumption }.fold(BigDecimal.ZERO, BigDecimal::add)
        assertEquals(BigDecimal("12.00"), totalConsumption)
    }

    @Test
    fun missingBoundaryReadings_producesMissingStatus() {
        val oct1 = 1696118400000L

        // Single reading on Oct 1
        val actualReadings = listOf(
            InputReading(id = 1, timestamp = oct1, value = "1000.0")
        )

        val result = DailyInterpolationEngine.interpolateReadings(
            actualReadings = actualReadings,
            forecastDays = 0
        )

        assertEquals(1, result.size)
        assertEquals(ReadingStatus.ACTUAL, result[0].status)
        assertEquals(BigDecimal("1000.00"), result[0].readingValue)
    }

    @Test
    fun meterReset_preventsInvalidInterpolation() {
        val oct1 = 1696118400000L
        val oct5 = oct1 + (4 * millisPerDay)

        // Decreasing reading with meter reset
        val actualReadings = listOf(
            InputReading(id = 1, timestamp = oct1, value = "1000.0"),
            InputReading(id = 2, timestamp = oct5, value = "50.0", isMeterReset = true)
        )

        val result = DailyInterpolationEngine.interpolateReadings(
            actualReadings = actualReadings,
            forecastDays = 0
        )

        assertEquals(5, result.size)
        assertEquals(ReadingStatus.ACTUAL, result[0].status)

        // Intermediate days should be MISSING due to meter reset boundary
        assertEquals(ReadingStatus.MISSING, result[1].status)
        assertNull(result[1].readingValue)

        assertEquals(ReadingStatus.MISSING, result[2].status)
        assertEquals(ReadingStatus.MISSING, result[3].status)

        assertEquals(ReadingStatus.ACTUAL, result[4].status)
        assertEquals(BigDecimal("50.00"), result[4].readingValue)
    }

    @Test
    fun forecastDays_producesForecastStatus() {
        val oct1 = 1696118400000L
        val oct3 = oct1 + (2 * millisPerDay)

        // Oct 1: 100, Oct 3: 110 (5/day)
        val actualReadings = listOf(
            InputReading(id = 1, timestamp = oct1, value = "100.0"),
            InputReading(id = 2, timestamp = oct3, value = "110.0")
        )

        val result = DailyInterpolationEngine.interpolateReadings(
            actualReadings = actualReadings,
            forecastDays = 2 // Oct 4 & Oct 5 forecast
        )

        assertEquals(5, result.size)
        assertEquals(ReadingStatus.ACTUAL, result[0].status) // Oct 1
        assertEquals(ReadingStatus.ESTIMATED, result[1].status) // Oct 2 (105)
        assertEquals(ReadingStatus.ACTUAL, result[2].status) // Oct 3 (110)
        assertEquals(ReadingStatus.FORECAST, result[3].status) // Oct 4 (115)
        assertEquals(ReadingStatus.FORECAST, result[4].status) // Oct 5 (120)

        assertEquals(BigDecimal("115.00"), result[3].readingValue)
        assertEquals(BigDecimal("120.00"), result[4].readingValue)
    }
}
