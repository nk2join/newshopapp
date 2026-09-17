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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.DateFilterType
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ShopDatePickerField
import com.example.ui.components.TransactionRowItem
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    viewModel: ShopViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val dateFilter by viewModel.transactionDateFilter.collectAsStateWithLifecycle()
    val customFrom by viewModel.customTransFromDate.collectAsStateWithLifecycle()
    val customTo by viewModel.customTransToDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var itemToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    Column(
        modifier = modifier.fillMaxSize().testTag("transactions_screen")
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.transactionSearchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .testTag("transactions_search_input"),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.transactionSearchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = dateFilter == DateFilterType.TODAY,
                onClick = { viewModel.transactionDateFilter.value = DateFilterType.TODAY },
                label = { Text("Today") },
                modifier = Modifier.testTag("filter_today")
            )
            FilterChip(
                selected = dateFilter == DateFilterType.YESTERDAY,
                onClick = { viewModel.transactionDateFilter.value = DateFilterType.YESTERDAY },
                label = { Text("Yesterday") },
                modifier = Modifier.testTag("filter_yesterday")
            )
            FilterChip(
                selected = dateFilter == DateFilterType.THIS_WEEK,
                onClick = { viewModel.transactionDateFilter.value = DateFilterType.THIS_WEEK },
                label = { Text("This Week") },
                modifier = Modifier.testTag("filter_week")
            )
            FilterChip(
                selected = dateFilter == DateFilterType.THIS_MONTH,
                onClick = { viewModel.transactionDateFilter.value = DateFilterType.THIS_MONTH },
                label = { Text("This Month") },
                modifier = Modifier.testTag("filter_month")
            )
            FilterChip(
                selected = dateFilter == DateFilterType.CUSTOM,
                onClick = { viewModel.transactionDateFilter.value = DateFilterType.CUSTOM },
                label = { Text("Custom Range") },
                modifier = Modifier.testTag("filter_custom")
            )
        }

        // Custom Date Range Row if selected
        if (dateFilter == DateFilterType.CUSTOM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShopDatePickerField(
                    label = "From",
                    selectedDate = customFrom,
                    onDateSelected = { viewModel.customTransFromDate.value = it },
                    modifier = Modifier.weight(1f)
                )
                ShopDatePickerField(
                    label = "To",
                    selectedDate = customTo,
                    onDateSelected = { viewModel.customTransToDate.value = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Transactions count label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${transactions.size} transactions found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (transactions.isEmpty()) {
            EmptyStateView(
                title = "No transactions found",
                subtitle = "No sales, purchases, or expenses match your selected date range or search query.",
                icon = Icons.Default.ReceiptLong,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { "${it.type}_${it.id}" }) { item ->
                    TransactionRowItem(
                        item = item,
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        val typeLabel = when (target.type) {
            TransactionType.PURCHASE -> "Purchase"
            TransactionType.SALE -> "Sale"
            TransactionType.EXPENSE -> "Expense"
        }
        val extraWarning = when (target.type) {
            TransactionType.PURCHASE -> "Deleting this purchase will revert the added stock (${target.quantity ?: 0} units) from the product inventory."
            TransactionType.SALE -> "Deleting this sale will restore the sold stock (${target.quantity ?: 0} units) back to inventory."
            TransactionType.EXPENSE -> "This expense record will be permanently removed."
        }

        ConfirmDeleteDialog(
            title = "Delete $typeLabel",
            message = "Are you sure you want to delete this $typeLabel transaction (${target.title})?\n\n$extraWarning",
            confirmText = "Delete",
            onConfirm = {
                when (target.type) {
                    TransactionType.PURCHASE -> {
                        viewModel.deletePurchase(target.id) { res ->
                            itemToDelete = null
                            scope.launch {
                                if (res.isSuccess) snackbarHostState.showSnackbar("Purchase deleted and stock restored.")
                                else snackbarHostState.showSnackbar("Error: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    }
                    TransactionType.SALE -> {
                        viewModel.deleteSale(target.id) { res ->
                            itemToDelete = null
                            scope.launch {
                                if (res.isSuccess) snackbarHostState.showSnackbar("Sale deleted and stock restored.")
                                else snackbarHostState.showSnackbar("Error: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    }
                    TransactionType.EXPENSE -> {
                        viewModel.deleteExpense(target.id) { res ->
                            itemToDelete = null
                            scope.launch {
                                if (res.isSuccess) snackbarHostState.showSnackbar("Expense deleted.")
                                else snackbarHostState.showSnackbar("Error: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }
            },
            onDismiss = { itemToDelete = null }
        )
    }
}
