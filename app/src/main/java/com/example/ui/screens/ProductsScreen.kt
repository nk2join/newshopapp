package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningOrange
import com.example.util.MoneyUtils
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(
    viewModel: ShopViewModel,
    onProductClick: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val products by viewModel.searchedProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    Box(modifier = modifier.fillMaxSize().testTag("products_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.productSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input"),
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.productSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (products.isEmpty()) {
                if (searchQuery.isBlank()) {
                    EmptyStateView(
                        title = "No products yet",
                        subtitle = "Add products to start tracking your inventory, purchases, and sales.",
                        actionButtonText = "Add Product",
                        onActionClick = {
                            productToEdit = null
                            showAddEditDialog = true
                        },
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EmptyStateView(
                        title = "No matching products",
                        subtitle = "Try searching with a different name or keyword.",
                        icon = Icons.Default.Search,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onEdit = {
                                productToEdit = product
                                showAddEditDialog = true
                            },
                            onDelete = {
                                productToDelete = product
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                productToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = productToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, purchaseUnit, saleUnit, unitsPerPurchaseUnit, purchasePrice, sellingPrice, stock, threshold ->
                if (productToEdit == null) {
                    viewModel.addProduct(
                        name = name,
                        purchaseUnit = purchaseUnit,
                        saleUnit = saleUnit,
                        unitsPerPurchaseUnit = unitsPerPurchaseUnit,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        openingStock = stock,
                        lowStockThreshold = threshold
                    ) { result ->
                        if (result.isSuccess) {
                            showAddEditDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Product added successfully.") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Error adding product.") }
                        }
                    }
                } else {
                    viewModel.updateProduct(
                        id = productToEdit!!.id,
                        name = name,
                        purchaseUnit = purchaseUnit,
                        saleUnit = saleUnit,
                        unitsPerPurchaseUnit = unitsPerPurchaseUnit,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        lowStockThreshold = threshold,
                        manualStockAdjustment = stock
                    ) { result ->
                        if (result.isSuccess) {
                            showAddEditDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Product updated successfully.") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Error updating product.") }
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Product",
            message = "Are you sure you want to delete '${productToDelete!!.name}'? If this product has existing sales or purchase history, it will be safely archived without deleting previous records.",
            confirmText = "Delete",
            onConfirm = {
                val prod = productToDelete!!
                viewModel.deleteProduct(prod.id) { res ->
                    productToDelete = null
                    scope.launch {
                        if (res.isSuccess) {
                            val wasSoftDeleted = res.getOrDefault(false)
                            if (wasSoftDeleted) {
                                snackbarHostState.showSnackbar("'${prod.name}' archived to preserve transaction history.")
                            } else {
                                snackbarHostState.showSnackbar("'${prod.name}' deleted.")
                            }
                        } else {
                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Could not delete product.")
                        }
                    }
                }
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.isLowStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = WarningOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Low Stock (${product.stockQuantity} ${product.saleUnit})",
                                    color = WarningOrange,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_product_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_product_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Unit conversion tag
            if (product.unitsPerPurchaseUnit > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "1 ${product.purchaseUnit} = ${product.unitsPerPurchaseUnit} ${product.saleUnit}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row: Stock, Purchase Price, Selling Price, Total Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.stockBreakdown,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.isLowStock) WarningOrange else MaterialTheme.colorScheme.onSurface
                    )
                    if (product.unitsPerPurchaseUnit > 1) {
                        Text(
                            text = "(${product.stockQuantity} ${product.saleUnit})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        text = "Cost / ${product.purchaseUnit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyUtils.formatRupees(product.purchasePrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (product.unitsPerPurchaseUnit > 1) {
                        Text(
                            text = "(${MoneyUtils.formatDoubleRupees(product.unitCostDoublePaisas)}/${product.saleUnit})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        text = "Sell / ${product.saleUnit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyUtils.formatRupees(product.sellingPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Stock Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyUtils.formatRupees(product.stockValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        purchaseUnit: String,
        saleUnit: String,
        unitsPerPurchaseUnit: Int,
        purchasePrice: Long,
        sellingPrice: Long,
        stock: Int,
        threshold: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var purchaseUnit by remember { mutableStateOf(product?.purchaseUnit ?: "Box") }
    var saleUnit by remember { mutableStateOf(product?.saleUnit ?: "Packet") }
    var unitsPerPurchaseUnitStr by remember {
        mutableStateOf(product?.unitsPerPurchaseUnit?.toString() ?: "12")
    }
    var purchasePriceStr by remember {
        mutableStateOf(product?.let { MoneyUtils.paisasToInputString(it.purchasePrice) } ?: "")
    }
    var sellingPriceStr by remember {
        mutableStateOf(product?.let { MoneyUtils.paisasToInputString(it.sellingPrice) } ?: "")
    }
    var stockStr by remember {
        mutableStateOf(product?.stockQuantity?.toString() ?: "")
    }
    var thresholdStr by remember {
        mutableStateOf(product?.lowStockThreshold?.toString() ?: "5")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live calculations for preview
    val unitsPerBoxInt by remember {
        derivedStateOf { unitsPerPurchaseUnitStr.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1 }
    }
    val costPricePaisas by remember {
        derivedStateOf { MoneyUtils.parseRupeesToPaisas(purchasePriceStr) ?: 0L }
    }
    val sellPricePaisas by remember {
        derivedStateOf { MoneyUtils.parseRupeesToPaisas(sellingPriceStr) ?: 0L }
    }
    val calculatedUnitCostDouble by remember {
        derivedStateOf {
            if (costPricePaisas > 0L && unitsPerBoxInt > 0) {
                costPricePaisas.toDouble() / unitsPerBoxInt.toDouble()
            } else 0.0
        }
    }
    val calculatedProfitPerUnitDouble by remember {
        derivedStateOf {
            if (sellPricePaisas > 0L) {
                sellPricePaisas.toDouble() - calculatedUnitCostDouble
            } else 0.0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Super Biscuit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                // Units Configuration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchaseUnit,
                        onValueChange = { purchaseUnit = it },
                        label = { Text("Purchase Unit *") },
                        placeholder = { Text("Box / Carton") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_purchase_unit_input")
                    )

                    OutlinedTextField(
                        value = saleUnit,
                        onValueChange = { saleUnit = it },
                        label = { Text("Sale Unit *") },
                        placeholder = { Text("Packet / Piece") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sale_unit_input")
                    )
                }

                // Units per Purchase Unit (Packets per Box)
                OutlinedTextField(
                    value = unitsPerPurchaseUnitStr,
                    onValueChange = { unitsPerPurchaseUnitStr = it },
                    label = { Text("${saleUnit.ifBlank { "Packet" }}s per ${purchaseUnit.ifBlank { "Box" }} *") },
                    placeholder = { Text("e.g. 12") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_units_per_box_input")
                )

                // Cost and Selling Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("Cost per ${purchaseUnit.ifBlank { "Box" }} (Rs.) *") },
                        placeholder = { Text("100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_purchase_price_input")
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Sell per ${saleUnit.ifBlank { "Packet" }} (Rs.) *") },
                        placeholder = { Text("10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_selling_price_input")
                    )
                }

                // Live Cost & Profit preview
                if (costPricePaisas > 0L || sellPricePaisas > 0L) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unit Cost:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${MoneyUtils.formatDoubleRupees(calculatedUnitCostDouble)} / ${saleUnit.ifBlank { "Packet" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (sellPricePaisas > 0L && costPricePaisas > 0L) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Gross Margin / Unit:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val marginPercent = if (calculatedUnitCostDouble > 0) {
                                        " (+%.1f%%)".format(calculatedProfitPerUnitDouble / calculatedUnitCostDouble * 100.0)
                                    } else ""
                                    Text(
                                        text = "${MoneyUtils.formatDoubleRupees(calculatedProfitPerUnitDouble)}$marginPercent",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calculatedProfitPerUnitDouble >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Stock & Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text(if (product == null) "Opening Stock (${saleUnit.ifBlank { "Packet" }}s)" else "Stock (${saleUnit.ifBlank { "Packet" }}s)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_stock_input")
                    )

                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Low Alert (${saleUnit.ifBlank { "Packet" }}s)") },
                        placeholder = { Text("5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_threshold_input")
                    )
                }

                // Stock breakdown hint
                val enteredStock = stockStr.trim().toIntOrNull() ?: 0
                if (enteredStock > 0 && unitsPerBoxInt > 1) {
                    val boxes = enteredStock / unitsPerBoxInt
                    val rem = enteredStock % unitsPerBoxInt
                    val desc = if (rem == 0) "$boxes $purchaseUnit" else "$boxes $purchaseUnit + $rem $saleUnit"
                    Text(
                        text = "$enteredStock $saleUnit = $desc",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Please enter a product name."
                        return@Button
                    }
                    val cleanPUnit = purchaseUnit.trim().ifEmpty { "Box" }
                    val cleanSUnit = saleUnit.trim().ifEmpty { "Packet" }
                    val unitsPerBox = unitsPerPurchaseUnitStr.trim().toIntOrNull() ?: 1
                    if (unitsPerBox <= 0) {
                        errorMessage = "Units per purchase unit must be at least 1."
                        return@Button
                    }
                    val pPrice = MoneyUtils.parseRupeesToPaisas(purchasePriceStr)
                    if (pPrice == null || pPrice < 0) {
                        errorMessage = "Please enter a valid cost price per $cleanPUnit."
                        return@Button
                    }
                    val sPrice = MoneyUtils.parseRupeesToPaisas(sellingPriceStr)
                    if (sPrice == null || sPrice < 0) {
                        errorMessage = "Please enter a valid selling price per $cleanSUnit."
                        return@Button
                    }
                    val stock = stockStr.trim().toIntOrNull() ?: 0
                    if (stock < 0) {
                        errorMessage = "Stock cannot be negative."
                        return@Button
                    }
                    val threshold = thresholdStr.trim().toIntOrNull() ?: 5
                    if (threshold < 0) {
                        errorMessage = "Low stock alert level cannot be negative."
                        return@Button
                    }

                    onSave(trimmedName, cleanPUnit, cleanSUnit, unitsPerBox, pPrice, sPrice, stock, threshold)
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_product_button")) {
                Text("Cancel")
            }
        }
    )
}
