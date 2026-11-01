package com.example.billtracker

import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.calculator.CalculationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BillingCalculatorTest {

    @Test
    fun electricityFixedRateCalculation_success() {
        // Previous = 1250, Current = 1300, Usage = 50, Rate = 12
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "1250",
            currentReadingStr = "1300",
            fixedRateStr = "12"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("50.00"), success.usage)
        assertEquals(BigDecimal("12.00"), success.effectiveRate)
        assertEquals(BigDecimal("600.00"), success.totalBill)
    }

    @Test
    fun waterFixedRateCalculation_success() {
        // Previous = 250, Current = 258, Usage = 8, Rate = 35
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "250",
            currentReadingStr = "258",
            fixedRateStr = "35"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("8.00"), success.usage)
        assertEquals(BigDecimal("35.00"), success.effectiveRate)
        assertEquals(BigDecimal("280.00"), success.totalBill)
    }

    @Test
    fun dynamicElectricityRateCalculation_success() {
        // Provider Bill = 4850, Provider Usage = 350 -> Rate = 13.8571 -> 13.86
        // Tenant Usage = 75 (1250 to 1325) -> Bill = 75 * 13.8571 = 1039.28 or 1039.50
        val (rate, error) = BillingCalculator.calculateDynamicRate(
            providerTotalBillStr = "4850",
            providerTotalUsageStr = "350"
        )

        assertEquals(null, error)
        assertEquals(BigDecimal("13.8571"), rate)

        val result = BillingCalculator.calculateDynamicRateBill(
            previousReadingStr = "1250",
            currentReadingStr = "1325",
            providerTotalBillStr = "4850",
            providerTotalUsageStr = "350"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("75.00"), success.usage)
        assertEquals(BigDecimal("1039.28"), success.totalBill)
    }

    @Test
    fun dynamicWaterRateCalculation_success() {
        // Provider Bill = 2400, Usage = 60 -> Rate = 40
        // Tenant Usage = 8 (250 to 258) -> Bill = 320
        val result = BillingCalculator.calculateDynamicRateBill(
            previousReadingStr = "250",
            currentReadingStr = "258",
            providerTotalBillStr = "2400",
            providerTotalUsageStr = "60"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("8.00"), success.usage)
        assertEquals(BigDecimal("40.00"), success.effectiveRate)
        assertEquals(BigDecimal("320.00"), success.totalBill)
    }

    @Test
    fun decimalValues_handling() {
        // Readings: 1250.5 to 1300.75 -> Usage = 50.25
        // Rate: 12.50 -> Bill = 628.125 -> 628.13
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "1250.5",
            currentReadingStr = "1300.75",
            fixedRateStr = "12.50"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("50.25"), success.usage)
        assertEquals(BigDecimal("628.13"), success.totalBill)
    }

    @Test
    fun zeroProviderUsage_returnsError() {
        val (rate, error) = BillingCalculator.calculateDynamicRate(
            providerTotalBillStr = "1000",
            providerTotalUsageStr = "0"
        )

        assertEquals(null, rate)
        assertEquals("Provider total usage must be greater than zero.", error)
    }

    @Test
    fun negativeValues_returnsNullOrError() {
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "-100",
            currentReadingStr = "200",
            fixedRateStr = "10"
        )

        assertTrue(result is CalculationResult.Error)
        assertEquals("Previous reading must be a valid non-negative number.", (result as CalculationResult.Error).message)
    }

    @Test
    fun currentReadingBelowPrevious_returnsError() {
        val result = BillingCalculator.calculateUsage(
            previousReadingStr = "1000",
            currentReadingStr = "950"
        )

        assertTrue(result is CalculationResult.Error)
        assertEquals("Current reading cannot be lower than the previous reading.", (result as CalculationResult.Error).message)
    }

    @Test
    fun largeMonetaryValues_accurateCalculation() {
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "1000000.00",
            currentReadingStr = "1050000.00",
            fixedRateStr = "15.75"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("50000.00"), success.usage)
        assertEquals(BigDecimal("787500.00"), success.totalBill)
    }

    @Test
    fun roundingBehavior_halfUp() {
        // Usage 1, Rate 10.005 -> 10.01
        val result = BillingCalculator.calculateFixedRateBill(
            previousReadingStr = "0",
            currentReadingStr = "1",
            fixedRateStr = "10.005"
        )

        assertTrue(result is CalculationResult.Success)
        val success = result as CalculationResult.Success
        assertEquals(BigDecimal("10.01"), success.totalBill)
    }
}
