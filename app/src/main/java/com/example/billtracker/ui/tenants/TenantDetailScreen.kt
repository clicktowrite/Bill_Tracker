package com.example.billtracker.ui.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.billtracker.ui.components.PhotoEvidencePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.calculator.ReadingStatus
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.formatter.ReadingFormatter
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.meters.ReadingStatusBadge
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.WaterBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailScreen(
    uiState: TenantDetailUiState,
    onEditTenantClick: (Long) -> Unit,
    onCalculateMeterBillClick: (Long) -> Unit,
    onDailyLogClick: (Long) -> Unit,
    onBillClick: (Long) -> Unit,
    onTogglePaymentStatus: (Long, Boolean) -> Unit,
    onToggleCycleEstimation: (Long, Boolean) -> Unit,
    onOpenEditReading: (Long, Long) -> Unit,
    onCloseEditReading: () -> Unit,
    onEditValueChange: (String) -> Unit,
    onEditNotesChange: (String) -> Unit,
    onEditPhotoPathChange: (String?) -> Unit,
    onSaveEditedReading: () -> Unit,
    onOpenAddMissingReading: (Long, Long, Long) -> Unit,
    onCloseAddMissingReading: () -> Unit,
    onMissingReadingValueChange: (String) -> Unit,
    onMissingReadingDateChange: (Long) -> Unit,
    onMissingReadingNotesChange: (String) -> Unit,
    onMissingReadingPhotoPathChange: (String?) -> Unit,
    onSaveMissingReading: () -> Unit,
    onBackClick: () -> Unit
) {
    val tenant = uiState.tenant
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val shortDateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    var expandedBillIds by remember { mutableStateOf(setOf<Long>()) }
    var expandedHistoryBillIds by remember { mutableStateOf(setOf<Long>()) }
    var showMissingDatePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Utilities Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tenant?.name ?: "Profile Details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    tenant?.let {
                        IconButton(onClick = { onEditTenantClick(it.id) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (tenant == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profile not found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Tenant / Profile Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Room / Unit: ${tenant.roomOrUnit}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tenant.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (!tenant.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tenant.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Total Due",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = CurrencyFormatter.format(uiState.totalDue, uiState.currencySymbol),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = ElectricityYellow
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Electricity: ${CurrencyFormatter.format(uiState.latestElectricityBill, uiState.currencySymbol)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = WaterBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Water: ${CurrencyFormatter.format(uiState.latestWaterBill, uiState.currencySymbol)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Assigned Active Meters Section
                item {
                    Text(
                        text = "Assigned Meters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.meters.isEmpty()) {
                    item {
                        Text(
                            text = "No active meters assigned to this profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.meters, key = { it.meter.id }) { item ->
                        val isElec = item.meter.utilityType == UtilityType.ELECTRICITY
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (isElec) Icons.Default.ElectricBolt else Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = if (isElec) ElectricityYellow else WaterBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.meter.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Current Reading: ${ReadingFormatter.formatReading(item.meter.currentReading, item.meter.utilityType)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onCalculateMeterBillClick(item.meter.id) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Calculate Bill",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onDailyLogClick(item.meter.id) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Daily Log",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bill History Section (Section 3: Collapsible Complete Cycle Reading History with Eye Icon)
                item {
                    Text(
                        text = "Bill History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState.readingsHistory.isEmpty()) {
                    item {
                        Text(
                            text = "No finalized bills recorded for this profile yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.readingsHistory, key = { it.reading.id }) { historyModel ->
                        val reading = historyModel.reading
                        val isExpanded = expandedBillIds.contains(reading.id)
                        val isHistoryExpanded = expandedHistoryBillIds.contains(reading.id)

                        // Authoritative billing cycle date range (First Reading Date -> Last Reading Date)
                        val cycleRangeText = "${shortDateFormatter.format(Date(historyModel.cycleStartDateMillis))} – ${shortDateFormatter.format(Date(historyModel.cycleEndDateMillis))}"
                        val unit = if (historyModel.utilityType == UtilityType.WATER) "m³" else "kWh"

                        val isEstimationEnabled = uiState.cycleEstimationMap[reading.id] ?: true
                        val filteredLogs = if (isEstimationEnabled) {
                            historyModel.cycleLogs
                        } else {
                            historyModel.cycleLogs.filter { it.reading.status == ReadingStatus.ACTUAL }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedBillIds = if (isExpanded) expandedBillIds - reading.id else expandedBillIds + reading.id
                                },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Collapsed Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cycleRangeText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${historyModel.meterName} • ${CurrencyFormatter.format(reading.totalAmount, uiState.currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Paid / Unpaid Status Badge Toggle
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (reading.isPaid) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.errorContainer
                                            )
                                            .clickable {
                                                onTogglePaymentStatus(reading.id, !reading.isPaid)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (reading.isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                                contentDescription = if (reading.isPaid) "Paid" else "Unpaid",
                                                tint = if (reading.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.height(14.dp).width(14.dp)
                                            )
                                            Text(
                                                text = if (reading.isPaid) "PAID" else "UNPAID",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (reading.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            expandedBillIds = if (isExpanded) {
                                                expandedBillIds - reading.id
                                            } else {
                                                expandedBillIds + reading.id
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse Details" else "Expand Details"
                                        )
                                    }
                                }

                                // Expanded Bill Computation & Collapsible Reading History Section
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Bill Computation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Previous Reading:", style = MaterialTheme.typography.bodyMedium)
                                        Text("${reading.previousReading} $unit", fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("New Reading:", style = MaterialTheme.typography.bodyMedium)
                                        Text("${reading.currentReading} $unit", fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Consumption:", style = MaterialTheme.typography.bodyMedium)
                                        Text("${reading.usage} $unit", fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Rate:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if (reading.rateType == RateType.FIXED)
                                                "${uiState.currencySymbol}${reading.rate} / $unit (Fixed)"
                                            else
                                                "${uiState.currencySymbol}${reading.rate} / $unit (Dynamic)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Calculation: ${reading.usage} $unit × ${uiState.currencySymbol}${reading.rate} = ${CurrencyFormatter.format(reading.totalAmount, uiState.currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Section 4 & 5: Independently expandable "History Daily Log" Card with conditional Eye Icon Estimation Toggle
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedHistoryBillIds = if (isHistoryExpanded) {
                                                            expandedHistoryBillIds - reading.id
                                                        } else {
                                                            expandedHistoryBillIds + reading.id
                                                        }
                                                    },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "History Daily Log",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Show existing eye-icon estimation toggle only after History Daily Log is expanded
                                                    if (isHistoryExpanded) {
                                                        Text(
                                                            text = "Estimation",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                onToggleCycleEstimation(reading.id, !isEstimationEnabled)
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isEstimationEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                                contentDescription = if (isEstimationEnabled) "Estimation Enabled" else "Estimation Disabled",
                                                                tint = if (isEstimationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.height(20.dp).width(20.dp)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            expandedHistoryBillIds = if (isHistoryExpanded) {
                                                                expandedHistoryBillIds - reading.id
                                                            } else {
                                                                expandedHistoryBillIds + reading.id
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            contentDescription = if (isHistoryExpanded) "Collapse History Daily Log" else "Expand History Daily Log"
                                                        )
                                                    }
                                                }
                                            }

                                            if (isHistoryExpanded) {
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Add Missing Reading Button
                                                OutlinedButton(
                                                    onClick = {
                                                        val midpoint = (historyModel.cycleStartDateMillis + historyModel.cycleEndDateMillis) / 2
                                                        onOpenAddMissingReading(reading.id, reading.meterId, midpoint)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add Missing Reading", maxLines = 1, softWrap = false)
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                if (filteredLogs.isEmpty()) {
                                                    Text(
                                                        text = "No readings available for this filter.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                } else {
                                                    filteredLogs.forEach { logItem ->
                                                        val dateStr = if (logItem.reading.hasTime) dateTimeFormatter.format(Date(logItem.reading.dateMillis)) else dateFormatter.format(Date(logItem.reading.dateMillis))
                                                        val valStr = logItem.reading.readingValue?.let { "$it $unit" } ?: "--"
                                                        val consStr = logItem.reading.dailyConsumption?.let { "+$it $unit" } ?: "N/A"

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(valStr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }

                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(consStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                ReadingStatusBadge(status = logItem.reading.status)

                                                                // Section 5: Pencil edit button for ACTUAL readings only!
                                                                if (logItem.reading.status == ReadingStatus.ACTUAL && logItem.reading.actualReadingId != null) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            onOpenEditReading(
                                                                                logItem.reading.actualReadingId,
                                                                                reading.meterId
                                                                            )
                                                                        }
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Edit,
                                                                            contentDescription = "Edit Actual Reading",
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.height(18.dp).width(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = { onBillClick(reading.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "View Full Statement",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Modal Bottom Sheet to Edit Actual Reading from Profile History (Section 5)
    if (uiState.editReadingState.isOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { if (!uiState.editReadingState.isSaving) onCloseEditReading() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Actual Reading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onCloseEditReading,
                        enabled = !uiState.editReadingState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Form")
                    }
                }

                if (uiState.editReadingState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.editReadingState.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.editReadingState.value,
                    onValueChange = onEditValueChange,
                    label = { Text("Reading Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.editReadingState.error != null,
                    enabled = !uiState.editReadingState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.editReadingState.notes,
                    onValueChange = onEditNotesChange,
                    label = { Text("Notes (Optional)") },
                    enabled = !uiState.editReadingState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                PhotoEvidencePicker(
                    photoPath = uiState.editReadingState.photoPath,
                    onPhotoPathChange = onEditPhotoPathChange,
                    enabled = !uiState.editReadingState.isSaving
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveEditedReading,
                        enabled = !uiState.editReadingState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (uiState.editReadingState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCloseEditReading,
                        enabled = !uiState.editReadingState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet to Add Missing Reading from Profile History
    if (uiState.addMissingReadingState.isOpen) {
        val missingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { if (!uiState.addMissingReadingState.isSaving) onCloseAddMissingReading() },
            sheetState = missingSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Missing Reading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onCloseAddMissingReading,
                        enabled = !uiState.addMissingReadingState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Form")
                    }
                }

                if (uiState.addMissingReadingState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.addMissingReadingState.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.addMissingReadingState.value,
                    onValueChange = onMissingReadingValueChange,
                    label = { Text("Reading Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.addMissingReadingState.error != null,
                    enabled = !uiState.addMissingReadingState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showMissingDatePickerDialog = true },
                    enabled = !uiState.addMissingReadingState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reading Date: ${dateFormatter.format(Date(uiState.addMissingReadingState.dateMillis))}")
                }

                OutlinedTextField(
                    value = uiState.addMissingReadingState.notes,
                    onValueChange = onMissingReadingNotesChange,
                    label = { Text("Notes (Optional)") },
                    enabled = !uiState.addMissingReadingState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                PhotoEvidencePicker(
                    photoPath = uiState.addMissingReadingState.photoPath,
                    onPhotoPathChange = onMissingReadingPhotoPathChange,
                    enabled = !uiState.addMissingReadingState.isSaving
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveMissingReading,
                        enabled = !uiState.addMissingReadingState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (uiState.addMissingReadingState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                        } else {
                            Text(
                                text = "Save Reading",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCloseAddMissingReading,
                        enabled = !uiState.addMissingReadingState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showMissingDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.addMissingReadingState.dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showMissingDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onMissingReadingDateChange(it) }
                        showMissingDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
