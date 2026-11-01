package com.example.billtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.WaterBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeterBottomSheet(
    isOpen: Boolean,
    utilityType: UtilityType,
    name: String,
    startingReading: String,
    selectedTenantId: Long?,
    availableTenants: List<Tenant>,
    rateType: RateType,
    fixedRate: String,
    currencySymbol: String = "₱",
    nameError: String?,
    readingError: String?,
    rateError: String?,
    isSaving: Boolean,
    onUtilityTypeChange: (UtilityType) -> Unit,
    onNameChange: (String) -> Unit,
    onStartingReadingChange: (String) -> Unit,
    onTenantSelect: (Long?) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onFixedRateChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (isOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tenantDropdownExpanded by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) onDismissRequest() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Meter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismissRequest,
                        enabled = !isSaving
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Form")
                    }
                }

                // Utility Type Selection
                Text(
                    text = "Utility Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = utilityType == UtilityType.ELECTRICITY,
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
                        selected = utilityType == UtilityType.WATER,
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
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Meter Name") },
                    placeholder = {
                        Text(if (utilityType == UtilityType.ELECTRICITY) "e.g. Room 1 Electricity" else "e.g. Room 1 Water")
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Starting Reading
                OutlinedTextField(
                    value = startingReading,
                    onValueChange = onStartingReadingChange,
                    label = { Text("Starting Reading (${utilityType.unit})") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = readingError != null,
                    supportingText = readingError?.let { { Text(it) } },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Tenant / Profile Selection Dropdown
                Text(
                    text = "Assign Profile / Tenant (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = tenantDropdownExpanded,
                    onExpandedChange = { if (!isSaving) tenantDropdownExpanded = !tenantDropdownExpanded }
                ) {
                    val selectedTenantName = availableTenants
                        .find { it.id == selectedTenantId }
                        ?.let { "${it.name} (${it.roomOrUnit})" } ?: "None / Personal Meter"

                    OutlinedTextField(
                        value = selectedTenantName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenantDropdownExpanded) },
                        enabled = !isSaving,
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
                        availableTenants.forEach { tenant ->
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
                            selected = rateType == RateType.FIXED,
                            onClick = { onRateTypeChange(RateType.FIXED) },
                            enabled = !isSaving
                        )
                        Text(text = "Fixed Rate (Per ${utilityType.unit})")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = rateType == RateType.DYNAMIC,
                            onClick = { onRateTypeChange(RateType.DYNAMIC) },
                            enabled = !isSaving
                        )
                        Text(text = "Dynamic Monthly Rate (Calculated from total provider bill)")
                    }
                }

                if (rateType == RateType.FIXED) {
                    OutlinedTextField(
                        value = fixedRate,
                        onValueChange = onFixedRateChange,
                        label = { Text("Default Rate per ${utilityType.unit} ($currencySymbol)") },
                        placeholder = { Text("12.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = rateError != null,
                        supportingText = rateError?.let { { Text(it) } },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveClick,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Meter")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isSaving,
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
}
