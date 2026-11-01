package com.example.billtracker.domain.model

data class Meter(
    val id: Long = 0,
    val name: String,
    val utilityType: UtilityType,
    val tenantId: Long? = null,
    val currentReading: String = "0.0",
    val rateType: RateType = RateType.FIXED,
    val fixedRate: String = "0.00",
    val isForecastEnabled: Boolean = true,
    val isClosed: Boolean = false,
    val parentMeterId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
