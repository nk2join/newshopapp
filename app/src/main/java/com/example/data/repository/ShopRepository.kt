package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.BackupData
import com.example.data.model.Expense
import com.example.data.model.PeriodSummary
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.ShopSetting
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ShopRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val purchaseDao = database.purchaseDao()
    private val saleDao = database.saleDao()
    private val expenseDao = database.expenseDao()
    private val settingDao = database.shopSettingDao()

    // --- Products ---
    val allActiveProducts: Flow<List<Product>> = productDao.getAllActiveProducts()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    suspend fun getProductDirect(id: Long): Product? = productDao.getProductByIdDirect(id)

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllActiveProducts()
        } else {
            productDao.searchActiveProducts(query.trim())
        }
    }

    suspend fun addProduct(
        name: String,
        purchaseUnit: String = "Box",
        saleUnit: String = "Packet",
        unitsPerPurchaseUnit: Int = 1,
        purchasePrice: Long, // in paisas per purchase unit / box
        sellingPrice: Long, // in paisas per individual sale unit / packet
        openingStock: Int = 0, // in individual sale units / packets
        lowStockThreshold: Int = 5
    ): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Product name cannot be empty."))
        }
        val cleanPurchaseUnit = purchaseUnit.trim().ifEmpty { "Box" }
        val cleanSaleUnit = saleUnit.trim().ifEmpty { "Packet" }
        val cleanUnitsPerBox = unitsPerPurchaseUnit.coerceAtLeast(1)

        if (purchasePrice < 0 || sellingPrice < 0) {
            return Result.failure(IllegalArgumentException("Prices cannot be negative."))
        }
        if (openingStock < 0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative."))
        }
        val existing = productDao.getActiveProductByName(trimmedName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("A product with this name already exists."))
        }

        // Calculate opening total cost pool for accurate weighted average inventory valuation
        val unitCostDouble = purchasePrice.toDouble() / cleanUnitsPerBox.toDouble()
        val openingCostPool = if (openingStock > 0) {
            kotlin.math.round(openingStock.toDouble() * unitCostDouble).toLong()
        } else 0L

        val product = Product(
            name = trimmedName,
            purchaseUnit = cleanPurchaseUnit,
            saleUnit = cleanSaleUnit,
            unitsPerPurchaseUnit = cleanUnitsPerBox,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            stockQuantity = openingStock,
            totalCostPool = openingCostPool,
            lowStockThreshold = lowStockThreshold,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = productDao.insert(product)
        return Result.success(id)
    }

    suspend fun updateProduct(
        id: Long,
        name: String,
        purchaseUnit: String,
        saleUnit: String,
        unitsPerPurchaseUnit: Int,
        purchasePrice: Long,
        sellingPrice: Long,
        lowStockThreshold: Int,
        manualStockAdjustment: Int? = null
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Product name cannot be empty."))
        }
        val cleanPurchaseUnit = purchaseUnit.trim().ifEmpty { "Box" }
        val cleanSaleUnit = saleUnit.trim().ifEmpty { "Packet" }
        val cleanUnitsPerBox = unitsPerPurchaseUnit.coerceAtLeast(1)

        if (purchasePrice < 0 || sellingPrice < 0) {
            return Result.failure(IllegalArgumentException("Prices cannot be negative."))
        }
        val existing = productDao.getProductByIdDirect(id)
            ?: return Result.failure(NoSuchElementException("Product not found."))

        val duplicate = productDao.getActiveProductByName(trimmedName)
        if (duplicate != null && duplicate.id != id) {
            return Result.failure(IllegalArgumentException("Another product with this name already exists."))
        }

        val finalStock = manualStockAdjustment ?: existing.stockQuantity
        if (finalStock < 0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative."))
        }

        // Adjust cost pool if manual stock adjustment occurred
        val newCostPool = if (manualStockAdjustment != null) {
            if (finalStock == 0) {
                0L
            } else if (existing.stockQuantity > 0 && existing.totalCostPool > 0L) {
                val unitCost = existing.unitCostDoublePaisas
                kotlin.math.round(finalStock.toDouble() * unitCost).toLong()
            } else {
                val unitCost = purchasePrice.toDouble() / cleanUnitsPerBox.toDouble()
                kotlin.math.round(finalStock.toDouble() * unitCost).toLong()
            }
        } else {
            existing.totalCostPool
        }

        val updated = existing.copy(
            name = trimmedName,
            purchaseUnit = cleanPurchaseUnit,
            saleUnit = cleanSaleUnit,
            unitsPerPurchaseUnit = cleanUnitsPerBox,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            stockQuantity = finalStock,
            totalCostPool = newCostPool,
            lowStockThreshold = lowStockThreshold,
            updatedAt = System.currentTimeMillis()
        )
        productDao.update(updated)
        return Result.success(Unit)
    }

    suspend fun deleteProduct(id: Long): Result<Boolean> {
        val product = productDao.getProductByIdDirect(id)
            ?: return Result.failure(NoSuchElementException("Product not found."))

        val purchaseCount = purchaseDao.countPurchasesForProduct(id)
        val saleCount = saleDao.countSalesForProduct(id)

        return if (purchaseCount > 0 || saleCount > 0) {
            // Soft delete to preserve historical records
            productDao.softDelete(id)
            Result.success(true) // indicates soft deleted
        } else {
            productDao.hardDelete(id)
            Result.success(false) // indicates permanently deleted
        }
    }

    // --- Purchases ---
    val allPurchases: Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    fun getPurchasesForProduct(productId: Long): Flow<List<Purchase>> =
        purchaseDao.getPurchasesForProduct(productId)

    suspend fun recordPurchase(
        productId: Long,
        quantity: Int, // in purchase units / boxes
        purchasePrice: Long, // in paisas per purchase unit / box
        date: Long,
        note: String
    ): Result<Long> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }
        if (purchasePrice < 0) {
            return Result.failure(IllegalArgumentException("Purchase price cannot be negative."))
        }

        return try {
            database.withTransaction {
                val product = productDao.getProductByIdDirect(productId)
                    ?: throw NoSuchElementException("Product not found.")

                val unitsPerBox = product.unitsPerPurchaseUnit.coerceAtLeast(1)
                val individualUnitsAdded = quantity * unitsPerBox
                val totalAmount = quantity.toLong() * purchasePrice
                val costPerUnit = if (individualUnitsAdded > 0) {
                    kotlin.math.round(totalAmount.toDouble() / individualUnitsAdded.toDouble()).toLong()
                } else purchasePrice

                val newStock = product.stockQuantity + individualUnitsAdded
                val newCostPool = product.totalCostPool + totalAmount
                val now = System.currentTimeMillis()

                // Update product stock, total cost pool, and latest purchase price per box
                productDao.updateStockCostPoolAndPrice(productId, newStock, newCostPool, purchasePrice, now)

                val purchase = Purchase(
                    productId = productId,
                    quantity = quantity,
                    unitsPerPurchaseUnit = unitsPerBox,
                    individualUnitsAdded = individualUnitsAdded,
                    purchasePrice = purchasePrice,
                    totalAmount = totalAmount,
                    costPerIndividualUnit = costPerUnit,
                    date = date,
                    note = note.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                val purchaseId = purchaseDao.insert(purchase)
                Result.success(purchaseId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchase(
        purchaseId: Long,
        newQuantity: Int,
        newPurchasePrice: Long,
        date: Long,
        note: String
    ): Result<Unit> {
        if (newQuantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }
        if (newPurchasePrice < 0) {
            return Result.failure(IllegalArgumentException("Purchase price cannot be negative."))
        }

        return try {
            database.withTransaction {
                val oldPurchase = purchaseDao.getPurchaseById(purchaseId)
                    ?: throw NoSuchElementException("Purchase not found.")
                val product = productDao.getProductByIdDirect(oldPurchase.productId)
                    ?: throw NoSuchElementException("Associated product not found.")

                // Revert old purchase units and cost from inventory
                val stockWithoutOld = product.stockQuantity - oldPurchase.individualUnitsAdded
                val unitsPerBox = oldPurchase.unitsPerPurchaseUnit.coerceAtLeast(1)
                val newAddedUnits = newQuantity * unitsPerBox
                val adjustedStock = stockWithoutOld + newAddedUnits
                if (adjustedStock < 0) {
                    throw IllegalStateException("Cannot update purchase: resulting stock would be negative ($adjustedStock).")
                }

                val costPoolWithoutOld = (product.totalCostPool - oldPurchase.totalAmount).coerceAtLeast(0L)
                val newTotalAmount = newQuantity.toLong() * newPurchasePrice
                val adjustedCostPool = costPoolWithoutOld + newTotalAmount
                val newCostPerUnit = if (newAddedUnits > 0) {
                    kotlin.math.round(newTotalAmount.toDouble() / newAddedUnits.toDouble()).toLong()
                } else newPurchasePrice

                val now = System.currentTimeMillis()
                productDao.updateStockCostPoolAndPrice(product.id, adjustedStock, adjustedCostPool, newPurchasePrice, now)

                val updatedPurchase = oldPurchase.copy(
                    quantity = newQuantity,
                    unitsPerPurchaseUnit = unitsPerBox,
                    individualUnitsAdded = newAddedUnits,
                    purchasePrice = newPurchasePrice,
                    totalAmount = newTotalAmount,
                    costPerIndividualUnit = newCostPerUnit,
                    date = date,
                    note = note.trim(),
                    updatedAt = now
                )
                purchaseDao.update(updatedPurchase)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePurchase(purchaseId: Long): Result<Unit> {
        return try {
            database.withTransaction {
                val purchase = purchaseDao.getPurchaseById(purchaseId)
                    ?: throw NoSuchElementException("Purchase not found.")
                val product = productDao.getProductByIdDirect(purchase.productId)

                if (product != null) {
                    val adjustedStock = product.stockQuantity - purchase.individualUnitsAdded
                    if (adjustedStock < 0) {
                        throw IllegalStateException("Cannot delete purchase: items from this purchase have already been sold. Remaining stock would be negative ($adjustedStock).")
                    }
                    val adjustedCostPool = if (adjustedStock == 0) {
                        0L
                    } else {
                        (product.totalCostPool - purchase.totalAmount).coerceAtLeast(0L)
                    }
                    productDao.updateStockAndCostPool(product.id, adjustedStock, adjustedCostPool, System.currentTimeMillis())
                }
                purchaseDao.delete(purchase)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Sales ---
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    fun getSalesForProduct(productId: Long): Flow<List<Sale>> =
        saleDao.getSalesForProduct(productId)

    suspend fun recordSale(
        productId: Long,
        quantity: Int, // in individual sellable units (packets / pieces)
        sellingPrice: Long, // in paisas per individual sale unit
        date: Long,
        note: String
    ): Result<Long> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }
        if (sellingPrice < 0) {
            return Result.failure(IllegalArgumentException("Selling price cannot be negative."))
        }

        return try {
            database.withTransaction {
                val product = productDao.getProductByIdDirect(productId)
                    ?: throw NoSuchElementException("Product not found.")

                if (quantity > product.stockQuantity) {
                    throw IllegalArgumentException("Sale quantity ($quantity ${product.saleUnit}) cannot be greater than available stock (${product.stockQuantity} ${product.saleUnit}).")
                }

                // Accurate Weighted Average Cost Calculation
                val costAmount: Long
                val costPrice: Long
                if (quantity == product.stockQuantity) {
                    // Clearing all inventory uses the entire remaining cost pool (no rounding leftovers)
                    costAmount = product.totalCostPool
                    costPrice = if (quantity > 0) {
                        kotlin.math.round(costAmount.toDouble() / quantity.toDouble()).toLong()
                    } else 0L
                } else {
                    val unitCost = product.unitCostDoublePaisas
                    costAmount = kotlin.math.round(quantity.toDouble() * unitCost).toLong()
                    costPrice = kotlin.math.round(unitCost).toLong()
                }

                val totalAmount = quantity.toLong() * sellingPrice
                val profit = totalAmount - costAmount
                val newStock = product.stockQuantity - quantity
                val newCostPool = if (newStock == 0) 0L else (product.totalCostPool - costAmount).coerceAtLeast(0L)
                val now = System.currentTimeMillis()

                productDao.updateStockAndCostPool(productId, newStock, newCostPool, now)

                val sale = Sale(
                    productId = productId,
                    quantity = quantity,
                    sellingPrice = sellingPrice,
                    costPrice = costPrice, // Historical cost locked at time of sale!
                    totalAmount = totalAmount,
                    costAmount = costAmount,
                    profit = profit,
                    date = date,
                    note = note.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                val saleId = saleDao.insert(sale)
                Result.success(saleId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSale(
        saleId: Long,
        newQuantity: Int,
        newSellingPrice: Long,
        date: Long,
        note: String
    ): Result<Unit> {
        if (newQuantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))
        }
        if (newSellingPrice < 0) {
            return Result.failure(IllegalArgumentException("Selling price cannot be negative."))
        }

        return try {
            database.withTransaction {
                val oldSale = saleDao.getSaleById(saleId)
                    ?: throw NoSuchElementException("Sale not found.")
                val product = productDao.getProductByIdDirect(oldSale.productId)
                    ?: throw NoSuchElementException("Associated product not found.")

                // Revert previous sale from stock and cost pool
                val stockWithRevert = product.stockQuantity + oldSale.quantity
                if (newQuantity > stockWithRevert) {
                    throw IllegalArgumentException("Sale quantity ($newQuantity) cannot be greater than available stock ($stockWithRevert ${product.saleUnit}).")
                }
                val costPoolWithRevert = product.totalCostPool + oldSale.costAmount

                val costAmount: Long
                val costPrice: Long
                if (newQuantity == stockWithRevert) {
                    costAmount = costPoolWithRevert
                    costPrice = if (newQuantity > 0) {
                        kotlin.math.round(costAmount.toDouble() / newQuantity.toDouble()).toLong()
                    } else 0L
                } else {
                    val unitCost = if (stockWithRevert > 0) {
                        costPoolWithRevert.toDouble() / stockWithRevert.toDouble()
                    } else {
                        oldSale.costPrice.toDouble()
                    }
                    costAmount = kotlin.math.round(newQuantity.toDouble() * unitCost).toLong()
                    costPrice = kotlin.math.round(unitCost).toLong()
                }

                val totalAmount = newQuantity.toLong() * newSellingPrice
                val profit = totalAmount - costAmount
                val adjustedStock = stockWithRevert - newQuantity
                val adjustedCostPool = if (adjustedStock == 0) 0L else (costPoolWithRevert - costAmount).coerceAtLeast(0L)
                val now = System.currentTimeMillis()

                productDao.updateStockAndCostPool(product.id, adjustedStock, adjustedCostPool, now)

                val updatedSale = oldSale.copy(
                    quantity = newQuantity,
                    sellingPrice = newSellingPrice,
                    costPrice = costPrice,
                    totalAmount = totalAmount,
                    costAmount = costAmount,
                    profit = profit,
                    date = date,
                    note = note.trim(),
                    updatedAt = now
                )
                saleDao.update(updatedSale)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSale(saleId: Long): Result<Unit> {
        return try {
            database.withTransaction {
                val sale = saleDao.getSaleById(saleId)
                    ?: throw NoSuchElementException("Sale not found.")
                val product = productDao.getProductByIdDirect(sale.productId)

                if (product != null) {
                    val restoredStock = product.stockQuantity + sale.quantity
                    val restoredCostPool = product.totalCostPool + sale.costAmount
                    productDao.updateStockAndCostPool(product.id, restoredStock, restoredCostPool, System.currentTimeMillis())
                }
                saleDao.delete(sale)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Expenses ---
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun addExpense(
        title: String,
        amount: Long,
        date: Long,
        note: String
    ): Result<Long> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter an expense name."))
        }
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Expense amount must be greater than zero."))
        }

        val now = System.currentTimeMillis()
        val expense = Expense(
            title = trimmed,
            amount = amount,
            date = date,
            note = note.trim(),
            createdAt = now,
            updatedAt = now
        )
        val id = expenseDao.insert(expense)
        return Result.success(id)
    }

    suspend fun updateExpense(
        id: Long,
        title: String,
        amount: Long,
        date: Long,
        note: String
    ): Result<Unit> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter an expense name."))
        }
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Expense amount must be greater than zero."))
        }
        val existing = expenseDao.getExpenseById(id)
            ?: return Result.failure(NoSuchElementException("Expense not found."))

        val updated = existing.copy(
            title = trimmed,
            amount = amount,
            date = date,
            note = note.trim(),
            updatedAt = System.currentTimeMillis()
        )
        expenseDao.update(updated)
        return Result.success(Unit)
    }

    suspend fun deleteExpense(id: Long): Result<Unit> {
        expenseDao.deleteById(id)
        return Result.success(Unit)
    }

    // --- Settings ---
    val shopNameFlow: Flow<String> = settingDao.getSettingFlow("shop_name").map { it ?: "My Shop" }

    suspend fun setShopName(name: String) {
        val trimmed = name.trim().ifEmpty { "My Shop" }
        settingDao.setSetting(ShopSetting("shop_name", trimmed))
    }

    suspend fun getShopName(): String {
        return settingDao.getSetting("shop_name") ?: "My Shop"
    }

    // --- Unified Transactions & Reports ---
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionItem>> {
        val productsFlow = productDao.getAllProducts()
        val purchasesFlow = purchaseDao.getPurchasesBetween(startDate, endDate)
        val salesFlow = saleDao.getSalesBetween(startDate, endDate)
        val expensesFlow = expenseDao.getExpensesBetween(startDate, endDate)

        return combine(productsFlow, purchasesFlow, salesFlow, expensesFlow) { products, purchases, sales, expenses ->
            val productMap = products.associateBy { it.id }
            val list = mutableListOf<TransactionItem>()

            purchases.forEach { p ->
                val prod = productMap[p.productId]
                val prodName = prod?.name ?: "Product #${p.productId}"
                list.add(
                    TransactionItem(
                        id = p.id,
                        type = TransactionType.PURCHASE,
                        title = prodName,
                        productId = p.productId,
                        quantity = p.quantity,
                        unitPrice = p.purchasePrice,
                        amount = p.totalAmount,
                        date = p.date,
                        note = p.note,
                        purchaseUnit = prod?.purchaseUnit ?: "Box",
                        saleUnit = prod?.saleUnit ?: "Packet",
                        unitsPerPurchaseUnit = p.unitsPerPurchaseUnit,
                        individualUnitsAdded = p.individualUnitsAdded
                    )
                )
            }

            sales.forEach { s ->
                val prod = productMap[s.productId]
                val prodName = prod?.name ?: "Product #${s.productId}"
                list.add(
                    TransactionItem(
                        id = s.id,
                        type = TransactionType.SALE,
                        title = prodName,
                        productId = s.productId,
                        quantity = s.quantity,
                        unitPrice = s.sellingPrice,
                        amount = s.totalAmount,
                        costAmount = s.costAmount,
                        profit = s.profit,
                        date = s.date,
                        note = s.note,
                        purchaseUnit = prod?.purchaseUnit ?: "Box",
                        saleUnit = prod?.saleUnit ?: "Packet",
                        unitsPerPurchaseUnit = prod?.unitsPerPurchaseUnit ?: 1
                    )
                )
            }

            expenses.forEach { e ->
                list.add(
                    TransactionItem(
                        id = e.id,
                        type = TransactionType.EXPENSE,
                        title = e.title,
                        amount = e.amount,
                        date = e.date,
                        note = e.note
                    )
                )
            }

            // Sort by date DESC, then id DESC
            list.sortedWith(compareByDescending<TransactionItem> { it.date }.thenByDescending { it.id })
        }
    }

    fun getPeriodSummary(startDate: Long, endDate: Long): Flow<PeriodSummary> {
        val purchasesFlow = purchaseDao.getPurchasesBetween(startDate, endDate)
        val salesFlow = saleDao.getSalesBetween(startDate, endDate)
        val expensesFlow = expenseDao.getExpensesBetween(startDate, endDate)
        val activeProductsFlow = productDao.getAllActiveProducts()

        return combine(purchasesFlow, salesFlow, expensesFlow, activeProductsFlow) { purchases, sales, expenses, products ->
            var totalPurchasesAmount = 0L
            var itemsPurchasedCount = 0
            purchases.forEach {
                totalPurchasesAmount += it.totalAmount
                itemsPurchasedCount += it.individualUnitsAdded
            }

            var totalSalesAmount = 0L
            var grossProfit = 0L
            var itemsSoldCount = 0
            sales.forEach {
                totalSalesAmount += it.totalAmount
                grossProfit += it.profit
                itemsSoldCount += it.quantity
            }

            var totalExpensesAmount = 0L
            expenses.forEach {
                totalExpensesAmount += it.amount
            }

            val netProfit = grossProfit - totalExpensesAmount

            var totalStockUnits = 0
            var totalStockValue = 0L
            products.forEach {
                totalStockUnits += it.stockQuantity
                totalStockValue += it.stockValue
            }

            PeriodSummary(
                totalSales = totalSalesAmount,
                totalPurchase = totalPurchasesAmount,
                grossProfit = grossProfit,
                totalExpenses = totalExpensesAmount,
                netProfit = netProfit,
                itemsSold = itemsSoldCount,
                itemsPurchased = itemsPurchasedCount,
                activeProductsCount = products.size,
                totalStockUnits = totalStockUnits,
                totalStockValue = totalStockValue
            )
        }
    }

    // --- Backup & Restore ---
    suspend fun createBackup(): BackupData {
        val shopName = getShopName()
        val products = productDao.getAllProductsList()
        val purchases = purchaseDao.getAllPurchasesList()
        val sales = saleDao.getAllSalesList()
        val expenses = expenseDao.getAllExpensesList()

        return BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            shopName = shopName,
            products = products,
            purchases = purchases,
            sales = sales,
            expenses = expenses
        )
    }

    suspend fun restoreBackup(backup: BackupData): Result<Unit> {
        return try {
            database.withTransaction {
                productDao.clearAll()
                purchaseDao.clearAll()
                saleDao.clearAll()
                expenseDao.clearAll()

                if (backup.products.isNotEmpty()) {
                    productDao.insertAll(backup.products)
                }
                if (backup.purchases.isNotEmpty()) {
                    purchaseDao.insertAll(backup.purchases)
                }
                if (backup.sales.isNotEmpty()) {
                    saleDao.insertAll(backup.sales)
                }
                if (backup.expenses.isNotEmpty()) {
                    expenseDao.insertAll(backup.expenses)
                }
                if (backup.shopName.isNotBlank()) {
                    settingDao.setSetting(ShopSetting("shop_name", backup.shopName))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
