package com.example.billtracker.domain.calculator

import java.math.BigDecimal
import java.util.Calendar

sealed class MissingReadingValidationResult {
    object Valid : MissingReadingValidationResult()
    data class Invalid(val message: String) : MissingReadingValidationResult()
}

object MissingReadingValidator {

    data class ActualReadingPoint(
        val id: Long,
        val timestamp: Long,
        val value: BigDecimal
    )

    private const val ONE_DAY_MILLIS = 86_400_000L

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (Math.abs(t1 - t2) < 1000L) return true
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun validate(
        newTimestamp: Long,
        newValue: BigDecimal,
        actualReadings: List<ActualReadingPoint>,
        isEditMode: Boolean = false,
        excludeId: Long? = null
    ): MissingReadingValidationResult {
        val filtered = if (excludeId != null) actualReadings.filter { it.id != excludeId } else actualReadings

        if (filtered.isEmpty()) {
            return MissingReadingValidationResult.Valid
        }

        // Duplicate check: only flag if dates are the same day (or identical timestamp)
        val isTimestampsRealistic = newTimestamp > ONE_DAY_MILLIS
        val duplicate = if (isTimestampsRealistic) {
            filtered.any { isSameDay(it.timestamp, newTimestamp) }
        } else {
            filtered.any { it.timestamp == newTimestamp }
        }

        if (duplicate) {
            return MissingReadingValidationResult.Invalid("An actual reading already exists on this date.")
        }

        val sorted = filtered.sortedBy { it.timestamp }
        val predecessor = sorted.filter { it.timestamp < newTimestamp }.maxByOrNull { it.timestamp }
        val successor = sorted.filter { it.timestamp > newTimestamp }.minByOrNull { it.timestamp }

        if (!isEditMode) {
            // For inserting a missing reading into a historical sequence, both neighbors are required
            if (predecessor == null || successor == null) {
                return MissingReadingValidationResult.Invalid("Reading date must fall between the neighboring readings.")
            }
        }

        // Numerical validation
        if (predecessor != null && newValue <= predecessor.value) {
            return MissingReadingValidationResult.Invalid("Reading must be greater than the previous actual reading.")
        }

        if (successor != null && newValue >= successor.value) {
            return MissingReadingValidationResult.Invalid("Reading must be lower than the next actual reading.")
        }

        return MissingReadingValidationResult.Valid
    }
}
