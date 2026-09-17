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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ShopDatePickerField
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LossRed
import com.example.util.MoneyUtils
import kotlinx.coroutines.launch

@Composable
fun ExpensesScreen(
    viewModel: ShopViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val totalExpensesPaisas = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    Box(modifier = modifier.fillMaxSize().testTag("expenses_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Recorded Expenses",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatRupees(totalExpensesPaisas),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = LossRed
                            )
                            Text(
                                text = "${expenses.size} expenses recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No expenses recorded",
                        subtitle = "Track utility bills, rent, refreshments, packaging, and other shop expenses.",
                        actionButtonText = "Add Expense",
                        onActionClick = { showAddDialog = true },
                        icon = Icons.Default.Receipt
                    )
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    val transactionItem = TransactionItem(
                        id = expense.id,
                        type = TransactionType.EXPENSE,
                        title = expense.title,
                        amount = expense.amount,
                        date = expense.date,
                        note = expense.note
                    )

                    TransactionRowItem(
                        item = transactionItem,
                        onEdit = { expenseToEdit = expense },
                        onDelete = { expenseToDelete = expense }
                    )
                }
            }
        }

        // FAB to Add Expense
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_expense_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }
    }

    // Add Expense Dialog
    if (showAddDialog) {
        AddEditExpenseDialog(
            expense = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, date, note ->
                viewModel.addExpense(title, amount, date, note) { res ->
                    if (res.isSuccess) {
                        showAddDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Expense added.") }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error adding expense.") }
                    }
                }
            }
        )
    }

    // Edit Expense Dialog
    if (expenseToEdit != null) {
        AddEditExpenseDialog(
            expense = expenseToEdit,
            onDismiss = { expenseToEdit = null },
            onSave = { title, amount, date, note ->
                viewModel.updateExpense(expenseToEdit!!.id, title, amount, date, note) { res ->
                    if (res.isSuccess) {
                        expenseToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Expense updated.") }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error updating expense.") }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense",
            message = "Are you sure you want to delete '${expenseToDelete!!.title}' (${MoneyUtils.formatRupees(expenseToDelete!!.amount)})?",
            confirmText = "Delete",
            onConfirm = {
                val exp = expenseToDelete!!
                viewModel.deleteExpense(exp.id) { res ->
                    expenseToDelete = null
                    scope.launch {
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Expense deleted.")
                        } else {
                            snackbarHostState.showSnackbar("Could not delete expense.")
                        }
                    }
                }
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@Composable
fun AddEditExpenseDialog(
    expense: Expense?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, date: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var amountStr by remember { mutableStateOf(expense?.let { MoneyUtils.paisasToInputString(it.amount) } ?: "") }
    var dateMillis by remember { mutableLongStateOf(expense?.date ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Add Shop Expense" else "Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title *") },
                    placeholder = { Text("e.g. Electricity bill, Rent, Tea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )

                ShopDatePickerField(
                    label = "Date",
                    selectedDate = dateMillis,
                    onDateSelected = { dateMillis = it }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        errorMsg = "Please enter an expense title."
                        return@Button
                    }
                    val amountPaisas = MoneyUtils.parseRupeesToPaisas(amountStr)
                    if (amountPaisas == null || amountPaisas <= 0) {
                        errorMsg = "Please enter a valid amount greater than 0."
                        return@Button
                    }
                    onSave(trimmed, amountPaisas, dateMillis, note)
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
