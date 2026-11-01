package com.example.billtracker.domain.calculator

import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.model.PaymentStatus
import java.math.BigDecimal
import java.math.RoundingMode

data class PaymentSummary(
    val totalBillAmount: BigDecimal,
    val totalPaidAmount: BigDecimal,
    val remainingBalance: BigDecimal,
    val status: PaymentStatus
)

object PaymentCalculator {

    private fun parseBigDecimal(input: String?): BigDecimal {
        if (input.isNullOrBlank()) return BigDecimal.ZERO
        return try {
            BigDecimal(input.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    /**
     * Calculates payment summary given a bill's total amount, list of payments, and optional due date.
     */
    fun calculateSummary(
        totalBillAmountStr: String?,
        payments: List<Payment>,
        dueDate: Long? = null,
        currentTime: Long = System.currentTimeMillis()
    ): PaymentSummary {
        val totalBill = parseBigDecimal(totalBillAmountStr)

        var totalPaid = BigDecimal.ZERO
        payments.forEach { p ->
            totalPaid = totalPaid.add(parseBigDecimal(p.amount))
        }
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP)

        val balance = totalBill.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP)

        val status = when {
            balance <= BigDecimal.ZERO && totalBill > BigDecimal.ZERO -> PaymentStatus.PAID
            totalPaid > BigDecimal.ZERO && balance > BigDecimal.ZERO -> PaymentStatus.PARTIALLY_PAID
            dueDate != null && dueDate < currentTime && balance > BigDecimal.ZERO -> PaymentStatus.OVERDUE
            else -> PaymentStatus.UNPAID
        }

        return PaymentSummary(
            totalBillAmount = totalBill,
            totalPaidAmount = totalPaid,
            remainingBalance = if (balance < BigDecimal.ZERO) BigDecimal.ZERO else balance,
            status = status
        )
    }
}
