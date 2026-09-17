package com.example.data.model

data class PeriodSummary(
    val totalSales: Long = 0L,
    val totalPurchase: Long = 0L,
    val grossProfit: Long = 0L,
    val totalExpenses: Long = 0L,
    val netProfit: Long = 0L,
    val itemsSold: Int = 0,
    val itemsPurchased: Int = 0,
    val activeProductsCount: Int = 0,
    val totalStockUnits: Int = 0,
    val totalStockValue: Long = 0L
)

enum class ReportPeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}
