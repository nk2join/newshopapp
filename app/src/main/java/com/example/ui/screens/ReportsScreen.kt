package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DateFilterType
import com.example.ui.ShopViewModel
import com.example.ui.components.ShopDatePickerField
import com.example.ui.components.StatMetricCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.MoneyUtils

@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val reportSummary by viewModel.reportSummary.collectAsStateWithLifecycle()
    val reportTransactions by viewModel.reportTransactions.collectAsStateWithLifecycle()
    val dateFilter by viewModel.reportDateFilter.collectAsStateWithLifecycle()
    val customFrom by viewModel.customReportFromDate.collectAsStateWithLifecycle()
    val customTo by viewModel.customReportToDate.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter selection row
        item {
            Column {
                Text(
                    text = "Reports & Profit Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateFilter == DateFilterType.TODAY,
                        onClick = { viewModel.reportDateFilter.value = DateFilterType.TODAY },
                        label = { Text("Today") },
                        modifier = Modifier.testTag("report_filter_today")
                    )
                    FilterChip(
                        selected = dateFilter == DateFilterType.YESTERDAY,
                        onClick = { viewModel.reportDateFilter.value = DateFilterType.YESTERDAY },
                        label = { Text("Yesterday") },
                        modifier = Modifier.testTag("report_filter_yesterday")
                    )
                    FilterChip(
                        selected = dateFilter == DateFilterType.THIS_WEEK,
                        onClick = { viewModel.reportDateFilter.value = DateFilterType.THIS_WEEK },
                        label = { Text("This Week") },
                        modifier = Modifier.testTag("report_filter_week")
                    )
                    FilterChip(
                        selected = dateFilter == DateFilterType.THIS_MONTH,
                        onClick = { viewModel.reportDateFilter.value = DateFilterType.THIS_MONTH },
                        label = { Text("This Month") },
                        modifier = Modifier.testTag("report_filter_month")
                    )
                    FilterChip(
                        selected = dateFilter == DateFilterType.CUSTOM,
                        onClick = { viewModel.reportDateFilter.value = DateFilterType.CUSTOM },
                        label = { Text("Custom Range") },
                        modifier = Modifier.testTag("report_filter_custom")
                    )
                }

                if (dateFilter == DateFilterType.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShopDatePickerField(
                            label = "From",
                            selectedDate = customFrom,
                            onDateSelected = { viewModel.customReportFromDate.value = it },
                            modifier = Modifier.weight(1f)
                        )
                        ShopDatePickerField(
                            label = "To",
                            selectedDate = customTo,
                            onDateSelected = { viewModel.customReportToDate.value = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Net Profit Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("report_net_profit_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (reportSummary.netProfit >= 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Net Profit for Selected Period",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MoneyUtils.formatRupees(reportSummary.netProfit),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reportSummary.netProfit >= 0) ProfitGreen else LossRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Net Profit = Gross Profit (${MoneyUtils.formatRupees(reportSummary.grossProfit)}) − Expenses (${MoneyUtils.formatRupees(reportSummary.totalExpenses)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // P&L Statement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Profit & Loss Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Sales Revenue (${reportSummary.itemsSold} items)")
                        Text(
                            MoneyUtils.formatRupees(reportSummary.totalSales),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cost of Goods Sold (COGS)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val cogs = reportSummary.totalSales - reportSummary.grossProfit
                        Text(
                            "− " + MoneyUtils.formatRupees(cogs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Profit", fontWeight = FontWeight.Bold)
                        Text(
                            MoneyUtils.formatRupees(reportSummary.grossProfit),
                            fontWeight = FontWeight.Bold,
                            color = if (reportSummary.grossProfit >= 0) ProfitGreen else LossRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Operating Expenses", color = LossRed)
                        Text(
                            "− " + MoneyUtils.formatRupees(reportSummary.totalExpenses),
                            color = LossRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Profit / Loss", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            MoneyUtils.formatRupees(reportSummary.netProfit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (reportSummary.netProfit >= 0) ProfitGreen else LossRed
                        )
                    }
                }
            }
        }

        // Summary Metric Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Total Sales",
                        value = MoneyUtils.formatRupees(reportSummary.totalSales),
                        subtitle = "${reportSummary.itemsSold} items sold",
                        icon = Icons.Default.ShoppingBag,
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Purchases Made",
                        value = MoneyUtils.formatRupees(reportSummary.totalPurchase),
                        subtitle = "${reportSummary.itemsPurchased} items bought",
                        icon = Icons.Default.ShoppingCart,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Gross Profit",
                        value = MoneyUtils.formatRupees(reportSummary.grossProfit),
                        subtitle = "Margin on sales",
                        icon = Icons.Default.TrendingUp,
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Total Expenses",
                        value = MoneyUtils.formatRupees(reportSummary.totalExpenses),
                        subtitle = "Shop overheads",
                        icon = Icons.Default.Receipt,
                        accentColor = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Period Transactions Header
        item {
            Text(
                text = "Period Transactions (${reportTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (reportTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No transactions occurred during this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(reportTransactions, key = { "${it.type}_${it.id}" }) { item ->
                TransactionRowItem(item = item)
            }
        }
    }
}
