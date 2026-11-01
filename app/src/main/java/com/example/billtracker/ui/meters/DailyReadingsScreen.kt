package com.example.billtracker.ui.meters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.billtracker.domain.calculator.ForecastResult
import com.example.billtracker.domain.calculator.InterpolatedDailyReading
import com.example.billtracker.domain.calculator.ReadingStatus
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
fun DailyReadingsScreen(
    uiState: DailyReadingsUiState,
    onOpenAddReadingForm: () -> Unit,
    onOpenEditReadingForm: (Long) -> Unit,
    onCloseAddReadingForm: () -> Unit,
    onToggleForecastExpanded: () -> Unit,
    onValueChange: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onHasTimeToggle: (Boolean) -> Unit,
    onMeterResetToggle: (Boolean) -> Unit,
    onIsLastReadingToggle: (Boolean) -> Unit,
    onDestinationTenantSelect: (Long?) -> Unit,
    onConfirmClosePeriod: () -> Unit,
    onDismissClosePeriodDialog: () -> Unit,
    onNotesChange: (String) -> Unit,
    onLogClick: () -> Unit,
    onToggleForecast: (Boolean) -> Unit,
    onDeleteReadingClick: (Long) -> Unit,
    onGenerateBillClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var readingToDelete by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val meter = uiState.meter
    val isClosed = meter?.isClosed == true
    val utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY
    val isElec = utilityType == UtilityType.ELECTRICITY

    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LaunchedEffect(uiState.periodClosedSuccess) {
        if (uiState.periodClosedSuccess) {
            onBackClick()
        }
    }

    // Customer Feedback: Redirect directly to Calculate Bill when "Mark as Last Reading" is logged!
    LaunchedEffect(uiState.navigateToCalculateBillMeterId) {
        if (uiState.navigateToCalculateBillMeterId != null) {
            onGenerateBillClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meter?.name ?: "Daily Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // AREA A: Stationary Top Controls & Meter Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Meter Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isElec) Icons.Default.ElectricBolt else Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = if (isElec) ElectricityYellow else WaterBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meter?.name ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Last Saved Reading: ${ReadingFormatter.formatReading(meter?.currentReading, utilityType)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Monthly Forecast Collapsible Card (Active cycles only)
                if (!isClosed) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Monthly Forecast",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Switch(
                                    checked = uiState.isForecastExpanded,
                                    onCheckedChange = { onToggleForecastExpanded() }
                                )
                            }

                            if (uiState.isForecastExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                when (val forecast = uiState.forecastResult) {
                                    is ForecastResult.Success -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Projected Usage", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = ReadingFormatter.formatReading(forecast.projectedConsumption.toString(), utilityType),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (forecast.projectedBill != null) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Estimated Bill", style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        text = CurrencyFormatter.format(forecast.projectedBill, uiState.currencySymbol),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Observed: ${forecast.observedDays} days (${forecast.verifiedUsage} ${utilityType.unit}) • Avg: ${forecast.averageDailyConsumption} ${utilityType.unit}/day",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is ForecastResult.InsufficientData -> {
                                        Text(
                                            text = forecast.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cohesive Container for Daily Reading History & Compact Estimation Control
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Container Header: Title + Eye-Icon Estimation Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isClosed) "Completed Cycle Reading History" else "Daily Reading History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (!isClosed) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Estimation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = { onToggleForecast(!uiState.isForecastEnabled) }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isForecastEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (uiState.isForecastEnabled) "Estimation Enabled" else "Estimation Disabled",
                                            tint = if (uiState.isForecastEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Container Content: Daily Reading Entries
                        if (uiState.interpolatedReadings.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.Speed,
                                title = "No readings yet",
                                message = "No readings yet. Add your first meter reading to get started.",
                                actionLabel = "Add Reading",
                                onActionClick = onOpenAddReadingForm,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.interpolatedReadings, key = { "${it.status.name}_${it.dateMillis}" }) { item ->
                                    val dateText = if (item.hasTime) {
                                        dateTimeFormatter.format(Date(item.dateMillis))
                                    } else {
                                        dateFormatter.format(Date(item.dateMillis))
                                    }

                                    val valueText = if (item.readingValue != null) {
                                        ReadingFormatter.formatReading(item.readingValue.toString(), utilityType)
                                    } else {
                                        "--"
                                    }

                                    val deltaText = if (item.dailyConsumption != null) {
                                        "+${ReadingFormatter.formatReading(item.dailyConsumption.toString(), utilityType)}"
                                    } else {
                                        "N/A"
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        ListItem(
                                            headlineContent = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = valueText,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    ReadingStatusBadge(status = item.status)
                                                }
                                            },
                                            supportingContent = {
                                                Column {
                                                    Text("Consumption: $deltaText")
                                                    Text(
                                                        text = dateText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (!item.notes.isNullOrBlank()) {
                                                        Text(
                                                            text = "Note: ${item.notes}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            trailingContent = {
                                                if (item.status == ReadingStatus.ACTUAL && item.actualReadingId != null && !isClosed) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(onClick = { onOpenEditReadingForm(item.actualReadingId) }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Reading",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }

                                                        IconButton(onClick = { readingToDelete = item.actualReadingId }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Reading",
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(12.dp)) }
                            }
                        }
                    }
                }
            }

            // AREA C: Stationary Bottom Action Area
            // Customer Feedback: For completed historical cycles (isClosed == true), REMOVE "Calculate Bill" completely!
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                if (isClosed) {
                    Button(
                        onClick = onOpenAddReadingForm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Missed Reading to Historical Cycle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenAddReadingForm,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Reading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onGenerateBillClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calculate Bill",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Popup Form for Adding / Editing a Daily Reading
    if (uiState.isBottomSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { if (!uiState.isSaving) onCloseAddReadingForm() },
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
                        text = if (uiState.editingReadingId != null) "Edit Meter Reading" else if (isClosed) "Add Missed Reading" else "Add Meter Reading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onCloseAddReadingForm,
                        enabled = !uiState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Form")
                    }
                }

                // Priority 2: Permitted Billing Cycle Date Range Banner
                if (uiState.cycleDateRangeText.isNotBlank()) {
                    Text(
                        text = uiState.cycleDateRangeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Animated Temporary Success Message
                AnimatedVisibility(
                    visible = uiState.successMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.successMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Priority 1: Reading value input with red border and inline red warning
                OutlinedTextField(
                    value = uiState.newReadingValue,
                    onValueChange = onValueChange,
                    label = { Text("Reading Value (${utilityType.unit})") },
                    placeholder = { Text("e.g. 1260.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.error != null || uiState.readingWarning != null,
                    supportingText = {
                        if (uiState.readingWarning != null) {
                            Text(
                                text = uiState.readingWarning,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (uiState.error != null) {
                            Text(
                                text = uiState.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date & Optional Time Selection Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateFormatter.format(Date(uiState.selectedDateMillis)))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Include Time", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = uiState.hasTime,
                            onCheckedChange = onHasTimeToggle,
                            enabled = !uiState.isSaving
                        )
                    }
                }

                if (uiState.hasTime) {
                    OutlinedButton(
                        onClick = { showTimePickerDialog = true },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Time: ${timeFormatter.format(Date(uiState.selectedDateMillis))}")
                    }
                }

                // Checkboxes: Reset & Mark as Last Reading (Active cycles only)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = uiState.isMeterReset,
                        onCheckedChange = onMeterResetToggle,
                        enabled = !uiState.isSaving
                    )
                    Text(
                        text = "Meter Reset / Replaced",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (uiState.editingReadingId == null && !isClosed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.isLastReading,
                            onCheckedChange = onIsLastReadingToggle,
                            enabled = !uiState.isSaving
                        )
                        Text(
                            text = "Mark as Last Reading",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (Optional)") },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLogClick,
                        enabled = !uiState.isSaving && uiState.readingWarning == null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isLastReading) Icons.Default.Calculate else Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (uiState.editingReadingId != null) "Update Reading"
                                else if (uiState.isLastReading) "Calculate Bill"
                                else "Save Reading"
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onCloseAddReadingForm,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Material3 Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material3 Time Picker Dialog
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.selectedHour,
            initialMinute = uiState.selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Select Reading Time", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    readingToDelete?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete Daily Reading?",
            message = "Are you sure you want to remove this logged reading?",
            onConfirm = {
                onDeleteReadingClick(id)
                readingToDelete = null
            },
            onDismiss = { readingToDelete = null }
        )
    }
}

@Composable
fun ReadingStatusBadge(status: ReadingStatus) {
    val (icon, color, text) = when (status) {
        ReadingStatus.ACTUAL -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF2E7D32),
            ReadingStatus.ACTUAL.displayName
        )
        ReadingStatus.ESTIMATED -> Triple(
            Icons.Default.Tune,
            Color(0xFF0288D1),
            ReadingStatus.ESTIMATED.displayName
        )
        ReadingStatus.FORECAST -> Triple(
            Icons.Default.Analytics,
            Color(0xFF7B1FA2),
            ReadingStatus.FORECAST.displayName
        )
        ReadingStatus.MISSING -> Triple(
            Icons.Default.Warning,
            Color(0xFFD32F2F),
            ReadingStatus.MISSING.displayName
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.height(14.dp).width(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
