package com.example.billtracker.ui.tenants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.billtracker.ui.components.AddProfileBottomSheet
import com.example.billtracker.ui.components.ConfirmDeleteDialog
import com.example.billtracker.ui.components.EmptyState
import com.example.billtracker.ui.components.TenantCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantsScreen(
    uiState: TenantsUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenAddProfileSheet: () -> Unit,
    onCloseAddProfileSheet: () -> Unit,
    onNameChange: (String) -> Unit,
    onRoomOrUnitChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveProfileFromSheet: () -> Unit,
    onTenantClick: (Long) -> Unit,
    onEditTenantClick: (Long) -> Unit,
    onDeleteTenantConfirm: (Long) -> Unit
) {
    var tenantToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Task 4: Utilities Tracker displayed prominently at the top
                        Text(
                            text = "Utilities Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Profiles",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddProfileSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile"
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
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search profiles by name, room, or unit...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.tenants.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.People,
                    title = if (uiState.searchQuery.isBlank()) "No profiles yet" else "No matching profiles",
                    message = if (uiState.searchQuery.isBlank())
                        "Create a profile to manage utility bills for multiple units."
                    else "Try a different search term.",
                    actionLabel = if (uiState.searchQuery.isBlank()) "Add Profile" else null,
                    onActionClick = if (uiState.searchQuery.isBlank()) onOpenAddProfileSheet else null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.tenants, key = { it.tenant.id }) { item ->
                        TenantCard(
                            tenant = item.tenant,
                            electricityBillStr = item.electricityBillStr,
                            waterBillStr = item.waterBillStr,
                            totalDueStr = item.totalDueStr,
                            currencySymbol = uiState.currencySymbol,
                            onClick = { onTenantClick(item.tenant.id) },
                            onEditClick = { onEditTenantClick(item.tenant.id) },
                            onDeleteClick = { tenantToDelete = item.tenant.id }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    // Modal Bottom Sheet Popup Form for Adding a Profile
    AddProfileBottomSheet(
        isOpen = uiState.isAddProfileSheetOpen,
        name = uiState.name,
        roomOrUnit = uiState.roomOrUnit,
        notes = uiState.notes,
        nameError = uiState.nameError,
        roomError = uiState.roomError,
        isSaving = uiState.isSavingProfile,
        onNameChange = onNameChange,
        onRoomOrUnitChange = onRoomOrUnitChange,
        onNotesChange = onNotesChange,
        onSaveClick = onSaveProfileFromSheet,
        onDismissRequest = onCloseAddProfileSheet
    )

    tenantToDelete?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete Profile?",
            message = "This will remove the profile from assigned meters and history. Are you sure?",
            onConfirm = {
                onDeleteTenantConfirm(id)
                tenantToDelete = null
            },
            onDismiss = { tenantToDelete = null }
        )
    }
}
