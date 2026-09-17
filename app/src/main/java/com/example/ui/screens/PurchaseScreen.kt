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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ProductSelector
import com.example.ui.components.ShopDatePickerField
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.NavyTertiary
import com.example.util.MoneyUtils
import kotlinx.coroutines.launch

@Composable
fun PurchaseScreen(
    viewModel: ShopViewModel,
    preselectedProductId: Long? = null,
    onNavigateToProducts: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allActiveProducts.collectAsStateWithLifecycle()
    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityStr by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }

    var purchaseToEdit by remember { mutableStateOf<Purchase?>(null) }
    var purchaseToDelete by remember { mutableStateOf<Purchase?>(null) }

    // Preselect product if passed
    LaunchedEffect(preselectedProductId, products) {
        if (preselectedProductId != null && selectedProduct == null) {
            products.find { it.id == preselectedProductId }?.let { prod ->
                selectedProduct = prod
                purchasePriceStr = MoneyUtils.paisasToInputString(prod.purchasePrice)
            }
        }
    }

    // Calculated total
    val quantityInt by remember {
        derivedStateOf { quantityStr.trim().toIntOrNull() ?: 0 }
    }
    val unitPricePaisas by remember {
        derivedStateOf { MoneyUtils.parseRupeesToPaisas(purchasePriceStr) ?: 0L }
    }
    val calculatedTotalPaisas by remember {
        derivedStateOf {
            if (quantityInt > 0 && unitPricePaisas >= 0L) {
                MoneyUtils.calculateTotal(quantityInt, unitPricePaisas)
            } else 0L
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("purchase_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Entry Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("purchase_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Record Purchase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (products.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "No products found",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Please add at least one product before recording a purchase.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToProducts,
                                    modifier = Modifier.testTag("go_to_products_button")
                                ) {
                                    Text("Add Product")
                                }
                            }
                        }
                    } else {
                        // Product Selector
                        ProductSelector(
                            products = products,
                            selectedProduct = selectedProduct,
                            onProductSelected = { prod ->
                                selectedProduct = prod
                                purchasePriceStr = MoneyUtils.paisasToInputString(prod.purchasePrice)
                                formErrorMessage = null
                            }
                        )

                        if (selectedProduct != null) {
                            val prod = selectedProduct!!
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (prod.unitsPerPurchaseUnit > 1) {
                                        Text(
                                            text = "Unit Packing: 1 ${prod.purchaseUnit} = ${prod.unitsPerPurchaseUnit} ${prod.saleUnit}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = "Current Stock: ${prod.fullStockDisplay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Quantity & Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pUnit = selectedProduct?.purchaseUnit ?: "Box"
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = {
                                    quantityStr = it
                                    formErrorMessage = null
                                },
                                label = { Text("Quantity (${pUnit}es) *") },
                                placeholder = { Text("e.g. 5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("purchase_quantity_input")
                            )

                            OutlinedTextField(
                                value = purchasePriceStr,
                                onValueChange = {
                                    purchasePriceStr = it
                                    formErrorMessage = null
                                },
                                label = { Text("Cost / $pUnit (Rs.) *") },
                                placeholder = { Text("100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("purchase_price_input")
                            )
                        }

                        // Total calculation and unit breakdown banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Purchase Cost",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = MoneyUtils.formatRupees(calculatedTotalPaisas),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (selectedProduct != null && selectedProduct!!.unitsPerPurchaseUnit > 1 && quantityInt > 0) {
                                    val prod = selectedProduct!!
                                    val unitsAdded = quantityInt * prod.unitsPerPurchaseUnit
                                    val enteredPricePaisas = MoneyUtils.parseRupeesToPaisas(purchasePriceStr) ?: prod.purchasePrice
                                    val unitCostDouble = enteredPricePaisas.toDouble() / prod.unitsPerPurchaseUnit.toDouble()

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Stock to add:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "+$unitsAdded ${prod.saleUnit}s ($quantityInt ${prod.purchaseUnit}s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Cost per ${prod.saleUnit}:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${MoneyUtils.formatDoubleRupees(unitCostDouble)} / ${prod.saleUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Date picker
                        ShopDatePickerField(
                            label = "Purchase Date",
                            selectedDate = dateMillis,
                            onDateSelected = { dateMillis = it },
                            modifier = Modifier.testTag("purchase_date_picker")
                        )

                        // Note
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (Optional)") },
                            placeholder = { Text("e.g. Supplier invoice #123") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("purchase_note_input")
                        )

                        if (formErrorMessage != null) {
                            Text(
                                text = formErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedProduct == null) {
                                    formErrorMessage = "Please select a product."
                                    return@Button
                                }
                                if (quantityInt <= 0) {
                                    formErrorMessage = "Quantity must be greater than zero."
                                    return@Button
                                }
                                val pPrice = MoneyUtils.parseRupeesToPaisas(purchasePriceStr)
                                if (pPrice == null || pPrice < 0) {
                                    formErrorMessage = "Please enter a valid purchase price."
                                    return@Button
                                }

                                val prod = selectedProduct!!
                                viewModel.recordPurchase(
                                    productId = prod.id,
                                    quantity = quantityInt,
                                    purchasePrice = pPrice,
                                    date = dateMillis,
                                    note = note
                                ) { res ->
                                    if (res.isSuccess) {
                                        val addedUnits = quantityInt * prod.unitsPerPurchaseUnit
                                        quantityStr = ""
                                        note = ""
                                        formErrorMessage = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Purchase recorded! Added $addedUnits ${prod.saleUnit}s ($quantityInt ${prod.purchaseUnit}s).")
                                        }
                                    } else {
                                        formErrorMessage = res.exceptionOrNull()?.message ?: "Error recording purchase."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_purchase_button")
                        ) {
                            Text("Save Purchase")
                        }
                    }
                }
            }
        }

        // Recent Purchases List
        item {
            Text(
                text = "Recent Purchases (${allPurchases.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (allPurchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No purchases recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val productMap = products.associateBy { it.id }
            items(allPurchases, key = { it.id }) { purchase ->
                val prodName = productMap[purchase.productId]?.name ?: "Product #${purchase.productId}"
                val transactionItem = TransactionItem(
                    id = purchase.id,
                    type = TransactionType.PURCHASE,
                    title = prodName,
                    productId = purchase.productId,
                    quantity = purchase.quantity,
                    unitPrice = purchase.purchasePrice,
                    amount = purchase.totalAmount,
                    date = purchase.date,
                    note = purchase.note
                )

                TransactionRowItem(
                    item = transactionItem,
                    onEdit = { purchaseToEdit = purchase },
                    onDelete = { purchaseToDelete = purchase }
                )
            }
        }
    }

    // Edit Purchase Dialog
    if (purchaseToEdit != null) {
        EditPurchaseDialog(
            purchase = purchaseToEdit!!,
            productName = products.find { it.id == purchaseToEdit!!.productId }?.name ?: "Product",
            onDismiss = { purchaseToEdit = null },
            onSave = { newQty, newPrice, newDate, newNote ->
                viewModel.updatePurchase(
                    purchaseId = purchaseToEdit!!.id,
                    quantity = newQty,
                    purchasePrice = newPrice,
                    date = newDate,
                    note = newNote
                ) { res ->
                    if (res.isSuccess) {
                        purchaseToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Purchase updated successfully.") }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Could not update purchase.") }
                    }
                }
            }
        )
    }

    // Delete Purchase Confirmation Dialog
    if (purchaseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Purchase",
            message = "Are you sure you want to delete this purchase? The purchased quantity (${purchaseToDelete!!.quantity}) will be reversed from product stock.",
            confirmText = "Delete",
            onConfirm = {
                val p = purchaseToDelete!!
                viewModel.deletePurchase(p.id) { res ->
                    purchaseToDelete = null
                    scope.launch {
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Purchase deleted and stock restored.")
                        } else {
                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Could not delete purchase.")
                        }
                    }
                }
            },
            onDismiss = { purchaseToDelete = null }
        )
    }
}

@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    productName: String,
    onDismiss: () -> Unit,
    onSave: (newQuantity: Int, newPurchasePrice: Long, date: Long, note: String) -> Unit
) {
    var quantityStr by remember { mutableStateOf(purchase.quantity.toString()) }
    var priceStr by remember { mutableStateOf(MoneyUtils.paisasToInputString(purchase.purchasePrice)) }
    var dateMillis by remember { mutableLongStateOf(purchase.date) }
    var note by remember { mutableStateOf(purchase.note) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Purchase: $productName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_purchase_quantity_input")
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Cost Price (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_purchase_price_input")
                )

                ShopDatePickerField(
                    label = "Date",
                    selectedDate = dateMillis,
                    onDateSelected = { dateMillis = it }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.trim().toIntOrNull() ?: 0
                    if (qty <= 0) {
                        errorMsg = "Quantity must be greater than zero."
                        return@Button
                    }
                    val price = MoneyUtils.parseRupeesToPaisas(priceStr)
                    if (price == null || price < 0) {
                        errorMsg = "Please enter a valid price."
                        return@Button
                    }
                    onSave(qty, price, dateMillis, note)
                },
                modifier = Modifier.testTag("save_edit_purchase_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
