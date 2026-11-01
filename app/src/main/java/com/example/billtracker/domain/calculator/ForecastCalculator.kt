package com.example.billtracker.domain.calculator

import com.example.billtracker.domain.model.UtilityType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date

sealed interface ForecastResult {
    data class InsufficientData(val message: String = "Not enough readings to estimate.") : ForecastResult

    data class Success(
        val utilityType: UtilityType,
        val startReadingValue: BigDecimal,
        val latestReadingValue: BigDecimal,
        val observedDays: BigDecimal,
        val totalBillingDays: Int,
        val remainingDays: BigDecimal,
        val verifiedUsage: BigDecimal,
        val averageDailyConsumption: BigDecimal,
        val projectedRemainingConsumption: BigDecimal,
        val projectedConsumption: BigDecimal,
        val estimatedRate: BigDecimal?,
        val projectedBill: BigDecimal?,
        val isRateDynamic: Boolean = false,
        val latestReadingDate: Long = System.currentTimeMillis(),
        val latestReadingHasTime: Boolean = false
    ) : ForecastResult
}

data class ForecastInputReading(
    val timestamp: Long,
    val value: String,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false
)

object ForecastCalculator {

    private const val MILLIS_PER_DAY = 86_400_000.0

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

    /**
     * Calculates time-normalized consumption and bill projection safely without throwing NaN/Infinity or ArithmeticExceptions.
     */
    fun calculateForecast(
        readings: List<ForecastInputReading>,
        utilityType: UtilityType,
        rateStr: String?,
        isRateDynamic: Boolean = false,
        totalBillingDays: Int = 30
    ): ForecastResult {
        return try {
            if (readings.size < 2) {
                return ForecastResult.InsufficientData("Not enough readings to estimate.")
            }

            // Sort by timestamp ascending
            val sorted = readings.sortedBy { it.timestamp }
            val first = sorted.first()
            val last = sorted.last()

            val firstVal = parseBigDecimal(first.value)
                ?: return ForecastResult.InsufficientData("Invalid reading data.")
            val lastVal = parseBigDecimal(last.value)
                ?: return ForecastResult.InsufficientData("Invalid reading data.")

            if (lastVal < firstVal) {
                return ForecastResult.InsufficientData("Latest reading is lower than start reading.")
            }

            val verifiedUsage = lastVal.subtract(firstVal).setScale(2, RoundingMode.HALF_UP)

            // Calculate observed duration in days
            val observedDaysDouble: Double = if (first.hasTime && last.hasTime) {
                val millisDiff = last.timestamp - first.timestamp
                if (millisDiff <= 0) 0.0 else millisDiff / MILLIS_PER_DAY
            } else {
                calendarDaysBetween(first.timestamp, last.timestamp).toDouble()
            }

            if (observedDaysDouble.isNaN() || observedDaysDouble.isInfinite() || observedDaysDouble <= 0.0001) {
                return ForecastResult.InsufficientData("Observation interval is too short to estimate.")
            }

            val observedDays = BigDecimal.valueOf(observedDaysDouble).setScale(2, RoundingMode.HALF_UP)
            val billingDaysBd = BigDecimal(totalBillingDays)

            // Average daily consumption = verifiedUsage / observedDays
            val avgDaily = if (observedDays > BigDecimal.ZERO) {
                verifiedUsage.divide(observedDays, 4, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            // Remaining days
            val remaining = billingDaysBd.subtract(observedDays)
            val remainingDays = if (remaining < BigDecimal.ZERO) BigDecimal.ZERO else remaining

            // Projected remaining consumption = avgDaily * remainingDays
            val projectedRemaining = avgDaily.multiply(remainingDays).setScale(2, RoundingMode.HALF_UP)

            // Projected total consumption = verifiedUsage + projectedRemaining
            val projectedTotalConsumption = verifiedUsage.add(projectedRemaining).setScale(2, RoundingMode.HALF_UP)

            // Rate & Bill
            val rate = parseBigDecimal(rateStr)
            val projectedBill = if (rate != null && rate > BigDecimal.ZERO) {
                projectedTotalConsumption.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            } else null

            ForecastResult.Success(
                utilityType = utilityType,
                startReadingValue = firstVal,
                latestReadingValue = lastVal,
                observedDays = observedDays,
                totalBillingDays = totalBillingDays,
                remainingDays = remainingDays,
                verifiedUsage = verifiedUsage,
                averageDailyConsumption = avgDaily.setScale(2, RoundingMode.HALF_UP),
                projectedRemainingConsumption = projectedRemaining,
                projectedConsumption = projectedTotalConsumption,
                estimatedRate = rate?.setScale(2, RoundingMode.HALF_UP),
                projectedBill = projectedBill,
                isRateDynamic = isRateDynamic,
                latestReadingDate = last.timestamp,
                latestReadingHasTime = last.hasTime
            )
        } catch (_: Exception) {
            ForecastResult.InsufficientData("Unable to calculate estimate due to invalid data.")
        }
    }

    private fun calendarDaysBetween(startMillis: Long, endMillis: Long): Int {
        val startCal = Calendar.getInstance().apply { time = Date(startMillis) }
        val endCal = Calendar.getInstance().apply { time = Date(endMillis) }

        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        val diff = endCal.timeInMillis - startCal.timeInMillis
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        return if (days <= 0) 1 else days
    }
}
