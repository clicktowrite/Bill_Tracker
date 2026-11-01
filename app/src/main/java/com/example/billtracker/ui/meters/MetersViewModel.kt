package com.example.billtracker.ui.meters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MeterFilter {
    ALL,
    ELECTRICITY,
    WATER
}

data class MeterWithTenant(
    val meter: Meter,
    val tenantName: String?
)

data class MetersUiState(
    val filter: MeterFilter = MeterFilter.ALL,
    val meters: List<MeterWithTenant> = emptyList(),
    val availableTenants: List<Tenant> = emptyList(),
    val currencySymbol: String = "₱",
    val isLoading: Boolean = true,
    // Add Meter Popup Sheet State
    val isAddMeterSheetOpen: Boolean = false,
    val isSavingMeter: Boolean = false,
    val newMeterName: String = "",
    val newUtilityType: UtilityType = UtilityType.ELECTRICITY,
    val startingReading: String = "0.0",
    val selectedTenantId: Long? = null,
    val rateType: RateType = RateType.FIXED,
    val fixedRate: String = "0.00",
    val nameError: String? = null,
    val readingError: String? = null,
    val rateError: String? = null
)

class MetersViewModel(
    private val repository: BillTrackerRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(MeterFilter.ALL)
    private val _formState = MutableStateFlow(MetersUiState())

    val uiState: StateFlow<MetersUiState> = combine(
        _formState,
        selectedFilter,
        repository.allMetersFlow,
        repository.allTenantsFlow,
        repository.currencySymbolFlow
    ) { form, filter, meters, tenants, currencySymbol ->

        val tenantsMap = tenants.associateBy { it.id }

        val filteredMeters = meters.filter { meter ->
            when (filter) {
                MeterFilter.ALL -> true
                MeterFilter.ELECTRICITY -> meter.utilityType == UtilityType.ELECTRICITY
                MeterFilter.WATER -> meter.utilityType == UtilityType.WATER
            }
        }.map { meter ->
            MeterWithTenant(
                meter = meter,
                tenantName = meter.tenantId?.let { tenantsMap[it]?.name }
            )
        }

        form.copy(
            filter = filter,
            meters = filteredMeters,
            availableTenants = tenants,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetersUiState()
    )

    fun setFilter(filter: MeterFilter) {
        selectedFilter.value = filter
    }

    fun onOpenAddMeterSheet() {
        _formState.value = _formState.value.copy(
            isAddMeterSheetOpen = true,
            newMeterName = "",
            startingReading = "0.0",
            fixedRate = "0.00",
            nameError = null,
            readingError = null,
            rateError = null
        )
    }

    fun onCloseAddMeterSheet() {
        _formState.value = _formState.value.copy(
            isAddMeterSheetOpen = false,
            newMeterName = "",
            startingReading = "0.0",
            fixedRate = "0.00",
            nameError = null,
            readingError = null,
            rateError = null
        )
    }

    fun onNewMeterNameChange(value: String) {
        _formState.value = _formState.value.copy(newMeterName = value, nameError = null)
    }

    fun onUtilityTypeChange(type: UtilityType) {
        _formState.value = _formState.value.copy(newUtilityType = type)
    }

    fun onStartingReadingChange(value: String) {
        _formState.value = _formState.value.copy(startingReading = value, readingError = null)
    }

    fun onTenantSelect(tenantId: Long?) {
        _formState.value = _formState.value.copy(selectedTenantId = tenantId)
    }

    fun onRateTypeChange(rateType: RateType) {
        _formState.value = _formState.value.copy(rateType = rateType)
    }

    fun onFixedRateChange(value: String) {
        _formState.value = _formState.value.copy(fixedRate = value, rateError = null)
    }

    fun saveMeterFromSheet() {
        val current = _formState.value
        if (current.isSavingMeter) return

        var isValid = true
        var nameErr: String? = null
        var readingErr: String? = null
        var rateErr: String? = null

        if (current.newMeterName.isBlank()) {
            nameErr = "Meter name is required."
            isValid = false
        }

        if (BillingCalculator.parseBigDecimal(current.startingReading) == null) {
            readingErr = "Starting reading must be a valid non-negative number."
            isValid = false
        }

        if (current.rateType == RateType.FIXED) {
            if (BillingCalculator.parseBigDecimal(current.fixedRate) == null) {
                rateErr = "Fixed rate must be a valid non-negative number."
                isValid = false
            }
        }

        if (!isValid) {
            _formState.value = current.copy(
                nameError = nameErr,
                readingError = readingErr,
                rateError = rateErr
            )
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSavingMeter = true)

            try {
                val meter = Meter(
                    name = current.newMeterName.trim(),
                    utilityType = current.newUtilityType,
                    tenantId = current.selectedTenantId,
                    currentReading = current.startingReading.trim(),
                    rateType = current.rateType,
                    fixedRate = current.fixedRate.trim()
                )

                // Atomically save meter and create initial baseline reading
                repository.saveMeterWithInitialReading(meter)

                _formState.value = _formState.value.copy(
                    isSavingMeter = false,
                    isAddMeterSheetOpen = false,
                    newMeterName = "",
                    startingReading = "0.0",
                    nameError = null,
                    readingError = null,
                    rateError = null
                )
            } catch (_: Exception) {
                _formState.value = _formState.value.copy(isSavingMeter = false)
            }
        }
    }

    fun deleteMeter(meterId: Long) {
        viewModelScope.launch {
            repository.deleteMeter(meterId)
        }
    }
}
