package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BackupData
import com.example.data.model.Expense
import com.example.data.model.PeriodSummary
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.TransactionItem
import com.example.data.repository.ShopRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DateFilterType {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = ShopRepository(database)

    // Shop Name
    val shopName: StateFlow<String> = repository.shopNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "My Shop"
    )

    // Products
    val allActiveProducts: StateFlow<List<Product>> = repository.allActiveProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Product search query
    val productSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedProducts: StateFlow<List<Product>> = productSearchQuery.flatMapLatest { query ->
        repository.searchProducts(query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's summary for Dashboard
    private val todayRange = DateUtils.todayRange()
    val todaySummary: StateFlow<PeriodSummary> = repository.getPeriodSummary(todayRange.first, todayRange.second).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeriodSummary()
    )

    // Recent transactions for Dashboard (last 10)
    val recentTransactions: StateFlow<List<TransactionItem>> = repository.getTransactionsBetween(0L, Long.MAX_VALUE).combine(todaySummary) { list, _ ->
        list.take(10)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Transactions History Screen State ---
    val transactionDateFilter = MutableStateFlow(DateFilterType.TODAY)
    val customTransFromDate = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))
    val customTransToDate = MutableStateFlow(DateUtils.endOfDay(System.currentTimeMillis()))
    val transactionSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        transactionDateFilter,
        customTransFromDate,
        customTransToDate
    ) { filter, from, to ->
        when (filter) {
            DateFilterType.TODAY -> DateUtils.todayRange()
            DateFilterType.YESTERDAY -> DateUtils.yesterdayRange()
            DateFilterType.THIS_WEEK -> DateUtils.thisWeekRange()
            DateFilterType.THIS_MONTH -> DateUtils.thisMonthRange()
            DateFilterType.CUSTOM -> DateUtils.customRange(from, to)
        }
    }.flatMapLatest { (start, end) ->
        repository.getTransactionsBetween(start, end)
    }.combine(transactionSearchQuery) { transactions, query ->
        if (query.isBlank()) {
            transactions
        } else {
            val q = query.trim().lowercase()
            transactions.filter {
                it.title.lowercase().contains(q) || it.note.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Reports Screen State ---
    val reportDateFilter = MutableStateFlow(DateFilterType.TODAY)
    val customReportFromDate = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))
    val customReportToDate = MutableStateFlow(DateUtils.endOfDay(System.currentTimeMillis()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportSummary: StateFlow<PeriodSummary> = combine(
        reportDateFilter,
        customReportFromDate,
        customReportToDate
    ) { filter, from, to ->
        when (filter) {
            DateFilterType.TODAY -> DateUtils.todayRange()
            DateFilterType.YESTERDAY -> DateUtils.yesterdayRange()
            DateFilterType.THIS_WEEK -> DateUtils.thisWeekRange()
            DateFilterType.THIS_MONTH -> DateUtils.thisMonthRange()
            DateFilterType.CUSTOM -> DateUtils.customRange(from, to)
        }
    }.flatMapLatest { (start, end) ->
        repository.getPeriodSummary(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeriodSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportTransactions: StateFlow<List<TransactionItem>> = combine(
        reportDateFilter,
        customReportFromDate,
        customReportToDate
    ) { filter, from, to ->
        when (filter) {
            DateFilterType.TODAY -> DateUtils.todayRange()
            DateFilterType.YESTERDAY -> DateUtils.yesterdayRange()
            DateFilterType.THIS_WEEK -> DateUtils.thisWeekRange()
            DateFilterType.THIS_MONTH -> DateUtils.thisMonthRange()
            DateFilterType.CUSTOM -> DateUtils.customRange(from, to)
        }
    }.flatMapLatest { (start, end) ->
        repository.getTransactionsBetween(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Purchases
    val allPurchases: StateFlow<List<Purchase>> = repository.allPurchases.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Sales
    val allSales: StateFlow<List<Sale>> = repository.allSales.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Expenses List ---
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Product Operations ---
    fun addProduct(
        name: String,
        purchaseUnit: String,
        saleUnit: String,
        unitsPerPurchaseUnit: Int,
        purchasePrice: Long,
        sellingPrice: Long,
        openingStock: Int,
        lowStockThreshold: Int = 5,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.addProduct(
                name = name,
                purchaseUnit = purchaseUnit,
                saleUnit = saleUnit,
                unitsPerPurchaseUnit = unitsPerPurchaseUnit,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                openingStock = openingStock,
                lowStockThreshold = lowStockThreshold
            )
            onResult(res)
        }
    }

    fun updateProduct(
        id: Long,
        name: String,
        purchaseUnit: String,
        saleUnit: String,
        unitsPerPurchaseUnit: Int,
        purchasePrice: Long,
        sellingPrice: Long,
        lowStockThreshold: Int,
        manualStockAdjustment: Int? = null,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateProduct(
                id = id,
                name = name,
                purchaseUnit = purchaseUnit,
                saleUnit = saleUnit,
                unitsPerPurchaseUnit = unitsPerPurchaseUnit,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                lowStockThreshold = lowStockThreshold,
                manualStockAdjustment = manualStockAdjustment
            )
            onResult(res)
        }
    }

    fun deleteProduct(id: Long, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteProduct(id)
            onResult(res)
        }
    }

    // --- Purchase Operations ---
    fun recordPurchase(
        productId: Long,
        quantity: Int,
        purchasePrice: Long,
        date: Long,
        note: String,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordPurchase(productId, quantity, purchasePrice, date, note)
            onResult(res)
        }
    }

    fun updatePurchase(
        purchaseId: Long,
        quantity: Int,
        purchasePrice: Long,
        date: Long,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updatePurchase(purchaseId, quantity, purchasePrice, date, note)
            onResult(res)
        }
    }

    fun deletePurchase(purchaseId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deletePurchase(purchaseId)
            onResult(res)
        }
    }

    // --- Sale Operations ---
    fun recordSale(
        productId: Long,
        quantity: Int,
        sellingPrice: Long,
        date: Long,
        note: String,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordSale(productId, quantity, sellingPrice, date, note)
            onResult(res)
        }
    }

    fun updateSale(
        saleId: Long,
        quantity: Int,
        sellingPrice: Long,
        date: Long,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateSale(saleId, quantity, sellingPrice, date, note)
            onResult(res)
        }
    }

    fun deleteSale(saleId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteSale(saleId)
            onResult(res)
        }
    }

    // --- Expense Operations ---
    fun addExpense(
        title: String,
        amount: Long,
        date: Long,
        note: String,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.addExpense(title, amount, date, note)
            onResult(res)
        }
    }

    fun updateExpense(
        id: Long,
        title: String,
        amount: Long,
        date: Long,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateExpense(id, title, amount, date, note)
            onResult(res)
        }
    }

    fun deleteExpense(id: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteExpense(id)
            onResult(res)
        }
    }

    // --- Settings Operations ---
    fun updateShopName(name: String) {
        viewModelScope.launch {
            repository.setShopName(name)
        }
    }

    // --- Backup & Restore ---
    suspend fun exportBackupJson(): String {
        val backup = repository.createBackup()
        return backup.toJsonString()
    }

    fun restoreBackupFromJson(jsonString: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = BackupData.fromJsonString(jsonString)
                val res = repository.restoreBackup(backup)
                onResult(res)
            } catch (e: Exception) {
                onResult(Result.failure(IllegalArgumentException("Could not restore this backup file. Invalid format.")))
            }
        }
    }
}
