package com.example.billtracker.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class TenantWithBillSummary(
    val tenant: Tenant,
    val electricityBillStr: String?,
    val waterBillStr: String?,
    val totalDueStr: String
)

data class TenantsUiState(
    val searchQuery: String = "",
    val tenants: List<TenantWithBillSummary> = emptyList(),
    val currencySymbol: String = "₱",
    val isLoading: Boolean = true,
    // Add Profile Sheet State
    val isAddProfileSheetOpen: Boolean = false,
    val isSavingProfile: Boolean = false,
    val name: String = "",
    val roomOrUnit: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val roomError: String? = null
)

class TenantsViewModel(
    private val repository: BillTrackerRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _formState = MutableStateFlow(TenantsUiState())

    private val tenantsDataFlow = combine(
        searchQuery,
        repository.allTenantsFlow,
        repository.allMetersFlow,
        repository.allReadingsFlow,
        repository.currencySymbolFlow
    ) { query, tenants, meters, readings, currencySymbol ->

        val metersMap = meters.associateBy { it.id }

        val filteredTenants = if (query.isBlank()) {
            tenants
        } else {
            tenants.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.roomOrUnit.contains(query, ignoreCase = true)
            }
        }

        val summaries = filteredTenants.map { tenant ->
            val tenantReadings = readings.filter { it.tenantId == tenant.id }

            val latestElecReading = tenantReadings
                .filter { metersMap[it.meterId]?.utilityType == UtilityType.ELECTRICITY }
                .maxByOrNull { it.billingDate }

            val latestWaterReading = tenantReadings
                .filter { metersMap[it.meterId]?.utilityType == UtilityType.WATER }
                .maxByOrNull { it.billingDate }

            val elecAmt = latestElecReading?.let {
                try { BigDecimal(it.totalAmount.replace(",", "")) } catch (_: Exception) { BigDecimal.ZERO }
            } ?: BigDecimal.ZERO

            val waterAmt = latestWaterReading?.let {
                try { BigDecimal(it.totalAmount.replace(",", "")) } catch (_: Exception) { BigDecimal.ZERO }
            } ?: BigDecimal.ZERO

            val total = elecAmt.add(waterAmt).setScale(2, RoundingMode.HALF_UP)

            TenantWithBillSummary(
                tenant = tenant,
                electricityBillStr = latestElecReading?.totalAmount,
                waterBillStr = latestWaterReading?.totalAmount,
                totalDueStr = total.toString()
            )
        }

        Triple(query, summaries, currencySymbol)
    }

    val uiState: StateFlow<TenantsUiState> = combine(
        _formState,
        tenantsDataFlow
    ) { form, (query, summaries, currencySymbol) ->
        form.copy(
            searchQuery = query,
            tenants = summaries,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantsUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onOpenAddProfileSheet() {
        _formState.value = _formState.value.copy(
            isAddProfileSheetOpen = true,
            name = "",
            roomOrUnit = "",
            notes = "",
            nameError = null,
            roomError = null
        )
    }

    fun onCloseAddProfileSheet() {
        _formState.value = _formState.value.copy(
            isAddProfileSheetOpen = false,
            name = "",
            roomOrUnit = "",
            notes = "",
            nameError = null,
            roomError = null
        )
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }

    fun onRoomOrUnitChange(value: String) {
        _formState.value = _formState.value.copy(roomOrUnit = value, roomError = null)
    }

    fun onNotesChange(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun saveProfileFromSheet() {
        val current = _formState.value
        if (current.isSavingProfile) return

        var isValid = true
        var nameErr: String? = null
        var roomErr: String? = null

        if (current.name.isBlank()) {
            nameErr = "Profile name is required."
            isValid = false
        }

        if (current.roomOrUnit.isBlank()) {
            roomErr = "Room or unit number is required."
            isValid = false
        }

        if (!isValid) {
            _formState.value = current.copy(
                nameError = nameErr,
                roomError = roomErr
            )
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSavingProfile = true)

            try {
                val tenant = Tenant(
                    name = current.name.trim(),
                    roomOrUnit = current.roomOrUnit.trim(),
                    notes = current.notes.trim().takeIf { it.isNotBlank() }
                )

                repository.saveTenant(tenant)

                _formState.value = _formState.value.copy(
                    isSavingProfile = false,
                    isAddProfileSheetOpen = false,
                    name = "",
                    roomOrUnit = "",
                    notes = "",
                    nameError = null,
                    roomError = null
                )
            } catch (_: Exception) {
                _formState.value = _formState.value.copy(isSavingProfile = false)
            }
        }
    }

    fun deleteTenant(tenantId: Long) {
        viewModelScope.launch {
            repository.deleteTenant(tenantId)
        }
    }
}
