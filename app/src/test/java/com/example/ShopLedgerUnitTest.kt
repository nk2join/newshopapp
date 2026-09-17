package com.example

import com.example.data.model.BackupData
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopLedgerUnitTest {

    @Test
    fun testMoneyUtilsFormatting() {
        assertEquals("Rs. 0", MoneyUtils.formatRupees(0L))
        assertEquals("Rs. 150", MoneyUtils.formatRupees(15000L))
        assertEquals("Rs. 1,500.50", MoneyUtils.formatRupees(150050L))
        assertEquals("-Rs. 50", MoneyUtils.formatRupees(-5000L))
    }

    @Test
    fun testMoneyUtilsParsing() {
        assertEquals(15000L, MoneyUtils.parseRupeesToPaisas("150"))
        assertEquals(15050L, MoneyUtils.parseRupeesToPaisas("150.5"))
        assertEquals(15075L, MoneyUtils.parseRupeesToPaisas("150.75"))
        assertEquals(0L, MoneyUtils.parseRupeesToPaisas("0"))
        assertEquals(null, MoneyUtils.parseRupeesToPaisas("abc"))
    }

    @Test
    fun testMoneyUtilsCalculations() {
        // 5 items at Rs. 120.50 (12050 paisas) each
        val total = MoneyUtils.calculateTotal(5, 12050L)
        assertEquals(60250L, total) // Rs. 602.50

        // Cost of 5 items at Rs. 100 (10000 paisas)
        val cost = MoneyUtils.calculateCost(5, 10000L)
        assertEquals(50000L, cost)

        // Profit
        val profit = MoneyUtils.calculateProfit(total, cost)
        assertEquals(10250L, profit) // Rs. 102.50 profit
    }

    @Test
    fun testProductStockValueAndLowStock() {
        val product = Product(
            id = 1L,
            name = "Sugar 1kg",
            purchasePrice = 14000L, // Rs. 140
            sellingPrice = 16000L, // Rs. 160
            stockQuantity = 4,
            lowStockThreshold = 5
        )

        assertEquals(56000L, product.stockValue) // 4 * 140 = 560
        assertTrue(product.isLowStock) // 4 <= 5 is low stock

        val healthyProduct = product.copy(stockQuantity = 10)
        assertFalse(healthyProduct.isLowStock)
    }

    @Test
    fun testBackupDataSerializationAndDeserialization() {
        val backup = BackupData(
            version = 1,
            exportedAt = 1700000000000L,
            shopName = "Bismillah General Store",
            products = listOf(
                Product(id = 1L, name = "Rice Basmati", purchasePrice = 25000L, sellingPrice = 30000L, stockQuantity = 20)
            ),
            purchases = listOf(
                Purchase(id = 1L, productId = 1L, quantity = 20, purchasePrice = 25000L, totalAmount = 500000L, date = 1700000000000L)
            ),
            sales = listOf(
                Sale(id = 1L, productId = 1L, quantity = 2, sellingPrice = 30000L, costPrice = 25000L, totalAmount = 60000L, costAmount = 50000L, profit = 10000L, date = 1700000000000L)
            ),
            expenses = listOf(
                Expense(id = 1L, title = "Tea & Refreshment", amount = 15000L, date = 1700000000000L)
            )
        )

        val jsonString = backup.toJsonString()
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Bismillah General Store"))
        assertTrue(jsonString.contains("Rice Basmati"))

        val restored = BackupData.fromJsonString(jsonString)
        assertEquals("Bismillah General Store", restored.shopName)
        assertEquals(1, restored.products.size)
        assertEquals("Rice Basmati", restored.products[0].name)
        assertEquals(25000L, restored.products[0].purchasePrice)
        assertEquals(1, restored.sales.size)
        assertEquals(10000L, restored.sales[0].profit)
        assertEquals(1, restored.expenses.size)
        assertEquals(15000L, restored.expenses[0].amount)
    }

    @Test
    fun testDateUtilsRanges() {
        val now = System.currentTimeMillis()
        val (todayStart, todayEnd) = DateUtils.todayRange()
        assertTrue(todayStart <= now)
        assertTrue(todayEnd >= now)
        assertTrue(todayEnd > todayStart)

        val (yesterdayStart, yesterdayEnd) = DateUtils.yesterdayRange()
        assertTrue(yesterdayEnd < todayStart)

        val (weekStart, weekEnd) = DateUtils.thisWeekRange()
        assertTrue(weekStart <= todayStart)
        assertTrue(weekEnd >= todayEnd)
    }

    @Test
    fun testBoxToPacketProductValuationAndBreakdown() {
        // 1 Box = 12 Packets, Rs. 100/box, Rs. 10/packet
        val product = Product(
            id = 1L,
            name = "Test Biscuit",
            purchaseUnit = "Box",
            saleUnit = "Packet",
            unitsPerPurchaseUnit = 12,
            purchasePrice = 10000L, // Rs. 100 per box
            sellingPrice = 1000L, // Rs. 10 per packet
            stockQuantity = 31, // 31 packets = 2 boxes + 7 packets
            totalCostPool = 25833L // ~Rs. 258.33
        )

        assertEquals("2 Box + 7 Packet", product.stockBreakdown)
        assertEquals("31 Packet (2 Box + 7 Packet)", product.fullStockDisplay)
        // Unit cost: 25833 / 31 = 833.32 paisas = ~Rs. 8.33
        assertEquals(833L, product.unitCostPaisas)
        assertEquals(25833L, product.stockValue)

        // Empty stock fallback uses purchasePrice / unitsPerPurchaseUnit (10000 / 12 = 833.33)
        val emptyProduct = product.copy(stockQuantity = 0, totalCostPool = 0L)
        assertEquals(833L, emptyProduct.unitCostPaisas)
    }

    @Test
    fun testBoxToPacketFullScenario() = kotlinx.coroutines.runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = com.example.data.repository.ShopRepository(db)

        // 1. Add Product: "Test Biscuit", 1 Box = 12 Packets, Rs. 100/Box, Rs. 10/Packet
        val addResult = repository.addProduct(
            name = "Test Biscuit",
            purchaseUnit = "Box",
            saleUnit = "Packet",
            unitsPerPurchaseUnit = 12,
            purchasePrice = 10000L, // Rs. 100 in paisas
            sellingPrice = 1000L, // Rs. 10 in paisas
            openingStock = 0,
            lowStockThreshold = 5
        )
        assertTrue(addResult.isSuccess)
        val productId = addResult.getOrThrow()

        // Verify product initial state
        var prod = db.productDao().getProductByIdDirect(productId)
        assertNotNull(prod)
        assertEquals(0, prod!!.stockQuantity)
        assertEquals(0L, prod.totalCostPool)

        // 2. Purchase 1 Box for Rs. 100
        val purchaseResult = repository.recordPurchase(
            productId = productId,
            quantity = 1, // 1 Box
            purchasePrice = 10000L, // Rs. 100
            date = System.currentTimeMillis(),
            note = "Initial box purchase"
        )
        assertTrue(purchaseResult.isSuccess)

        // Verify stock is now 12 Packets and cost pool is Rs. 100 (10000 paisas)
        prod = db.productDao().getProductByIdDirect(productId)
        assertNotNull(prod)
        assertEquals(12, prod!!.stockQuantity)
        assertEquals(10000L, prod.totalCostPool)
        assertEquals(833L, prod.unitCostPaisas) // 100 / 12 = 8.33

        // 3. Sell all 12 Packets at Rs. 10 each
        val saleResult = repository.recordSale(
            productId = productId,
            quantity = 12, // 12 Packets
            sellingPrice = 1000L, // Rs. 10 per packet
            date = System.currentTimeMillis(),
            note = "Sold whole box in packets"
        )
        assertTrue(saleResult.isSuccess)
        val saleId = saleResult.getOrThrow()

        // 4. Verify Sale financial calculations:
        // Total Sale = 12 * Rs. 10 = Rs. 120 (12000 paisas)
        // Total Cost = Rs. 100 (10000 paisas)
        // Gross Profit = Rs. 20 (2000 paisas)
        val sale = db.saleDao().getSaleById(saleId)
        assertNotNull(sale)
        assertEquals(12, sale!!.quantity)
        assertEquals(1000L, sale.sellingPrice)
        assertEquals(12000L, sale.totalAmount) // Rs. 120
        assertEquals(10000L, sale.costAmount) // Rs. 100
        assertEquals(2000L, sale.profit) // Rs. 20 Profit!

        // 5. Verify Product ending state: 0 packets left, 0 cost pool
        prod = db.productDao().getProductByIdDirect(productId)
        assertNotNull(prod)
        assertEquals(0, prod!!.stockQuantity)
        assertEquals(0L, prod.totalCostPool)

        db.close()
    }
}
