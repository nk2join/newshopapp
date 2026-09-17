package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"], unique = false),
        Index(value = ["isDeleted"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val purchaseUnit: String = "Unit", // e.g. "Box", "Carton", "Pack"
    val saleUnit: String = "Unit", // e.g. "Packet", "Piece", "Bottle"
    val unitsPerPurchaseUnit: Int = 1, // Number of individual sale units per purchase unit (e.g. 12 packets per box)
    val purchasePrice: Long, // in paisas per purchase unit / box (e.g. Rs. 100 / box = 10000 paisas)
    val sellingPrice: Long, // in paisas per individual sale unit / packet (e.g. Rs. 10 / packet = 1000 paisas)
    val stockQuantity: Int, // Current stock stored in SMALLEST SELLABLE UNIT (sale units / packets)
    val totalCostPool: Long = 0L, // Total cost of currently held stock in paisas (weighted average costing)
    val lowStockThreshold: Int = 5, // Alert threshold in sale units / packets
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Aliases for explicit domain naming
    val purchasePricePerPurchaseUnit: Long get() = purchasePrice
    val sellingPricePerSaleUnit: Long get() = sellingPrice
    val stockQuantityInSaleUnits: Int get() = stockQuantity

    /**
     * Cost per individual sale unit (in paisas) as Double for high precision.
     * When stock is held, uses weighted average of totalCostPool / stockQuantity.
     * When stock is zero or not tracked, uses purchasePrice / unitsPerPurchaseUnit.
     */
    val unitCostDoublePaisas: Double
        get() {
            return if (stockQuantity > 0 && totalCostPool > 0L) {
                totalCostPool.toDouble() / stockQuantity.toDouble()
            } else if (unitsPerPurchaseUnit > 0) {
                purchasePrice.toDouble() / unitsPerPurchaseUnit.toDouble()
            } else {
                purchasePrice.toDouble()
            }
        }

    /**
     * Rounded cost per individual unit in paisas.
     */
    val unitCostPaisas: Long
        get() = kotlin.math.round(unitCostDoublePaisas).toLong()

    /**
     * Total valuation of current inventory in paisas.
     */
    val stockValue: Long
        get() = if (stockQuantity > 0 && totalCostPool > 0L) {
            totalCostPool
        } else {
            kotlin.math.round(stockQuantity.toDouble() * unitCostDoublePaisas).toLong()
        }

    val isLowStock: Boolean
        get() = stockQuantity <= lowStockThreshold

    /**
     * Formats stock breakdown showing both complete boxes and remaining packets.
     * Example: 31 Packets -> "2 Boxes + 7 Packets" or "31 Packets"
     */
    val stockBreakdown: String
        get() {
            if (unitsPerPurchaseUnit <= 1) {
                return "$stockQuantity $saleUnit"
            }
            val boxes = stockQuantity / unitsPerPurchaseUnit
            val remaining = stockQuantity % unitsPerPurchaseUnit
            return when {
                boxes == 0 -> "$remaining $saleUnit"
                remaining == 0 -> "$boxes $purchaseUnit"
                else -> "$boxes $purchaseUnit + $remaining $saleUnit"
            }
        }

    /**
     * Human-readable full breakdown string.
     * Example: "31 Packets (2 Boxes + 7 Packets)"
     */
    val fullStockDisplay: String
        get() {
            if (unitsPerPurchaseUnit <= 1) {
                return "$stockQuantity $saleUnit"
            }
            return "$stockQuantity $saleUnit ($stockBreakdown)"
        }
}
