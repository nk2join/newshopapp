package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    private val integerFormatter = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
        applyPattern("#,##0")
    }
    
    private val decimalFormatter = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
        applyPattern("#,##0.00")
    }

    /**
     * Formats an amount in paisas (1 PKR = 100 paisas) as a readable Pakistani Rupee string.
     * Examples:
     * 150000L -> "Rs. 1,500"
     * 150050L -> "Rs. 1,500.50"
     * -20000L -> "-Rs. 200"
     */
    fun formatRupees(amountInPaisas: Long): String {
        val isNegative = amountInPaisas < 0
        val absPaisas = kotlin.math.abs(amountInPaisas)
        val rupees = absPaisas / 100
        val remainingPaisas = absPaisas % 100

        val formattedNum = if (remainingPaisas == 0L) {
            integerFormatter.format(rupees)
        } else {
            val decimalValue = rupees + (remainingPaisas.toDouble() / 100.0)
            decimalFormatter.format(decimalValue)
        }

        return if (isNegative) "-Rs. $formattedNum" else "Rs. $formattedNum"
    }

    /**
     * Formats a floating-point paisa value (e.g. 833.33 paisas -> "Rs. 8.33")
     */
    fun formatDoubleRupees(doublePaisas: Double): String {
        val isNegative = doublePaisas < 0.0
        val absPaisas = kotlin.math.abs(doublePaisas)
        val rupees = absPaisas / 100.0
        val formatted = decimalFormatter.format(rupees)
        return if (isNegative) "-Rs. $formatted" else "Rs. $formatted"
    }

    /**
     * Formats paisas for editable text input (e.g. 150000L -> "1500" or 150050L -> "1500.50")
     */
    fun paisasToInputString(amountInPaisas: Long): String {
        if (amountInPaisas == 0L) return ""
        val rupees = amountInPaisas / 100
        val remainingPaisas = amountInPaisas % 100
        return if (remainingPaisas == 0L) {
            rupees.toString()
        } else {
            "%.2f".format(Locale.US, amountInPaisas.toDouble() / 100.0).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Parses user entered text (e.g. "150", "150.5", "1,500") into paisas (Long).
     * Returns null if invalid or negative.
     */
    fun parseRupeesToPaisas(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return null

        return try {
            val doubleVal = cleaned.toDouble()
            if (doubleVal < 0) null
            else kotlin.math.round(doubleVal * 100.0).toLong()
        } catch (_: Exception) {
            null
        }
    }

    fun calculateTotal(quantity: Int, unitPricePaisas: Long): Long {
        return quantity.toLong() * unitPricePaisas
    }

    fun calculateCost(quantity: Int, costPricePaisas: Long): Long {
        return quantity.toLong() * costPricePaisas
    }

    fun calculateProfit(saleTotalAmount: Long, costAmount: Long): Long {
        return saleTotalAmount - costAmount
    }

    fun calculateNetProfit(grossProfit: Long, expenses: Long): Long {
        return grossProfit - expenses
    }
}
