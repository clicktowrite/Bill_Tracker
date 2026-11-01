package com.example.billtracker.domain.model

data class Payment(
    val id: Long = 0,
    val readingId: Long,
    val tenantId: Long? = null,
    val amount: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
