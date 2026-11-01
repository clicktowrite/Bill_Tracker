package com.example.billtracker.ui.meters

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billtracker.data.local.ImageStorageManager
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.formatter.ReadingFormatter
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.components.AddProfileBottomSheet
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.WaterBlue
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculateBillScreen(
    uiState: CalculateBillUiState,
    onTenantSelect: (Long?) -> Unit,
    onOpenAddProfileSheet: () -> Unit,
    onCloseAddProfileSheet: () -> Unit,
    onNewProfileNameChange: (String) -> Unit,
    onNewProfileRoomChange: (String) -> Unit,
    onNewProfileNotesChange: (String) -> Unit,
    onSaveProfileAndResumeBillSave: () -> Unit,
    onCurrentReadingChange: (String) -> Unit,
    onBillingDateChange: (Long) -> Unit,
    onHasTimeToggle: (Boolean) -> Unit,
    onMeterResetToggle: (Boolean) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onFixedRateChange: (String) -> Unit,
    onProviderTotalBillChange: (String) -> Unit,
    onProviderTotalUsageChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPhotoPathChange: (String?) -> Unit,
    onSaveBillClick: () -> Unit,
    onBillSavedSuccess: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var tenantDropdownExpanded by remember { mutableStateOf(false) }

    val meter = uiState.meter
    val utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY
    val isElectricity = utilityType == UtilityType.ELECTRICITY
    val icon = if (isElectricity) Icons.Default.ElectricBolt else Icons.Default.WaterDrop
    val badgeColor = if (isElectricity) ElectricityYellow else WaterBlue

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Photo Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val path = ImageStorageManager.saveImageFromUri(context, uri)
                onPhotoPathChange(path)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch {
                val path = ImageStorageManager.saveBitmap(context, bitmap)
                onPhotoPathChange(path)
            }
        }
    }

    LaunchedEffect(uiState.savedSuccessTenantId) {
        uiState.savedSuccessTenantId?.let { tenantId ->
            onBillSavedSuccess(tenantId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculate Bill",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meter Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(
                            text = meter?.name ?: "Meter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val profileName = uiState.tenant?.let { "${it.name} (${it.roomOrUnit})" } ?: "Unassigned / Personal Meter"
                        Text(
                            text = "Assigned Profile: $profileName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Assign Profile / Renter Dropdown (Section 2)
            Text(
                text = "Assign Renter / Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = tenantDropdownExpanded,
                onExpandedChange = { if (!uiState.isSavingBill) tenantDropdownExpanded = !tenantDropdownExpanded }
            ) {
                val selectedTenantName = uiState.availableTenants
                    .find { it.id == uiState.selectedTenantId }
                    ?.let { "${it.name} (${it.roomOrUnit})" } ?: "None / Personal Meter"

                OutlinedTextField(
                    value = selectedTenantName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenantDropdownExpanded) },
                    enabled = !uiState.isSavingBill,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = tenantDropdownExpanded,
                    onDismissRequest = { tenantDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("+ Create New Profile") },
                        onClick = {
                            tenantDropdownExpanded = false
                            onOpenAddProfileSheet()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("None / Personal Meter") },
                        onClick = {
                            onTenantSelect(null)
                            tenantDropdownExpanded = false
                        }
                    )
                    uiState.availableTenants.forEach { tenant ->
                        DropdownMenuItem(
                            text = { Text("${tenant.name} - Room/Unit ${tenant.roomOrUnit}") },
                            onClick = {
                                onTenantSelect(tenant.id)
                                tenantDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Meter Readings Inputs
            Text(
                text = "Meter Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Billing Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePickerDialog = true }
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(dateFormatter.format(Date(uiState.billingDateMillis)))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Include Time", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = uiState.hasTime,
                        onCheckedChange = onHasTimeToggle
                    )
                }
            }

            // Auto-filled Previous Reading
            OutlinedTextField(
                value = if (uiState.isMeterReset) "0.0 (Reset)" else uiState.previousReading,
                onValueChange = {},
                readOnly = true,
                label = { Text("Previous Reading (${utilityType.unit})") },
                supportingText = { Text("Automatically loaded from last billing cycle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Current Reading Input
            OutlinedTextField(
                value = uiState.currentReading,
                onValueChange = onCurrentReadingChange,
                label = { Text("New Current Reading (${utilityType.unit})") },
                placeholder = { Text("Enter current reading") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.calculationError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Meter Reset Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.isMeterReset,
                    onCheckedChange = onMeterResetToggle
                )
                Text(
                    text = "Meter Reset / Replaced (Establish new baseline reading)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Meter Reading Photo Evidence Section
            Text(
                text = "Meter Photo Evidence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val photoPath = uiState.photoPath
                    if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                        val bitmap = BitmapFactory.decodeFile(photoPath)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Meter Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        OutlinedButton(
                            onClick = { onPhotoPathChange(null) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Photo")
                        }
                    } else {
                        Text(
                            text = "Attach a photo of the meter reading for record keeping",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera")
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }
            }

            // Rate Calculation Options
            Text(
                text = "Rate Calculation Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.rateType == RateType.FIXED,
                        onClick = { onRateTypeChange(RateType.FIXED) }
                    )
                    Text("Fixed Rate")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.rateType == RateType.DYNAMIC,
                        onClick = { onRateTypeChange(RateType.DYNAMIC) }
                    )
                    Text("Dynamic Rate (From Utility Provider Bill)")
                }
            }

            if (uiState.rateType == RateType.FIXED) {
                OutlinedTextField(
                    value = uiState.fixedRate,
                    onValueChange = onFixedRateChange,
                    label = { Text("Rate per ${utilityType.unit} (${uiState.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Provider Monthly Bill Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.providerTotalBill,
                            onValueChange = onProviderTotalBillChange,
                            label = { Text("Total Provider Bill (${uiState.currencySymbol})") },
                            placeholder = { Text("e.g. 4850") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = uiState.providerTotalUsage,
                            onValueChange = onProviderTotalUsageChange,
                            label = { Text("Total Provider Consumption (${utilityType.unit})") },
                            placeholder = { Text("e.g. 350") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (uiState.dynamicCalculatedRate != null) {
                            Text(
                                text = "Calculated Rate: ${ReadingFormatter.formatRate(uiState.dynamicCalculatedRate, utilityType, uiState.currencySymbol)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Real-time Bill Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculation Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.calculationError != null) {
                        Text(
                            text = uiState.calculationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Usage (${utilityType.unit}):")
                            Text(
                                text = ReadingFormatter.formatReading(uiState.calculatedUsage.toString(), utilityType),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Effective Rate:")
                            Text(
                                text = ReadingFormatter.formatRate(uiState.calculatedRate.toString(), utilityType, uiState.currencySymbol),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Bill:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.format(uiState.calculatedTotalBill, uiState.currencySymbol),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Optional Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g. Month of October") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSaveBillClick,
                enabled = !uiState.isSavingBill,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isSavingBill) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.width(20.dp).height(20.dp)
                    )
                } else {
                    Text(text = "Save Bill & Update Meter")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal Bottom Sheet for Creating Profile when none exists (Section 3)
    AddProfileBottomSheet(
        isOpen = uiState.isAddProfileSheetOpen,
        name = uiState.newProfileName,
        roomOrUnit = uiState.newProfileRoom,
        notes = uiState.newProfileNotes,
        nameError = uiState.newProfileNameError,
        roomError = uiState.newProfileRoomError,
        isSaving = uiState.isSavingProfile,
        onNameChange = onNewProfileNameChange,
        onRoomOrUnitChange = onNewProfileRoomChange,
        onNotesChange = onNewProfileNotesChange,
        onSaveClick = onSaveProfileAndResumeBillSave,
        onDismissRequest = onCloseAddProfileSheet
    )

    // Material3 Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.billingDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onBillingDateChange(it) }
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
}
