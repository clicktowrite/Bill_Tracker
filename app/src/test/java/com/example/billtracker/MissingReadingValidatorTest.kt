package com.example.billtracker

import com.example.billtracker.domain.calculator.MissingReadingValidationResult
import com.example.billtracker.domain.calculator.MissingReadingValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MissingReadingValidatorTest {

    @Test
    fun testValidInsertionBetweenActualReadings() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120")),
            MissingReadingValidator.ActualReadingPoint(3L, 10000L, BigDecimal("150"))
        )

        // Insert at 3000L with value 110 (between 1000L/100 and 5000L/120)
        val result = MissingReadingValidator.validate(3000L, BigDecimal("110"), actuals)
        assertTrue(result is MissingReadingValidationResult.Valid)
    }

    @Test
    fun testRejectReadingLowerThanPreviousActualReading() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        val result = MissingReadingValidator.validate(3000L, BigDecimal("90"), actuals)
        assertTrue(result is MissingReadingValidationResult.Invalid)
        assertEquals("Reading must be greater than the previous actual reading.", (result as MissingReadingValidationResult.Invalid).message)
    }

    @Test
    fun testRejectReadingEqualToPreviousActualReading() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        val result = MissingReadingValidator.validate(3000L, BigDecimal("100"), actuals)
        assertTrue(result is MissingReadingValidationResult.Invalid)
        assertEquals("Reading must be greater than the previous actual reading.", (result as MissingReadingValidationResult.Invalid).message)
    }

    @Test
    fun testRejectReadingEqualToNextActualReading() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        val result = MissingReadingValidator.validate(3000L, BigDecimal("120"), actuals)
        assertTrue(result is MissingReadingValidationResult.Invalid)
        assertEquals("Reading must be lower than the next actual reading.", (result as MissingReadingValidationResult.Invalid).message)
    }

    @Test
    fun testRejectReadingHigherThanNextActualReading() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        val result = MissingReadingValidator.validate(3000L, BigDecimal("130"), actuals)
        assertTrue(result is MissingReadingValidationResult.Invalid)
        assertEquals("Reading must be lower than the next actual reading.", (result as MissingReadingValidationResult.Invalid).message)
    }

    @Test
    fun testRejectReadingDateOutsideNeighboringReadings() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        // Date before first reading
        val resultBefore = MissingReadingValidator.validate(500L, BigDecimal("90"), actuals)
        assertTrue(resultBefore is MissingReadingValidationResult.Invalid)
        assertEquals("Reading date must fall between the neighboring readings.", (resultBefore as MissingReadingValidationResult.Invalid).message)

        // Date after last reading
        val resultAfter = MissingReadingValidator.validate(6000L, BigDecimal("130"), actuals)
        assertTrue(resultAfter is MissingReadingValidationResult.Invalid)
        assertEquals("Reading date must fall between the neighboring readings.", (resultAfter as MissingReadingValidationResult.Invalid).message)
    }

    @Test
    fun testRejectDuplicateActualReadingDate() {
        val actuals = listOf(
            MissingReadingValidator.ActualReadingPoint(1L, 1000L, BigDecimal("100")),
            MissingReadingValidator.ActualReadingPoint(2L, 5000L, BigDecimal("120"))
        )

        val result = MissingReadingValidator.validate(1000L, BigDecimal("110"), actuals)
        assertTrue(result is MissingReadingValidationResult.Invalid)
        assertEquals("An actual reading already exists on this date.", (result as MissingReadingValidationResult.Invalid).message)
    }
}
