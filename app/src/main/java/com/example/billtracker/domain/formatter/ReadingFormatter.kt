package com.example.billtracker.domain.formatter

import com.example.billtracker.domain.model.UtilityType
import java.math.BigDecimal
import java.text.DecimalFormat

object ReadingFormatter {

    fun formatReading(valueStr: String?, utilityType: UtilityType): String {
        if (valueStr.isNullOrBlank()) return "0 ${utilityType.unit}"
        return try {
            val decimal = BigDecimal(valueStr.trim().replace(",", ""))
            val formatter = DecimalFormat("#,##0.00")
            "${formatter.format(decimal)} ${utilityType.unit}"
        } catch (_: Exception) {
            "0 ${utilityType.unit}"
        }
    }

    fun formatRate(rateStr: String?, utilityType: UtilityType, currencySymbol: String = "₱"): String {
        if (rateStr.isNullOrBlank()) return "${currencySymbol}0.00/${utilityType.unit}"
        return try {
            val decimal = BigDecimal(rateStr.trim().replace(",", ""))
            val formatter = DecimalFormat("#,##0.00")
            "$currencySymbol${formatter.format(decimal)}/${utilityType.unit}"
        } catch (_: Exception) {
            "${currencySymbol}0.00/${utilityType.unit}"
        }
    }
}
