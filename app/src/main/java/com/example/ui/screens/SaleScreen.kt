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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.Sale
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ProductSelector
import com.example.ui.components.ShopDatePickerField
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningOrange
import com.example.util.MoneyUtils
import kotlinx.coroutines.launch

@Composable
fun SaleScreen(
    viewModel: ShopViewModel,
    preselectedProductId: Long? = null,
    onNavigateToProducts: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allActiveProducts.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }

    var saleToEdit by remember { mutableStateOf<Sale?>(null) }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }

    // Preselect product if passed
    LaunchedEffect(preselectedProductId, products) {
        if (preselectedProductId != null && selectedProduct == null) {
            products.find { it.id == preselectedProductId }?.let { prod ->
                selectedProduct = prod
                sellingPriceStr = MoneyUtils.paisasToInputString(prod.sellingPrice)
            }
        }
    }

    val quantityInt by remember {
        derivedStateOf { quantityStr.trim().toIntOrNull() ?: 0 }
    }
    val unitSellingPricePaisas by remember {
        derivedStateOf { MoneyUtils.parseRupeesToPaisas(sellingPriceStr) ?: 0L }
    }
    val applicableCostPricePaisas by remember {
        derivedStateOf { selectedProduct?.unitCostPaisas ?: 0L }
    }

    val calculatedSaleTotal by remember {
        derivedStateOf {
            if (quantityInt > 0 && unitSellingPricePaisas >= 0L) {
                MoneyUtils.calculateTotal(quantityInt, unitSellingPricePaisas)
            } else 0L
        }
    }

    val calculatedCostAmount by remember {
        derivedStateOf {
            val prod = selectedProduct ?: return@derivedStateOf 0L
            if (quantityInt <= 0) return@derivedStateOf 0L
            if (quantityInt == prod.stockQuantity) {
                prod.totalCostPool
            } else {
                kotlin.math.round(quantityInt.toDouble() * prod.unitCostDoublePaisas).toLong()
            }
        }
    }

    val calculatedProfit by remember {
        derivedStateOf {
            MoneyUtils.calculateProfit(calculatedSaleTotal, calculatedCostAmount)
        }
    }

    val isQuantityExceedingStock by remember {
        derivedStateOf {
            val stock = selectedProduct?.stockQuantity ?: 0
            quantityInt > stock
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("sale_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Record Sale Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("sale_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Record Sale",
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
                                    text = "No products in inventory",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Please add products before recording sales.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToProducts,
                                    modifier = Modifier.testTag("sale_go_to_products_button")
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
                                sellingPriceStr = MoneyUtils.paisasToInputString(prod.sellingPrice)
                                formErrorMessage = null
                            }
                        )

                        // Available Stock Indicator Card
                        if (selectedProduct != null) {
                            val prod = selectedProduct!!
                            val stock = prod.stockQuantity
                            Surface(
                                color = if (stock <= 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else if (prod.isLowStock) WarningOrange.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Available: ${prod.fullStockDisplay}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (stock <= 0) LossRed else if (prod.isLowStock) WarningOrange else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Cost: ${MoneyUtils.formatDoubleRupees(prod.unitCostDoublePaisas)} / ${prod.saleUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (prod.unitsPerPurchaseUnit > 1) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Purchased in ${prod.purchaseUnit}s (1 ${prod.purchaseUnit} = ${prod.unitsPerPurchaseUnit} ${prod.saleUnit}s), sold individually",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity & Selling Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sUnit = selectedProduct?.saleUnit ?: "Unit"
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = {
                                    quantityStr = it
                                    formErrorMessage = null
                                },
                                label = { Text("Quantity Sold (${sUnit}s) *") },
                                placeholder = { Text("e.g. 5") },
                                isError = isQuantityExceedingStock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("sale_quantity_input")
                            )

                            OutlinedTextField(
                                value = sellingPriceStr,
                                onValueChange = {
                                    sellingPriceStr = it
                                    formErrorMessage = null
                                },
                                label = { Text("Sell / $sUnit (Rs.) *") },
                                placeholder = { Text("10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("sale_price_input")
                            )
                        }

                        // Warning if quantity exceeds available stock
                        if (isQuantityExceedingStock) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed, modifier = Modifier.padding(end = 4.dp))
                                Text(
                                    text = "Sale quantity cannot be greater than available stock (${selectedProduct?.stockQuantity ?: 0} ${selectedProduct?.saleUnit ?: "units"}).",
                                    color = LossRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Live Calculation Breakdown Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sale Amount", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = MoneyUtils.formatRupees(calculatedSaleTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cost of Sold Items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = MoneyUtils.formatRupees(calculatedCostAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gross Profit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    val marginText = if (calculatedSaleTotal > 0L) {
                                        val margin = calculatedProfit.toDouble() / calculatedSaleTotal.toDouble() * 100.0
                                        " (+%.1f%%)".format(margin)
                                    } else ""
                                    Text(
                                        text = "${MoneyUtils.formatRupees(calculatedProfit)}$marginText",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calculatedProfit >= 0) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }

                        // Date Picker
                        ShopDatePickerField(
                            label = "Sale Date",
                            selectedDate = dateMillis,
                            onDateSelected = { dateMillis = it },
                            modifier = Modifier.testTag("sale_date_picker")
                        )

                        // Note
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (Optional)") },
                            placeholder = { Text("e.g. Walk-in customer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("sale_note_input")
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
                                val prod = selectedProduct!!
                                if (quantityInt > prod.stockQuantity) {
                                    formErrorMessage = "Sale quantity cannot be greater than available stock (${prod.stockQuantity} ${prod.saleUnit}s)."
                                    return@Button
                                }
                                val sPrice = MoneyUtils.parseRupeesToPaisas(sellingPriceStr)
                                if (sPrice == null || sPrice < 0) {
                                    formErrorMessage = "Please enter a valid selling price."
                                    return@Button
                                }

                                viewModel.recordSale(
                                    productId = prod.id,
                                    quantity = quantityInt,
                                    sellingPrice = sPrice,
                                    date = dateMillis,
                                    note = note
                                ) { res ->
                                    if (res.isSuccess) {
                                        val profitStr = MoneyUtils.formatRupees(calculatedProfit)
                                        quantityStr = ""
                                        note = ""
                                        formErrorMessage = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Sale recorded! Sold $quantityInt ${prod.saleUnit}s. Profit: $profitStr")
                                        }
                                    } else {
                                        formErrorMessage = res.exceptionOrNull()?.message ?: "Error recording sale."
                                    }
                                }
                            },
                            enabled = !isQuantityExceedingStock && selectedProduct != null && quantityInt > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            modifier = Modifier.fillMaxWidth().testTag("save_sale_button")
                        ) {
                            Text("Save Sale")
                        }
                    }
                }
            }
        }

        // Recent Sales List
        item {
            Text(
                text = "Recent Sales (${allSales.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (allSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No sales recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val productMap = products.associateBy { it.id }
            items(allSales, key = { it.id }) { sale ->
                val prodName = productMap[sale.productId]?.name ?: "Product #${sale.productId}"
                val transactionItem = TransactionItem(
                    id = sale.id,
                    type = TransactionType.SALE,
                    title = prodName,
                    productId = sale.productId,
                    quantity = sale.quantity,
                    unitPrice = sale.sellingPrice,
                    amount = sale.totalAmount,
                    costAmount = sale.costAmount,
                    profit = sale.profit,
                    date = sale.date,
                    note = sale.note
                )

                TransactionRowItem(
                    item = transactionItem,
                    onEdit = { saleToEdit = sale },
                    onDelete = { saleToDelete = sale }
                )
            }
        }
    }

    // Edit Sale Dialog
    if (saleToEdit != null) {
        val prod = products.find { it.id == saleToEdit!!.productId }
        val currentProductStock = prod?.stockQuantity ?: 0
        val maxAllowedForEdit = currentProductStock + saleToEdit!!.quantity

        EditSaleDialog(
            sale = saleToEdit!!,
            productName = prod?.name ?: "Product",
            maxAllowedStock = maxAllowedForEdit,
            onDismiss = { saleToEdit = null },
            onSave = { newQty, newPrice, newDate, newNote ->
                viewModel.updateSale(
                    saleId = saleToEdit!!.id,
                    quantity = newQty,
                    sellingPrice = newPrice,
                    date = newDate,
                    note = newNote
                ) { res ->
                    if (res.isSuccess) {
                        saleToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Sale updated successfully.") }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Could not update sale.") }
                    }
                }
            }
        )
    }

    // Delete Sale Confirmation Dialog
    if (saleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Sale",
            message = "Are you sure you want to delete this sale? The sold quantity (${saleToDelete!!.quantity}) will be restored to product stock.",
            confirmText = "Delete",
            onConfirm = {
                val s = saleToDelete!!
                viewModel.deleteSale(s.id) { res ->
                    saleToDelete = null
                    scope.launch {
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Sale deleted and stock restored.")
                        } else {
                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Could not delete sale.")
                        }
                    }
                }
            },
            onDismiss = { saleToDelete = null }
        )
    }
}

