package com.example.billtracker.ui.meters

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.components.AddMeterBottomSheet
import com.example.billtracker.ui.components.ConfirmDeleteDialog
import com.example.billtracker.ui.components.EmptyState
import com.example.billtracker.ui.components.MeterCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetersScreen(
    uiState: MetersUiState,
    onFilterSelect: (MeterFilter) -> Unit,
    onOpenAddMeterSheet: () -> Unit,
    onCloseAddMeterSheet: () -> Unit,
    onNewMeterNameChange: (String) -> Unit,
    onUtilityTypeChange: (UtilityType) -> Unit,
    onStartingReadingChange: (String) -> Unit,
    onTenantSelect: (Long?) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onFixedRateChange: (String) -> Unit,
    onSaveMeterFromSheet: () -> Unit,
    onCalculateClick: (Long) -> Unit,
    onDailyLogClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit
) {
    var meterToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Meters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddMeterSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Meter"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == MeterFilter.ALL,
                    onClick = { onFilterSelect(MeterFilter.ALL) },
                    label = { Text("All Meters") }
                )
                FilterChip(
                    selected = uiState.filter == MeterFilter.ELECTRICITY,
                    onClick = { onFilterSelect(MeterFilter.ELECTRICITY) },
                    label = { Text("Electricity (kWh)") }
                )
                FilterChip(
                    selected = uiState.filter == MeterFilter.WATER,
                    onClick = { onFilterSelect(MeterFilter.WATER) },
                    label = { Text("Water (m³)") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.meters.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Speed,
                    title = "No meters found",
                    message = "Add an electricity or water meter to begin tracking consumption.",
                    actionLabel = "Add Meter",
                    onActionClick = onOpenAddMeterSheet,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.meters, key = { it.meter.id }) { item ->
                        MeterCard(
                            meter = item.meter,
                            tenantName = item.tenantName,
                            currencySymbol = uiState.currencySymbol,
                            onCalculateClick = { onCalculateClick(item.meter.id) },
                            onDailyLogClick = { onDailyLogClick(item.meter.id) },
                            onEditClick = { onEditClick(item.meter.id) },
                            onDeleteClick = { meterToDelete = item.meter.id }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    // Modal Bottom Sheet Popup Form for Adding a Meter (Task 2)
    AddMeterBottomSheet(
        isOpen = uiState.isAddMeterSheetOpen,
        utilityType = uiState.newUtilityType,
        name = uiState.newMeterName,
        startingReading = uiState.startingReading,
        selectedTenantId = uiState.selectedTenantId,
        availableTenants = uiState.availableTenants,
        rateType = uiState.rateType,
        fixedRate = uiState.fixedRate,
        currencySymbol = uiState.currencySymbol,
        nameError = uiState.nameError,
        readingError = uiState.readingError,
        rateError = uiState.rateError,
        isSaving = uiState.isSavingMeter,
        onUtilityTypeChange = onUtilityTypeChange,
        onNameChange = onNewMeterNameChange,
        onStartingReadingChange = onStartingReadingChange,
        onTenantSelect = onTenantSelect,
        onRateTypeChange = onRateTypeChange,
        onFixedRateChange = onFixedRateChange,
        onSaveClick = onSaveMeterFromSheet,
        onDismissRequest = onCloseAddMeterSheet
    )

    meterToDelete?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete Meter?",
            message = "This will also remove all billing history recorded for this meter. Are you sure?",
            onConfirm = {
                onDeleteConfirm(id)
                meterToDelete = null
            },
            onDismiss = { meterToDelete = null }
        )
    }
}
