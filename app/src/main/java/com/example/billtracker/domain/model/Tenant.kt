package com.example.billtracker.domain.model

data class Tenant(
    val id: Long = 0,
    val name: String,
    val roomOrUnit: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
