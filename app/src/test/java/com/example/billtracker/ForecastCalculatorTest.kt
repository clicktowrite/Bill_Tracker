package com.example.billtracker

import com.example.billtracker.domain.calculator.ForecastCalculator
import com.example.billtracker.domain.calculator.ForecastInputReading
import com.example.billtracker.domain.calculator.ForecastResult
import com.example.billtracker.domain.model.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ForecastCalculatorTest {

    @Test
    fun insufficientData_returnsInsufficientResult() {
        val readings = listOf(
            ForecastInputReading(timestamp = 1000000L, value = "100.0")
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.ELECTRICITY,
            rateStr = "12.00"
        )

        assertTrue(result is ForecastResult.InsufficientData)
    }

    @Test
    fun octoberSection41Example_success() {
        // Oct 1 to Oct 11 (10 days elapsed)
        val oct1 = 1696118400000L
        val oct11 = oct1 + (10 * 86400000L)

        // Starting reading = 100.0, Oct 11 reading = 160.0
        val readings = listOf(
            ForecastInputReading(timestamp = oct1, value = "100.0", hasTime = false),
            ForecastInputReading(timestamp = oct11, value = "160.0", hasTime = false)
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.ELECTRICITY,
            rateStr = "12.00",
            totalBillingDays = 30
        )

        assertTrue(result is ForecastResult.Success)
        val success = result as ForecastResult.Success

        assertEquals(BigDecimal("100.00"), success.startReadingValue)
        assertEquals(BigDecimal("160.00"), success.latestReadingValue)
        assertEquals(BigDecimal("60.00"), success.verifiedUsage) // 160 - 100 = 60
        assertEquals(BigDecimal("10.00"), success.observedDays)
        assertEquals(BigDecimal("20.00"), success.remainingDays)
        assertEquals(BigDecimal("6.00"), success.averageDailyConsumption) // 60 / 10 = 6
        assertEquals(BigDecimal("120.00"), success.projectedRemainingConsumption) // 6 * 20 = 120
        assertEquals(BigDecimal("180.00"), success.projectedConsumption) // 60 + 120 = 180
        assertEquals(BigDecimal("2160.00"), success.projectedBill) // 180 * 12 = 2160
    }

    @Test
    fun electricityFixedRateForecast_success() {
        val day1 = 1700000000000L
        val day10 = day1 + (86400000L * 10) // 10 days later

        val readings = listOf(
            ForecastInputReading(timestamp = day1, value = "1000.0", hasTime = true),
            ForecastInputReading(timestamp = day10, value = "1050.0", hasTime = true)
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.ELECTRICITY,
            rateStr = "12.00",
            totalBillingDays = 30
        )

        assertTrue(result is ForecastResult.Success)
        val success = result as ForecastResult.Success

        assertEquals(BigDecimal("10.00"), success.observedDays)
        assertEquals(BigDecimal("20.00"), success.remainingDays)
        assertEquals(BigDecimal("50.00"), success.verifiedUsage)
        assertEquals(BigDecimal("5.00"), success.averageDailyConsumption)
        assertEquals(BigDecimal("150.00"), success.projectedConsumption)
        assertEquals(BigDecimal("1800.00"), success.projectedBill)
    }

    @Test
    fun waterFixedRateForecast_success() {
        val day1 = 1700000000000L
        val day10 = day1 + (86400000L * 10)

        val readings = listOf(
            ForecastInputReading(timestamp = day1, value = "250.0", hasTime = true),
            ForecastInputReading(timestamp = day10, value = "251.5", hasTime = true)
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.WATER,
            rateStr = "40.00",
            totalBillingDays = 30
        )

        assertTrue(result is ForecastResult.Success)
        val success = result as ForecastResult.Success

        assertEquals(BigDecimal("1.50"), success.verifiedUsage)
        assertEquals(BigDecimal("0.15"), success.averageDailyConsumption)
        assertEquals(BigDecimal("4.50"), success.projectedConsumption)
        assertEquals(BigDecimal("180.00"), success.projectedBill)
    }

    @Test
    fun zeroConsumption_returnsZeroForecast() {
        val day1 = 1700000000000L
        val day5 = day1 + (86400000L * 5)

        val readings = listOf(
            ForecastInputReading(timestamp = day1, value = "100.0", hasTime = true),
            ForecastInputReading(timestamp = day5, value = "100.0", hasTime = true)
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.ELECTRICITY,
            rateStr = "10.00"
        )

        assertTrue(result is ForecastResult.Success)
        val success = result as ForecastResult.Success

        assertEquals(BigDecimal("0.00"), success.verifiedUsage)
        assertEquals(BigDecimal("0.00"), success.projectedConsumption)
        assertEquals(BigDecimal("0.00"), success.projectedBill)
    }

    @Test
    fun missingRate_returnsConsumptionWithoutBill() {
        val day1 = 1700000000000L
        val day10 = day1 + (86400000L * 10)

        val readings = listOf(
            ForecastInputReading(timestamp = day1, value = "1000.0", hasTime = true),
            ForecastInputReading(timestamp = day10, value = "1050.0", hasTime = true)
        )

        val result = ForecastCalculator.calculateForecast(
            readings = readings,
            utilityType = UtilityType.ELECTRICITY,
            rateStr = null
        )

        assertTrue(result is ForecastResult.Success)
        val success = result as ForecastResult.Success

        assertEquals(BigDecimal("150.00"), success.projectedConsumption)
        assertEquals(null, success.projectedBill)
    }
}
