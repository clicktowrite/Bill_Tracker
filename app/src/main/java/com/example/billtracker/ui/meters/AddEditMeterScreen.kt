package com.example.billtracker.ui.meters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.WaterBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMeterScreen(
    uiState: AddEditMeterUiState,
    onNameChange: (String) -> Unit,
    onUtilityTypeChange: (UtilityType) -> Unit,
    onStartingReadingChange: (String) -> Unit,
    onTenantSelect: (Long?) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onFixedRateChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var tenantDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Meter" else "Add Meter",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Utility Type Selection
            Text(
                text = "Utility Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.utilityType == UtilityType.ELECTRICITY,
                    onClick = { onUtilityTypeChange(UtilityType.ELECTRICITY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = ElectricityYellow
                        )
                    }
                ) {
                    Text("Electricity (kWh)")
                }

                SegmentedButton(
                    selected = uiState.utilityType == UtilityType.WATER,
                    onClick = { onUtilityTypeChange(UtilityType.WATER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = WaterBlue
                        )
                    }
                ) {
                    Text("Water (m³)")
                }
            }

            // Meter Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("Meter Name") },
                placeholder = {
                    Text(if (uiState.utilityType == UtilityType.ELECTRICITY) "e.g. Room 1 Electricity" else "e.g. Room 1 Water")
                },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Starting Reading
            OutlinedTextField(
                value = uiState.startingReading,
                onValueChange = onStartingReadingChange,
                label = { Text("Starting Reading (${uiState.utilityType.unit})") },
                placeholder = { Text("0.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.readingError != null,
                supportingText = uiState.readingError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tenant Selection Dropdown
            Text(
                text = "Assign Renter / Tenant (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = tenantDropdownExpanded,
                onExpandedChange = { tenantDropdownExpanded = !tenantDropdownExpanded }
            ) {
                val selectedTenantName = uiState.availableTenants
                    .find { it.id == uiState.selectedTenantId }
                    ?.let { "${it.name} (${it.roomOrUnit})" } ?: "None / Personal Meter"

                OutlinedTextField(
                    value = selectedTenantName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenantDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = tenantDropdownExpanded,
                    onDismissRequest = { tenantDropdownExpanded = false }
                ) {
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

            // Rate Calculation Method Selection
            Text(
                text = "Rate Calculation Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = uiState.rateType == RateType.FIXED,
                        onClick = { onRateTypeChange(RateType.FIXED) }
                    )
                    Text(text = "Fixed Rate (Per ${uiState.utilityType.unit})")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = uiState.rateType == RateType.DYNAMIC,
                        onClick = { onRateTypeChange(RateType.DYNAMIC) }
                    )
                    Text(text = "Dynamic Monthly Rate (Calculated from total provider bill)")
                }
            }

            // Fixed Rate Input (if Fixed chosen)
            if (uiState.rateType == RateType.FIXED) {
                OutlinedTextField(
                    value = uiState.fixedRate,
                    onValueChange = onFixedRateChange,
                    label = { Text("Default Rate per ${uiState.utilityType.unit} (${uiState.currencySymbol})") },
                    placeholder = { Text("12.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.rateError != null,
                    supportingText = uiState.rateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = if (uiState.isEditMode) "Update Meter" else "Save Meter")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
