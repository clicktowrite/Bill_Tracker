package com.example.billtracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.pdf.PdfBillGenerator
import com.example.billtracker.ui.components.BillSummaryCard
import com.example.billtracker.ui.components.ConfirmDeleteDialog
import com.example.billtracker.ui.components.PaymentStatusBadge
import com.example.billtracker.ui.components.RecordPaymentDialog
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    uiState: BillDetailUiState,
    onRecordPayment: (Payment) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteConfirm: () -> Unit,
    onBackClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    val reading = uiState.reading
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val paymentSummary = uiState.paymentSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (reading != null) {
                                val uri = PdfBillGenerator.generateAndGetShareUri(
                                    context = context,
                                    reading = reading,
                                    utilityType = uiState.utilityType,
                                    meterName = uiState.meterName,
                                    tenant = uiState.tenant,
                                    paymentSummary = paymentSummary,
                                    currencySymbol = uiState.currencySymbol
                                )
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Utility Bill PDF"))
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Bill",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (reading == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Bill details not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BillSummaryCard(
                    reading = reading,
                    utilityType = uiState.utilityType,
                    meterName = uiState.meterName,
                    tenantName = uiState.tenant?.name,
                    currencySymbol = uiState.currencySymbol
                )

                // Payment Summary & Tracking Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            PaymentStatusBadge(status = paymentSummary.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Paid", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = CurrencyFormatter.format(paymentSummary.totalPaidAmount, uiState.currencySymbol),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining Balance", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = CurrencyFormatter.format(paymentSummary.remainingBalance, uiState.currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (paymentSummary.remainingBalance > BigDecimal.ZERO)
                                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (paymentSummary.remainingBalance > BigDecimal.ZERO) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showRecordPaymentDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.AddCard, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record Payment")
                            }
                        }
                    }
                }

                // Payment History List
                if (uiState.payments.isNotEmpty()) {
                    Text(
                        text = "Payment Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.payments.forEach { payment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = "${payment.paymentMethod.displayName} - ${CurrencyFormatter.format(payment.amount, uiState.currencySymbol)}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    supportingContent = {
                                        Column {
                                            Text("Date: ${dateFormatter.format(Date(payment.paymentDate))}")
                                            if (!payment.referenceNumber.isNullOrBlank()) {
                                                Text("Ref: ${payment.referenceNumber}")
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { onDeletePayment(payment.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Payment",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showRecordPaymentDialog && reading != null) {
        RecordPaymentDialog(
            readingId = reading.id,
            tenantId = reading.tenantId,
            remainingBalance = paymentSummary.remainingBalance,
            currencySymbol = uiState.currencySymbol,
            onConfirmPayment = { payment ->
                onRecordPayment(payment)
                showRecordPaymentDialog = false
            },
            onDismiss = { showRecordPaymentDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Bill?",
            message = "Are you sure you want to delete this bill record?",
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirm()
                onBackClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
