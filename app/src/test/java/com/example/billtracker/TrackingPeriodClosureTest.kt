package com.example.billtracker

import com.example.billtracker.domain.calculator.DailyInterpolationEngine
import com.example.billtracker.domain.calculator.InputReading
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TrackingPeriodClosureTest {

    @Test
    fun sharedBoundary_doesNotAddDuplicateConsumption() {
        val oct15 = 1697328000000L
        val oct20 = oct15 + (5 * 86400000L)

        // Old period closes at 1250 kWh on Oct 15
        val oldPeriodClosingReading = "1250.0"

        // New period starts with baseline 1250 kWh on Oct 15 and first new reading on Oct 20 of 1280 kWh
        val newPeriodReadings = listOf(
            InputReading(id = 1, timestamp = oct15, value = oldPeriodClosingReading, isMeterReset = false),
            InputReading(id = 2, timestamp = oct20, value = "1280.0", isMeterReset = false)
        )

        val interpolated = DailyInterpolationEngine.interpolateReadings(
            actualReadings = newPeriodReadings,
            forecastDays = 0,
            utilityType = UtilityType.ELECTRICITY
        )

        // Total consumption in new period must be exactly 30 kWh (1280 - 1250)
        val totalConsumption = interpolated.mapNotNull { it.dailyConsumption }.fold(BigDecimal.ZERO, BigDecimal::add)
        assertEquals(BigDecimal("30.00"), totalConsumption)
    }

    @Test
    fun meterClosureModel_verifiesFlagsAndProperties() {
        val oldMeter = Meter(
            id = 1L,
            name = "Room 1 Meter",
            utilityType = UtilityType.ELECTRICITY,
            tenantId = 10L,
            currentReading = "1000.0",
            isClosed = false
        )

        // Close period
        val closedOldMeter = oldMeter.copy(
            isClosed = true,
            currentReading = "1250.0"
        )

        // Create new active meter for Tenant 20
        val newMeter = Meter(
            id = 2L,
            name = oldMeter.name,
            utilityType = oldMeter.utilityType,
            tenantId = 20L,
            currentReading = "1250.0",
            isClosed = false,
            parentMeterId = oldMeter.id
        )

        assertTrue(closedOldMeter.isClosed)
        assertEquals("1250.0", closedOldMeter.currentReading)
        assertEquals(10L, closedOldMeter.tenantId)

        assertFalse(newMeter.isClosed)
        assertEquals("1250.0", newMeter.currentReading)
        assertEquals(20L, newMeter.tenantId)
        assertEquals(1L, newMeter.parentMeterId)
    }
}
