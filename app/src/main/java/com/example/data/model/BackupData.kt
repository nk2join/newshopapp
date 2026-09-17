package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val shopName: String,
    val products: List<Product>,
    val purchases: List<Purchase>,
    val sales: List<Sale>,
    val expenses: List<Expense>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("shopName", shopName)

        val productsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("purchaseUnit", p.purchaseUnit)
            obj.put("saleUnit", p.saleUnit)
            obj.put("unitsPerPurchaseUnit", p.unitsPerPurchaseUnit)
            obj.put("purchasePrice", p.purchasePrice)
            obj.put("sellingPrice", p.sellingPrice)
            obj.put("stockQuantity", p.stockQuantity)
            obj.put("totalCostPool", p.totalCostPool)
            obj.put("lowStockThreshold", p.lowStockThreshold)
            obj.put("isDeleted", p.isDeleted)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            productsArray.put(obj)
        }
        root.put("products", productsArray)

        val purchasesArray = JSONArray()
        purchases.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("productId", p.productId)
            obj.put("quantity", p.quantity)
            obj.put("unitsPerPurchaseUnit", p.unitsPerPurchaseUnit)
            obj.put("individualUnitsAdded", p.individualUnitsAdded)
            obj.put("purchasePrice", p.purchasePrice)
            obj.put("totalAmount", p.totalAmount)
            obj.put("costPerIndividualUnit", p.costPerIndividualUnit)
            obj.put("date", p.date)
            obj.put("note", p.note)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            purchasesArray.put(obj)
        }
        root.put("purchases", purchasesArray)

        val salesArray = JSONArray()
        sales.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("productId", s.productId)
            obj.put("quantity", s.quantity)
            obj.put("sellingPrice", s.sellingPrice)
            obj.put("costPrice", s.costPrice)
            obj.put("totalAmount", s.totalAmount)
            obj.put("costAmount", s.costAmount)
            obj.put("profit", s.profit)
            obj.put("date", s.date)
            obj.put("note", s.note)
            obj.put("createdAt", s.createdAt)
            obj.put("updatedAt", s.updatedAt)
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        val expensesArray = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("amount", e.amount)
            obj.put("date", e.date)
            obj.put("note", e.note)
            obj.put("createdAt", e.createdAt)
            obj.put("updatedAt", e.updatedAt)
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupData {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val shopName = root.optString("shopName", "My Shop")

            val productsList = mutableListOf<Product>()
            val productsArray = root.optJSONArray("products") ?: JSONArray()
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                val pPrice = obj.getLong("purchasePrice")
                val stock = obj.getInt("stockQuantity")
                val defaultCostPool = stock.toLong() * pPrice
                productsList.add(
                    Product(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        purchaseUnit = obj.optString("purchaseUnit", "Unit"),
                        saleUnit = obj.optString("saleUnit", "Unit"),
                        unitsPerPurchaseUnit = obj.optInt("unitsPerPurchaseUnit", 1).coerceAtLeast(1),
                        purchasePrice = pPrice,
                        sellingPrice = obj.getLong("sellingPrice"),
                        stockQuantity = stock,
                        totalCostPool = obj.optLong("totalCostPool", defaultCostPool),
                        lowStockThreshold = obj.optInt("lowStockThreshold", 5),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val purchasesList = mutableListOf<Purchase>()
            val purchasesArray = root.optJSONArray("purchases") ?: JSONArray()
            for (i in 0 until purchasesArray.length()) {
                val obj = purchasesArray.getJSONObject(i)
                val qty = obj.getInt("quantity")
                val pPrice = obj.getLong("purchasePrice")
                val totalAmt = obj.getLong("totalAmount")
                val unitsPerBox = obj.optInt("unitsPerPurchaseUnit", 1).coerceAtLeast(1)
                val addedUnits = obj.optInt("individualUnitsAdded", qty * unitsPerBox)
                val costPerUnit = obj.optLong("costPerIndividualUnit", if (addedUnits > 0) totalAmt / addedUnits else pPrice)
                purchasesList.add(
                    Purchase(
                        id = obj.optLong("id", 0L),
                        productId = obj.getLong("productId"),
                        quantity = qty,
                        unitsPerPurchaseUnit = unitsPerBox,
                        individualUnitsAdded = addedUnits,
                        purchasePrice = pPrice,
                        totalAmount = totalAmt,
                        costPerIndividualUnit = costPerUnit,
                        date = obj.getLong("date"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val salesList = mutableListOf<Sale>()
            val salesArray = root.optJSONArray("sales") ?: JSONArray()
            for (i in 0 until salesArray.length()) {
                val obj = salesArray.getJSONObject(i)
                salesList.add(
                    Sale(
                        id = obj.optLong("id", 0L),
                        productId = obj.getLong("productId"),
                        quantity = obj.getInt("quantity"),
                        sellingPrice = obj.getLong("sellingPrice"),
                        costPrice = obj.getLong("costPrice"),
                        totalAmount = obj.getLong("totalAmount"),
                        costAmount = obj.getLong("costAmount"),
                        profit = obj.getLong("profit"),
                        date = obj.getLong("date"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val expensesList = mutableListOf<Expense>()
            val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.getJSONObject(i)
                expensesList.add(
                    Expense(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        amount = obj.getLong("amount"),
                        date = obj.getLong("date"),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            return BackupData(
                version = version,
                exportedAt = exportedAt,
                shopName = shopName,
                products = productsList,
                purchases = purchasesList,
                sales = salesList,
                expenses = expensesList
            )
        }
    }
}
