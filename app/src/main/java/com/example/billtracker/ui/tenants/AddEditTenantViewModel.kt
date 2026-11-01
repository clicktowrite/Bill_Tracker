package com.example.billtracker.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.model.Tenant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditTenantUiState(
    val tenantId: Long = 0,
    val isEditMode: Boolean = false,
    val name: String = "",
    val roomOrUnit: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val roomError: String? = null,
    val isSaved: Boolean = false
)

class AddEditTenantViewModel(
    private val repository: BillTrackerRepository,
    private val tenantIdToEdit: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTenantUiState())
    val uiState: StateFlow<AddEditTenantUiState> = _uiState.asStateFlow()

    init {
        if (tenantIdToEdit != null && tenantIdToEdit > 0) {
            viewModelScope.launch {
                val existing = repository.getTenantById(tenantIdToEdit)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        tenantId = existing.id,
                        isEditMode = true,
                        name = existing.name,
                        roomOrUnit = existing.roomOrUnit,
                        notes = existing.notes ?: ""
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun onRoomOrUnitChange(value: String) {
        _uiState.value = _uiState.value.copy(roomOrUnit = value, roomError = null)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun saveTenant() {
        val current = _uiState.value

        var isValid = true
        var nameErr: String? = null
        var roomErr: String? = null

        if (current.name.isBlank()) {
            nameErr = "Tenant name is required."
            isValid = false
        }

        if (current.roomOrUnit.isBlank()) {
            roomErr = "Room or Unit number is required."
            isValid = false
        }

        if (!isValid) {
            _uiState.value = current.copy(nameError = nameErr, roomError = roomErr)
            return
        }

        viewModelScope.launch {
            val tenant = Tenant(
                id = current.tenantId,
                name = current.name.trim(),
                roomOrUnit = current.roomOrUnit.trim(),
                notes = current.notes.trim().takeIf { it.isNotEmpty() }
            )

            if (current.isEditMode) {
                repository.updateTenant(tenant)
            } else {
                repository.saveTenant(tenant)
            }

            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
