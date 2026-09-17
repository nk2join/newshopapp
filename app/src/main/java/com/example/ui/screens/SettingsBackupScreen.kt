package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ShopViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsBackupScreen(
    viewModel: ShopViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentShopName by viewModel.shopName.collectAsStateWithLifecycle()

    var shopNameInput by remember(currentShopName) { mutableStateOf(currentShopName) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreContent by remember { mutableStateOf<String?>(null) }

    // Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { writer ->
                            writer.write(json)
                            writer.flush()
                        }
                    }
                    snackbarHostState.showSnackbar("Backup saved successfully.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error writing backup: ${e.message}")
                }
            }
        }
    }

    // Import Document Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val stringBuilder = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            var line = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line).append('\n')
                                line = reader.readLine()
                            }
                        }
                    }
                    pendingRestoreContent = stringBuilder.toString()
                    showRestoreConfirmDialog = true
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Could not read backup file: ${e.message}")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shop Profile Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shop_name_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shop Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = shopNameInput,
                        onValueChange = { shopNameInput = it },
                        label = { Text("Shop Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("shop_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.updateShopName(shopNameInput)
                            scope.launch { snackbarHostState.showSnackbar("Shop name updated.") }
                        },
                        modifier = Modifier.testTag("save_shop_name_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Name")
                    }
                }
            }
        }

        // Currency & System Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Currency", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Pakistani Rupee (PKR - Rs.)", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculation Precision", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Integer Paisas (100% Accurate)", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Network Access", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "100% Offline (Zero internet required)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Backup & Restore Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("backup_restore_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your data is stored safely on your device. Export a backup file to keep a secure copy on Google Drive, USB drive, or another phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export Backup Button
                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val defaultFilename = "ShopLedger_Backup_$timeStamp.json"
                            exportLauncher.launch(defaultFilename)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup File (.json)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Share or Copy Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.exportBackupJson()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Shop Ledger Backup")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Backup JSON"))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_backup_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.exportBackupJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Shop Ledger Backup", json))
                                    snackbarHostState.showSnackbar("Backup JSON copied to clipboard.")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("copy_backup_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Restore Backup Button
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore from Backup File")
                    }
                }
            }
        }

        // About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shop Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0 • Personal Shop Accounting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Designed specifically for single-shop owners. Fast data entry, reliable stock updates, and 100% accurate profit calculation without subscriptions or cloud dependencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Restore
    if (showRestoreConfirmDialog && pendingRestoreContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreContent = null
            },
            title = { Text("Restore Shop Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Warning: Replace Current Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Text("Restoring this backup file will replace your current products, stock, purchases, sales, and expenses with the data in the backup file.\n\nAre you sure you want to proceed?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = pendingRestoreContent ?: ""
                        showRestoreConfirmDialog = false
                        pendingRestoreContent = null
                        viewModel.restoreBackupFromJson(content) { res ->
                            scope.launch {
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Backup restored successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Error restoring backup: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Proceed with Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreContent = null
                    },
                    modifier = Modifier.testTag("cancel_restore_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
