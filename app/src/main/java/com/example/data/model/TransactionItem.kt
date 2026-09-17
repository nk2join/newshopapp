package com.example.data.model

enum class TransactionType {
    PURCHASE,
    SALE,
    EXPENSE
}

data class TransactionItem(
    val id: Long,
    val type: TransactionType,
    val title: String,
    val productId: Long? = null,
    val quantity: Int? = null,
    val unitPrice: Long? = null,
    val amount: Long, // Total amount in paisas (positive for sale/purchase/expense)
    val costAmount: Long? = null,
    val profit: Long? = null, // only for SALE
    val date: Long,
    val note: String = "",
    val purchaseUnit: String? = null,
    val saleUnit: String? = null,
    val unitsPerPurchaseUnit: Int? = null,
    val individualUnitsAdded: Int? = null
)
