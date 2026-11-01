package com.example.billtracker.domain.model

data class MeterReading(
    val id: Long = 0,
    val meterId: Long,
    val tenantId: Long? = null,
    val billingDate: Long = System.currentTimeMillis(),
    val previousReading: String,
    val currentReading: String,
    val usage: String,
    val rate: String,
    val rateType: RateType,
    val providerTotalBill: String? = null,
    val providerTotalUsage: String? = null,
    val totalAmount: String,
    val notes: String? = null,
    val photoPath: String? = null,
    val isFinalizedBill: Boolean = true,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val isPaid: Boolean = false // Task 4: Paid / Unpaid Status Tracker
)
