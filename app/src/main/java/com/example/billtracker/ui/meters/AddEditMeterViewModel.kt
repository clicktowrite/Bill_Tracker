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

data class AddEditMeterUiState(
    val meterId: Long = 0,
    val isEditMode: Boolean = false,
    val name: String = "",
    val utilityType: UtilityType = UtilityType.ELECTRICITY,
    val startingReading: String = "0.0",
    val selectedTenantId: Long? = null,
    val rateType: RateType = RateType.FIXED,
    val fixedRate: String = "0.00",
    val availableTenants: List<Tenant> = emptyList(),
    val nameError: String? = null,
    val readingError: String? = null,
    val rateError: String? = null,
    val isSaved: Boolean = false,
    val currencySymbol: String = "₱"
)

class AddEditMeterViewModel(
    private val repository: BillTrackerRepository,
    private val meterIdToEdit: Long? = null,
    initialUtilityType: UtilityType? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(
        AddEditMeterUiState(
            utilityType = initialUtilityType ?: UtilityType.ELECTRICITY
        )
    )

    val uiState: StateFlow<AddEditMeterUiState> = combine(
        _formState,
        repository.allTenantsFlow,
        repository.currencySymbolFlow
    ) { form, tenants, currencySymbol ->
        form.copy(
            availableTenants = tenants,
            currencySymbol = currencySymbol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditMeterUiState()
    )

    init {
        if (meterIdToEdit != null && meterIdToEdit > 0) {
            viewModelScope.launch {
                val existing = repository.getMeterById(meterIdToEdit)
                if (existing != null) {
                    _formState.value = _formState.value.copy(
                        meterId = existing.id,
                        isEditMode = true,
                        name = existing.name,
                        utilityType = existing.utilityType,
                        startingReading = existing.currentReading,
                        selectedTenantId = existing.tenantId,
                        rateType = existing.rateType,
                        fixedRate = existing.fixedRate
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }

    fun onUtilityTypeChange(type: UtilityType) {
        _formState.value = _formState.value.copy(utilityType = type)
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

    fun saveMeter() {
        val current = _formState.value

        var isValid = true
        var nameErr: String? = null
        var readingErr: String? = null
        var rateErr: String? = null

        if (current.name.isBlank()) {
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
            val meter = Meter(
                id = current.meterId,
                name = current.name.trim(),
                utilityType = current.utilityType,
                tenantId = current.selectedTenantId,
                currentReading = current.startingReading.trim(),
                rateType = current.rateType,
                fixedRate = current.fixedRate.trim()
            )

            if (current.isEditMode) {
                repository.updateMeter(meter)
            } else {
                // Automatically record initial daily reading when creating new meter
                repository.saveMeterWithInitialReading(meter)
            }

            _formState.value = _formState.value.copy(isSaved = true)
        }
    }
}
