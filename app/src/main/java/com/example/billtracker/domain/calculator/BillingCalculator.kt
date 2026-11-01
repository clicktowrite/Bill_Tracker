package com.example.billtracker.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface CalculationResult {
    data class Success(
        val usage: BigDecimal,
        val effectiveRate: BigDecimal,
        val totalBill: BigDecimal
    ) : CalculationResult

    data class Error(val message: String) : CalculationResult
}

object BillingCalculator {

    private const val CURRENCY_SCALE = 2
    private const val RATE_SCALE = 4

    /**
     * Parse string to BigDecimal safely. Returns null if invalid or negative.
     */
    fun parseBigDecimal(input: String?): BigDecimal? {
        if (input.isNullOrBlank()) return null
        return try {
            val cleaned = input.trim().replace(",", "")
            val decimal = BigDecimal(cleaned)
            if (decimal < BigDecimal.ZERO) null else decimal
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates usage given current and previous readings.
     */
    fun calculateUsage(previousReadingStr: String, currentReadingStr: String): CalculationResult {
        val prev = parseBigDecimal(previousReadingStr)
            ?: return CalculationResult.Error("Previous reading must be a valid non-negative number.")
        val curr = parseBigDecimal(currentReadingStr)
            ?: return CalculationResult.Error("Current reading must be a valid non-negative number.")

        if (curr < prev) {
            return CalculationResult.Error("Current reading cannot be lower than the previous reading.")
        }

        val usage = curr.subtract(prev).setScale(2, RoundingMode.HALF_UP)
        return CalculationResult.Success(
            usage = usage,
            effectiveRate = BigDecimal.ZERO,
            totalBill = BigDecimal.ZERO
        )
    }

    /**
     * Calculates bill using a fixed rate per unit.
     */
    fun calculateFixedRateBill(
        previousReadingStr: String,
        currentReadingStr: String,
        fixedRateStr: String
    ): CalculationResult {
        val usageResult = calculateUsage(previousReadingStr, currentReadingStr)
        if (usageResult is CalculationResult.Error) return usageResult

        val usage = (usageResult as CalculationResult.Success).usage

        val rate = parseBigDecimal(fixedRateStr)
            ?: return CalculationResult.Error("Rate per unit must be a valid non-negative number.")

        val total = usage.multiply(rate).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)

        return CalculationResult.Success(
            usage = usage,
            effectiveRate = rate.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP),
            totalBill = total
        )
    }

    /**
     * Calculates dynamic rate per unit: Provider Total Bill / Provider Total Usage.
     */
    fun calculateDynamicRate(
        providerTotalBillStr: String,
        providerTotalUsageStr: String
    ): Pair<BigDecimal?, String?> {
        val bill = parseBigDecimal(providerTotalBillStr)
            ?: return Pair(null, "Total provider bill must be a valid non-negative number.")
        val usage = parseBigDecimal(providerTotalUsageStr)
            ?: return Pair(null, "Total provider usage must be a valid number.")

        if (usage <= BigDecimal.ZERO) {
            return Pair(null, "Provider total usage must be greater than zero.")
        }

        val rate = bill.divide(usage, RATE_SCALE, RoundingMode.HALF_UP)
        return Pair(rate, null)
    }

    /**
     * Calculates bill using dynamic rate derived from provider inputs.
     */
    fun calculateDynamicRateBill(
        previousReadingStr: String,
        currentReadingStr: String,
        providerTotalBillStr: String,
        providerTotalUsageStr: String
    ): CalculationResult {
        val usageResult = calculateUsage(previousReadingStr, currentReadingStr)
        if (usageResult is CalculationResult.Error) return usageResult

        val usage = (usageResult as CalculationResult.Success).usage

        val (rate, rateError) = calculateDynamicRate(providerTotalBillStr, providerTotalUsageStr)
        if (rateError != null || rate == null) {
            return CalculationResult.Error(rateError ?: "Invalid provider rate inputs.")
        }

        val total = usage.multiply(rate).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)

        return CalculationResult.Success(
            usage = usage,
            effectiveRate = rate.setScale(2, RoundingMode.HALF_UP),
            totalBill = total
        )
    }
}
