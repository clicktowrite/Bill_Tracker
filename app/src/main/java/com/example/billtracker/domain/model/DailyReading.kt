package com.example.billtracker.domain.model

data class DailyReading(
    val id: Long = 0,
    val meterId: Long,
    val readingDate: Long = System.currentTimeMillis(),
    val readingValue: String,
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val photoPath: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
