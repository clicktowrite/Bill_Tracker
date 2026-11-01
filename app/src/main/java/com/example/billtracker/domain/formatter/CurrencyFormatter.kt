package com.example.billtracker.domain.formatter

import java.math.BigDecimal
import java.text.DecimalFormat

object CurrencyFormatter {

    fun format(
        amount: BigDecimal?,
        currencySymbol: String = "₱"
    ): String {
        if (amount == null) return "${currencySymbol}0.00"
        val formatter = DecimalFormat("#,##0.00")
        return "$currencySymbol${formatter.format(amount)}"
    }

    fun format(
        amountStr: String?,
        currencySymbol: String = "₱"
    ): String {
        if (amountStr.isNullOrBlank()) return "${currencySymbol}0.00"
        return try {
            val decimal = BigDecimal(amountStr.trim().replace(",", ""))
            format(decimal, currencySymbol)
        } catch (_: Exception) {
            "${currencySymbol}0.00"
        }
    }
}
