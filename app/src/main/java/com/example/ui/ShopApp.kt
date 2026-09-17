package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.PurchaseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SaleScreen
import com.example.ui.screens.SettingsBackupScreen
import com.example.ui.screens.TransactionsScreen

enum class MainNavTab {
    DASHBOARD,
    PRODUCTS,
    PURCHASES,
    SALES,
    LEDGER
}

enum class LedgerSubTab {
    TRANSACTIONS,
    REPORTS,
    EXPENSES,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopApp(
    viewModel: ShopViewModel = viewModel()
) {
    val shopName by viewModel.shopName.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentMainTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
    var currentLedgerSubTab by remember { mutableStateOf(LedgerSubTab.TRANSACTIONS) }

    // Product Detail Navigation State
    var selectedProductIdForDetail by remember { mutableStateOf<Long?>(null) }

    // Preselected product ID for Purchase / Sale
    var preselectedProductIdForPurchase by remember { mutableStateOf<Long?>(null) }
    var preselectedProductIdForSale by remember { mutableStateOf<Long?>(null) }

    val topBarTitle = when {
        selectedProductIdForDetail != null -> "Product Detail"
        currentMainTab == MainNavTab.DASHBOARD -> shopName
        currentMainTab == MainNavTab.PRODUCTS -> "Product Inventory"
        currentMainTab == MainNavTab.PURCHASES -> "Purchases & Stock In"
        currentMainTab == MainNavTab.SALES -> "Sales & Profit"
        currentMainTab == MainNavTab.LEDGER -> when (currentLedgerSubTab) {
            LedgerSubTab.TRANSACTIONS -> "All Transactions"
            LedgerSubTab.REPORTS -> "Reports & P&L"
            LedgerSubTab.EXPENSES -> "Shop Expenses"
            LedgerSubTab.SETTINGS -> "Settings & Backup"
        }
        else -> shopName
    }

    Scaffold(
        topBar = {
            if (selectedProductIdForDetail == null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topBarTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (currentMainTab != MainNavTab.DASHBOARD) {
                                Text(
                                    text = shopName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                currentMainTab = MainNavTab.LEDGER
                                currentLedgerSubTab = LedgerSubTab.SETTINGS
                            },
                            modifier = Modifier.testTag("top_app_bar_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings & Backup")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (selectedProductIdForDetail == null) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentMainTab == MainNavTab.DASHBOARD,
                        onClick = { currentMainTab = MainNavTab.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentMainTab == MainNavTab.PRODUCTS,
                        onClick = { currentMainTab = MainNavTab.PRODUCTS },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "Products") },
                        label = { Text("Products") },
                        modifier = Modifier.testTag("nav_item_products")
                    )
                    NavigationBarItem(
                        selected = currentMainTab == MainNavTab.PURCHASES,
                        onClick = {
                            preselectedProductIdForPurchase = null
                            currentMainTab = MainNavTab.PURCHASES
                        },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Purchase") },
                        label = { Text("Purchase") },
                        modifier = Modifier.testTag("nav_item_purchases")
                    )
                    NavigationBarItem(
                        selected = currentMainTab == MainNavTab.SALES,
                        onClick = {
                            preselectedProductIdForSale = null
                            currentMainTab = MainNavTab.SALES
                        },
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Sale") },
                        label = { Text("Sale") },
                        modifier = Modifier.testTag("nav_item_sales")
                    )
                    NavigationBarItem(
                        selected = currentMainTab == MainNavTab.LEDGER,
                        onClick = { currentMainTab = MainNavTab.LEDGER },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                        label = { Text("Ledger") },
                        modifier = Modifier.testTag("nav_item_ledger")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedProductIdForDetail != null) {
                ProductDetailScreen(
                    productId = selectedProductIdForDetail!!,
                    viewModel = viewModel,
                    onBack = { selectedProductIdForDetail = null },
                    onRecordPurchase = { prodId ->
                        selectedProductIdForDetail = null
                        preselectedProductIdForPurchase = prodId
                        currentMainTab = MainNavTab.PURCHASES
                    },
                    onRecordSale = { prodId ->
                        selectedProductIdForDetail = null
                        preselectedProductIdForSale = prodId
                        currentMainTab = MainNavTab.SALES
                    }
                )
            } else {
                when (currentMainTab) {
                    MainNavTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToProducts = { currentMainTab = MainNavTab.PRODUCTS },
                            onNavigateToPurchase = {
                                preselectedProductIdForPurchase = null
                                currentMainTab = MainNavTab.PURCHASES
                            },
                            onNavigateToSale = {
                                preselectedProductIdForSale = null
                                currentMainTab = MainNavTab.SALES
                            },
                            onNavigateToExpenses = {
                                currentMainTab = MainNavTab.LEDGER
                                currentLedgerSubTab = LedgerSubTab.EXPENSES
                            },
                            onNavigateToTransactions = {
                                currentMainTab = MainNavTab.LEDGER
                                currentLedgerSubTab = LedgerSubTab.TRANSACTIONS
                            }
                        )
                    }

                    MainNavTab.PRODUCTS -> {
                        ProductsScreen(
                            viewModel = viewModel,
                            onProductClick = { prodId -> selectedProductIdForDetail = prodId },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    MainNavTab.PURCHASES -> {
                        PurchaseScreen(
                            viewModel = viewModel,
                            preselectedProductId = preselectedProductIdForPurchase,
                            onNavigateToProducts = { currentMainTab = MainNavTab.PRODUCTS },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    MainNavTab.SALES -> {
                        SaleScreen(
                            viewModel = viewModel,
                            preselectedProductId = preselectedProductIdForSale,
                            onNavigateToProducts = { currentMainTab = MainNavTab.PRODUCTS },
                            snackbarHostState = snackbarHostState
                        )
                    }

                    MainNavTab.LEDGER -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SecondaryTabRow(
                                selectedTabIndex = currentLedgerSubTab.ordinal,
                                modifier = Modifier.fillMaxWidth().testTag("ledger_subtabs")
                            ) {
                                Tab(
                                    selected = currentLedgerSubTab == LedgerSubTab.TRANSACTIONS,
                                    onClick = { currentLedgerSubTab = LedgerSubTab.TRANSACTIONS },
                                    text = { Text("History") },
                                    modifier = Modifier.testTag("tab_history")
                                )
                                Tab(
                                    selected = currentLedgerSubTab == LedgerSubTab.REPORTS,
                                    onClick = { currentLedgerSubTab = LedgerSubTab.REPORTS },
                                    text = { Text("Reports") },
                                    modifier = Modifier.testTag("tab_reports")
                                )
                                Tab(
                                    selected = currentLedgerSubTab == LedgerSubTab.EXPENSES,
                                    onClick = { currentLedgerSubTab = LedgerSubTab.EXPENSES },
                                    text = { Text("Expenses") },
                                    modifier = Modifier.testTag("tab_expenses")
                                )
                                Tab(
                                    selected = currentLedgerSubTab == LedgerSubTab.SETTINGS,
                                    onClick = { currentLedgerSubTab = LedgerSubTab.SETTINGS },
                                    text = { Text("Backup") },
                                    modifier = Modifier.testTag("tab_backup")
                                )
                            }

                            when (currentLedgerSubTab) {
                                LedgerSubTab.TRANSACTIONS -> TransactionsScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
                                )
                                LedgerSubTab.REPORTS -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                LedgerSubTab.EXPENSES -> ExpensesScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
                                )
                                LedgerSubTab.SETTINGS -> SettingsBackupScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
