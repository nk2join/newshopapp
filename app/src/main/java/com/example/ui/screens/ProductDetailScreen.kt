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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.ShopViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningOrange
import com.example.util.DateUtils
import com.example.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    viewModel: ShopViewModel,
    onBack: () -> Unit,
    onRecordPurchase: (Long) -> Unit,
    onRecordSale: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val productFlow = viewModel.repository.getProductById(productId)
    val product by productFlow.collectAsStateWithLifecycle(initialValue = null)

    val purchasesFlow = viewModel.repository.getPurchasesForProduct(productId)
    val purchases by purchasesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val salesFlow = viewModel.repository.getSalesForProduct(productId)
    val sales by salesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val productTransactions = (purchases.map { p ->
        TransactionItem(
            id = p.id,
            type = TransactionType.PURCHASE,
            title = product?.name ?: "Product",
            productId = p.productId,
            quantity = p.quantity,
            unitPrice = p.purchasePrice,
            amount = p.totalAmount,
            date = p.date,
            note = p.note
        )
    } + sales.map { s ->
        TransactionItem(
            id = s.id,
            type = TransactionType.SALE,
            title = product?.name ?: "Product",
            productId = s.productId,
            quantity = s.quantity,
            unitPrice = s.sellingPrice,
            amount = s.totalAmount,
            costAmount = s.costAmount,
            profit = s.profit,
            date = s.date,
            note = s.note
        )
    }).sortedWith(compareByDescending<TransactionItem> { it.date }.thenByDescending { it.id })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("product_detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (product == null) {
            EmptyStateView(
                title = "Product not found",
                subtitle = "This product may have been removed.",
                actionButtonText = "Go Back",
                onActionClick = onBack,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val p = product!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("product_detail_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (p.isLowStock) {
                                    Surface(
                                        color = WarningOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.padding(end = 4.dp))
                                            Text(
                                                text = "Low Stock Alert (${p.stockQuantity} ${p.saleUnit})",
                                                color = WarningOrange,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (p.unitsPerPurchaseUnit > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "1 ${p.purchaseUnit} = ${p.unitsPerPurchaseUnit} ${p.saleUnit}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Current Stock", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(p.stockBreakdown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (p.unitsPerPurchaseUnit > 1) {
                                        Text("(${p.stockQuantity} ${p.saleUnit})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column {
                                    Text("Cost / ${p.purchaseUnit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyUtils.formatRupees(p.purchasePrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (p.unitsPerPurchaseUnit > 1) {
                                        Text("(${MoneyUtils.formatDoubleRupees(p.unitCostDoublePaisas)}/${p.saleUnit})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column {
                                    Text("Sell / ${p.saleUnit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyUtils.formatRupees(p.sellingPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    val unitProfit = p.sellingPrice.toDouble() - p.unitCostDoublePaisas
                                    Text("Profit: ${MoneyUtils.formatDoubleRupees(unitProfit)}", style = MaterialTheme.typography.labelSmall, color = ProfitGreen, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Stock Value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyUtils.formatRupees(p.stockValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Low Stock Threshold: ${p.lowStockThreshold} ${p.saleUnit}s  •  Updated: ${DateUtils.formatDate(p.updatedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onRecordSale(p.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            modifier = Modifier.weight(1f).testTag("product_record_sale_button")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("Sell (${p.saleUnit})")
                        }
                        Button(
                            onClick = { onRecordPurchase(p.id) },
                            modifier = Modifier.weight(1f).testTag("product_record_purchase_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("Buy (${p.purchaseUnit})")
                        }
                    }
                }

                // Product Transaction History Header
                item {
                    Text(
                        text = "Transaction History (${productTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (productTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No purchase or sale transactions for this product yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(productTransactions, key = { "${it.type}_${it.id}" }) { item ->
                        TransactionRowItem(item = item)
                    }
                }
            }
        }
    }
}
