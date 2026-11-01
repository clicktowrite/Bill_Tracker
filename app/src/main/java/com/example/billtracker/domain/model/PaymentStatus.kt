package com.example.billtracker.domain.model

enum class PaymentStatus(val displayName: String) {
    UNPAID("Unpaid"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid"),
    OVERDUE("Overdue")
}
