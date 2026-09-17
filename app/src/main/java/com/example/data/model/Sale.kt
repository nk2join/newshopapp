package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["date"])
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val productId: Long,
    val quantity: Int, // in individual sellable units (packets / pieces)
    val sellingPrice: Long, // in paisas per individual sale unit
    val costPrice: Long, // applicable individual unit purchase cost at time of sale (in paisas)
    val totalAmount: Long, // quantity * sellingPrice
    val costAmount: Long, // quantity * costPrice (or remaining inventory cost if selling entire stock)
    val profit: Long, // totalAmount - costAmount
    val date: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Explicit domain naming aliases
    val quantityInSaleUnits: Int get() = quantity
    val sellingPricePerSaleUnit: Long get() = sellingPrice
    val costPerIndividualUnit: Long get() = costPrice
    val totalSaleAmount: Long get() = totalAmount
    val totalCostAmount: Long get() = costAmount
}
