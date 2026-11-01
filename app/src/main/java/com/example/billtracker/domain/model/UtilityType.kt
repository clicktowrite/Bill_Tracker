package com.example.billtracker.domain.model

enum class UtilityType(val displayName: String, val unit: String) {
    ELECTRICITY("Electricity", "kWh"),
    WATER("Water", "m³")
}
