package com.example.billtracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.formatter.ReadingFormatter
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.components.ConfirmDeleteDialog
import com.example.billtracker.ui.components.EmptyState
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.WaterBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBillClick: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit
) {
    var readingToDelete by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Billing History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.historyItems.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No billing history",
                    message = "Calculated utility bills will be stored here.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(uiState.historyItems, key = { it.reading.id }) { item ->
                        val isElec = item.utilityType == UtilityType.ELECTRICITY
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBillClick(item.reading.id) },
                            shape = MaterialTheme.shapes.large
                        ) {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isElec) Icons.Default.ElectricBolt else Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = if (isElec) ElectricityYellow else WaterBlue
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = item.meterName,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        if (!item.tenantName.isNullOrBlank()) {
                                            Text(
                                                text = "Tenant: ${item.tenantName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "Usage: ${ReadingFormatter.formatReading(item.reading.usage, item.utilityType)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = dateFormatter.format(Date(item.reading.billingDate)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyFormatter.format(item.reading.totalAmount, uiState.currencySymbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { readingToDelete = item.reading.id }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Bill",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    readingToDelete?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete Bill Record?",
            message = "Are you sure you want to delete this historical bill record?",
            onConfirm = {
                onDeleteConfirm(id)
                readingToDelete = null
            },
            onDismiss = { readingToDelete = null }
        )
    }
}
