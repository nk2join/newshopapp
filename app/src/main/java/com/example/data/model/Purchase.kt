package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["date"])
    ]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productId: Long,
    val quantity: Int, // Number of purchase units / boxes purchased
    val unitsPerPurchaseUnit: Int = 1, // Snapshot of units per box at purchase time
    val individualUnitsAdded: Int = quantity * unitsPerPurchaseUnit, // Number of sellable packets added to inventory
    val purchasePrice: Long, // in paisas per purchase unit / box
    val totalAmount: Long, // quantity * purchasePrice in paisas
    val costPerIndividualUnit: Long = if (individualUnitsAdded > 0) totalAmount / individualUnitsAdded else purchasePrice, // in paisas
    val date: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Explicit domain naming aliases
    val purchaseQuantity: Int get() = quantity
    val purchasePricePerPurchaseUnit: Long get() = purchasePrice
    val totalPurchaseAmount: Long get() = totalAmount
}
