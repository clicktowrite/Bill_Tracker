package com.example.billtracker.domain.model

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    GCASH("GCash"),
    MAYA("Maya"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other")
}
