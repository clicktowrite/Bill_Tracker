package com.example.billtracker.domain.calculator

import java.math.BigDecimal

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

    fun validate(
        newTimestamp: Long,
        newValue: BigDecimal,
        actualReadings: List<ActualReadingPoint>
    ): MissingReadingValidationResult {
        if (actualReadings.isEmpty()) {
            return MissingReadingValidationResult.Valid
        }

        val duplicate = actualReadings.any { it.timestamp == newTimestamp }
        if (duplicate) {
            return MissingReadingValidationResult.Invalid("An actual reading already exists on this date.")
        }

        val sorted = actualReadings.sortedBy { it.timestamp }
        val predecessor = sorted.filter { it.timestamp < newTimestamp }.maxByOrNull { it.timestamp }
        val successor = sorted.filter { it.timestamp > newTimestamp }.minByOrNull { it.timestamp }

        if (predecessor == null || successor == null) {
            return MissingReadingValidationResult.Invalid("Reading date must fall between the neighboring readings.")
        }

        if (newValue <= predecessor.value) {
            return MissingReadingValidationResult.Invalid("Reading must be greater than the previous actual reading.")
        }

        if (newValue >= successor.value) {
            return MissingReadingValidationResult.Invalid("Reading must be lower than the next actual reading.")
        }

        return MissingReadingValidationResult.Valid
    }
}
