package com.example.billtracker

import com.example.billtracker.domain.calculator.PaymentCalculator
import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.model.PaymentMethod
import com.example.billtracker.domain.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PaymentCalculatorTest {

    @Test
    fun noPayments_returnsUnpaid() {
        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = "1600.00",
            payments = emptyList()
        )

        assertEquals(BigDecimal("1600.00"), summary.totalBillAmount)
        assertEquals(BigDecimal("0.00"), summary.totalPaidAmount)
        assertEquals(BigDecimal("1600.00"), summary.remainingBalance)
        assertEquals(PaymentStatus.UNPAID, summary.status)
    }

    @Test
    fun partialPayment_returnsPartiallyPaid() {
        val payments = listOf(
            Payment(id = 1, readingId = 10, amount = "1000.00", paymentMethod = PaymentMethod.GCASH)
        )

        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = "1600.00",
            payments = payments
        )

        assertEquals(BigDecimal("1000.00"), summary.totalPaidAmount)
        assertEquals(BigDecimal("600.00"), summary.remainingBalance)
        assertEquals(PaymentStatus.PARTIALLY_PAID, summary.status)
    }

    @Test
    fun multiplePartialPayments_accumulatesCorrectly() {
        val payments = listOf(
            Payment(id = 1, readingId = 10, amount = "500.00"),
            Payment(id = 2, readingId = 10, amount = "600.00")
        )

        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = "1500.00",
            payments = payments
        )

        assertEquals(BigDecimal("1100.00"), summary.totalPaidAmount)
        assertEquals(BigDecimal("400.00"), summary.remainingBalance)
        assertEquals(PaymentStatus.PARTIALLY_PAID, summary.status)
    }

    @Test
    fun fullPayment_returnsPaid() {
        val payments = listOf(
            Payment(id = 1, readingId = 10, amount = "1600.00")
        )

        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = "1600.00",
            payments = payments
        )

        assertEquals(BigDecimal("1600.00"), summary.totalPaidAmount)
        assertEquals(BigDecimal("0.00"), summary.remainingBalance)
        assertEquals(PaymentStatus.PAID, summary.status)
    }

    @Test
    fun overdueDate_returnsOverdue() {
        val pastDate = System.currentTimeMillis() - 86400000L * 5 // 5 days ago

        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = "1250.00",
            payments = emptyList(),
            dueDate = pastDate
        )

        assertEquals(PaymentStatus.OVERDUE, summary.status)
    }
}