@Composable
fun EditSaleDialog(
    sale: Sale,
    productName: String,
    maxAllowedStock: Int,
    onDismiss: () -> Unit,
    onSave: (newQuantity: Int, newSellingPrice: Long, date: Long, note: String) -> Unit
) {
    var quantityStr by remember { mutableStateOf(sale.quantity.toString()) }
    var priceStr by remember { mutableStateOf(MoneyUtils.paisasToInputString(sale.sellingPrice)) }
    var dateMillis by remember { mutableLongStateOf(sale.date) }
    var note by remember { mutableStateOf(sale.note) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val qty = quantityStr.trim().toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sale: $productName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Max available stock for this edit: $maxAllowedStock units",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Sold *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_sale_quantity_input")
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Selling Price (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_sale_price_input")
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
                    if (qty <= 0) {
                        errorMsg = "Quantity must be greater than zero."
                        return@Button
                    }
                    if (qty > maxAllowedStock) {
                        errorMsg = "Quantity cannot exceed available stock ($maxAllowedStock)."
                        return@Button
                    }
                    val price = MoneyUtils.parseRupeesToPaisas(priceStr)
                    if (price == null || price < 0) {
                        errorMsg = "Please enter a valid selling price."
                        return@Button
                    }
                    onSave(qty, price, dateMillis, note)
                },
                modifier = Modifier.testTag("save_edit_sale_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
