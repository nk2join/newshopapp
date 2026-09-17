package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionItem
import com.example.ui.ShopViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatMetricCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.DateUtils
import com.example.util.MoneyUtils

@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToSale: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val allProducts by viewModel.allActiveProducts.collectAsStateWithLifecycle()
    val shopName by viewModel.shopName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shop Greeting & Date
        item {
            Column {
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Today • ${DateUtils.formatDate(System.currentTimeMillis())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // If no products exist yet, show a welcoming CTA
        if (allProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("add_first_product_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Add your first product",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Get started by adding products to your shop inventory. Then record purchases and sales to start tracking profits automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToProducts,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("add_first_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Add Product")
                        }
                    }
                }
            }
        }

        // Primary Financial Highlights
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Today's Net Profit Card (Big emphasis)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("net_profit_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todaySummary.netProfit >= 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Today's Net Profit",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatRupees(todaySummary.netProfit),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (todaySummary.netProfit >= 0) ProfitGreen else LossRed
                            )
                            Text(
                                text = "Gross: ${MoneyUtils.formatRupees(todaySummary.grossProfit)}  |  Expenses: ${MoneyUtils.formatRupees(todaySummary.totalExpenses)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grid 1: Sales & Purchases
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Today's Sales",
                        value = MoneyUtils.formatRupees(todaySummary.totalSales),
                        subtitle = "${todaySummary.itemsSold} items sold",
                        icon = Icons.Default.ShoppingBag,
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f).testTag("today_sales_card")
                    )
                    StatMetricCard(
                        title = "Today's Purchase",
                        value = MoneyUtils.formatRupees(todaySummary.totalPurchase),
                        subtitle = "${todaySummary.itemsPurchased} items bought",
                        icon = Icons.Default.ShoppingCart,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f).testTag("today_purchase_card")
                    )
                }

                // Grid 2: Gross Profit & Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Gross Profit",
                        value = MoneyUtils.formatRupees(todaySummary.grossProfit),
                        subtitle = "Sales − Cost of Goods",
                        icon = Icons.Default.TrendingUp,
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f).testTag("gross_profit_card")
                    )
                    StatMetricCard(
                        title = "Today's Expenses",
                        value = MoneyUtils.formatRupees(todaySummary.totalExpenses),
                        subtitle = "Shop operations",
                        icon = Icons.Default.Receipt,
                        accentColor = LossRed,
                        modifier = Modifier.weight(1f).testTag("today_expenses_card")
                    )
                }

                // Grid 3: Stock Items & Inventory Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Current Stock Items",
                        value = "${todaySummary.activeProductsCount} Products",
                        subtitle = "${todaySummary.totalStockUnits} Total units",
                        icon = Icons.Default.Category,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).testTag("stock_items_card")
                    )
                    StatMetricCard(
                        title = "Total Stock Value",
                        value = MoneyUtils.formatRupees(todaySummary.totalStockValue),
                        subtitle = "At purchase cost",
                        icon = Icons.Default.Inventory,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f).testTag("stock_value_card")
                    )
                }
            }
        }

        // Fast Action Buttons Row
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToSale,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    modifier = Modifier.weight(1f).testTag("quick_sale_button")
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("New Sale")
                }
                Button(
                    onClick = onNavigateToPurchase,
                    modifier = Modifier.weight(1f).testTag("quick_purchase_button")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("Purchase")
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.testTag("view_all_transactions_button")
                ) {
                    Text("View All")
                }
            }
        }

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No transactions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { "${it.type}_${it.id}" }) { item ->
                TransactionRowItem(item = item)
            }
        }
    }
}
