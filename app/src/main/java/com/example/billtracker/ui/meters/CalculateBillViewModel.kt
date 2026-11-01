package com.example.billtracker.ui.meters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.calculator.CalculationResult
import com.example.billtracker.domain.model.DailyReading
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.Tenant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class CalculateBillUiState(
    val meter: Meter? = null,
    val tenant: Tenant? = null,
    val selectedTenantId: Long? = null,
    val availableTenants: List<Tenant> = emptyList(),
    val actualReadings: List<DailyReading> = emptyList(),
    val previousReading: String = "0.0",
    val currentReading: String = "",
    val billingDateMillis: Long = 0L, // Fixed: Default to 0L so it inherits the exact last reading date from ViewModel init
    val hasTime: Boolean = false,
    val isMeterReset: Boolean = false,
    val rateType: RateType = RateType.FIXED,
    val fixedRate: String = "0.00",
    val providerTotalBill: String = "",
    val providerTotalUsage: String = "",
    val dynamicCalculatedRate: String? = null,
    val calculatedUsage: BigDecimal = BigDecimal.ZERO,
    val calculatedRate: BigDecimal = BigDecimal.ZERO,
    val calculatedTotalBill: BigDecimal = BigDecimal.ZERO,
    val notes: String = "",
    val photoPath: String? = null,
    val calculationError: String? = null,
    val isSavingBill: Boolean = false,
    val savedReadingId: Long? = null,
    val savedSuccessTenantId: Long? = null,
    val currencySymbol: String = "₱",
    // Add Profile Popup Sheet State
    val isAddProfileSheetOpen: Boolean = false,
    val isSavingProfile: Boolean = false,
    val newProfileName: String = "",
    val newProfileRoom: String = "",
    val newProfileNotes: String = "",
    val newProfileNameError: String? = null,
    val newProfileRoomError: String? = null
)

class CalculateBillViewModel(
    private val repository: BillTrackerRepository,
    private val meterId: Long
) : ViewModel() {

    private val _formState = MutableStateFlow(CalculateBillUiState())

    val uiState: StateFlow<CalculateBillUiState> = combine(
        _formState,
        repository.allTenantsFlow,
        repository.currencySymbolFlow,
        repository.recordReadingTimeFlow
    ) { form, tenants, currencySymbol, recordTimeEnabled ->
        form.copy(
            availableTenants = tenants,
            currencySymbol = currencySymbol,
            hasTime = if (form.savedReadingId == null && form.currentReading.isEmpty()) recordTimeEnabled else form.hasTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculateBillUiState()
    )

    init {
        viewModelScope.launch {
            val meter = repository.getMeterById(meterId)
            if (meter != null) {
                val tenant = meter.tenantId?.let { repository.getTenantById(it) }

                // Retrieve daily readings for this meter sorted chronologically
                val dailyReadings = repository.getDailyReadingsForMeter(meterId).first()
                    .sortedBy { it.readingDate }

                val earliestReading = dailyReadings.firstOrNull()?.readingValue ?: meter.currentReading
                val latestReading = dailyReadings.lastOrNull()?.readingValue ?: meter.currentReading

                // Fixed: Inherit the exact date of the last actual daily reading in this cycle as default billingDate
                val latestReadingDate = dailyReadings.lastOrNull()?.readingDate ?: System.currentTimeMillis()

                _formState.value = _formState.value.copy(
                    meter = meter,
                    tenant = tenant,
                    selectedTenantId = meter.tenantId,
                    actualReadings = dailyReadings,
                    previousReading = earliestReading,
                    currentReading = latestReading,
                    billingDateMillis = if (_formState.value.billingDateMillis == 0L) latestReadingDate else _formState.value.billingDateMillis,
                    rateType = meter.rateType,
                    fixedRate = meter.fixedRate
                )
                recalculate()
            }
        }
    }

    fun onTenantSelect(tenantId: Long?) {
        viewModelScope.launch {
            val tenant = tenantId?.let { repository.getTenantById(it) }
            _formState.value = _formState.value.copy(
                selectedTenantId = tenantId,
                tenant = tenant
            )
        }
    }

    fun onOpenAddProfileSheet() {
        _formState.value = _formState.value.copy(
            isAddProfileSheetOpen = true,
            newProfileName = "",
            newProfileRoom = "",
            newProfileNotes = "",
            newProfileNameError = null,
            newProfileRoomError = null
        )
    }

    fun onCloseAddProfileSheet() {
        _formState.value = _formState.value.copy(
            isAddProfileSheetOpen = false,
            newProfileName = "",
            newProfileRoom = "",
            newProfileNotes = "",
            newProfileNameError = null,
            newProfileRoomError = null
        )
    }

    fun onNewProfileNameChange(value: String) {
        _formState.value = _formState.value.copy(newProfileName = value, newProfileNameError = null)
    }

    fun onNewProfileRoomChange(value: String) {
        _formState.value = _formState.value.copy(newProfileRoom = value, newProfileRoomError = null)
    }

    fun onNewProfileNotesChange(value: String) {
        _formState.value = _formState.value.copy(newProfileNotes = value)
    }

    fun saveProfileAndResumeBillSave() {
        val current = _formState.value
        if (current.isSavingProfile) return

        var isValid = true
        var nameErr: String? = null
        var roomErr: String? = null

        if (current.newProfileName.isBlank()) {
            nameErr = "Profile name is required."
            isValid = false
        }

        if (current.newProfileRoom.isBlank()) {
            roomErr = "Room or unit number is required."
            isValid = false
        }

        if (!isValid) {
            _formState.value = current.copy(
                newProfileNameError = nameErr,
                newProfileRoomError = roomErr
            )
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSavingProfile = true)

            try {
                val tenant = Tenant(
                    name = current.newProfileName.trim(),
                    roomOrUnit = current.newProfileRoom.trim(),
                    notes = current.newProfileNotes.trim().takeIf { it.isNotBlank() }
                )

                val newTenantId = repository.saveTenant(tenant)
                val newTenant = repository.getTenantById(newTenantId)

                // Update meter tenantId in DB
                current.meter?.let { m ->
                    repository.updateMeter(m.copy(tenantId = newTenantId))
                }

                _formState.value = _formState.value.copy(
                    isSavingProfile = false,
                    isAddProfileSheetOpen = false,
                    selectedTenantId = newTenantId,
                    tenant = newTenant,
                    meter = current.meter?.copy(tenantId = newTenantId)
                )

                executeSaveBill(newTenantId)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSavingProfile = false,
                    calculationError = e.localizedMessage ?: "Failed to create profile."
                )
            }
        }
    }

    fun onCurrentReadingChange(value: String) {
        _formState.value = _formState.value.copy(currentReading = value, calculationError = null)
        recalculate()
    }

    fun onBillingDateChange(millis: Long) {
        _formState.value = _formState.value.copy(billingDateMillis = millis)
    }

    fun onHasTimeToggle(hasTime: Boolean) {
        _formState.value = _formState.value.copy(hasTime = hasTime)
    }

    fun onMeterResetToggle(isReset: Boolean) {
        _formState.value = _formState.value.copy(isMeterReset = isReset, calculationError = null)
        recalculate()
    }

    fun onRateTypeChange(rateType: RateType) {
        _formState.value = _formState.value.copy(rateType = rateType, calculationError = null)
        recalculate()
    }

    fun onFixedRateChange(value: String) {
        _formState.value = _formState.value.copy(fixedRate = value, calculationError = null)
        recalculate()
    }

    fun onProviderTotalBillChange(value: String) {
        _formState.value = _formState.value.copy(providerTotalBill = value, calculationError = null)
        recalculate()
    }

    fun onProviderTotalUsageChange(value: String) {
        _formState.value = _formState.value.copy(providerTotalUsage = value, calculationError = null)
        recalculate()
    }

    fun onNotesChange(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun onPhotoPathChange(path: String?) {
        _formState.value = _formState.value.copy(photoPath = path)
    }

    private fun recalculate() {
        val current = _formState.value

        // Priority 2: Validate at least 2 actual readings exist
        if (current.actualReadings.size < 2) {
            _formState.value = current.copy(
                calculatedUsage = BigDecimal.ZERO,
                calculatedTotalBill = BigDecimal.ZERO,
                calculationError = "At least two actual meter readings are required to calculate a bill."
            )
            return
        }

        if (current.currentReading.isBlank()) {
            _formState.value = current.copy(
                calculatedUsage = BigDecimal.ZERO,
                calculatedTotalBill = BigDecimal.ZERO,
                calculationError = null
            )
            return
        }

        val currBD = BillingCalculator.parseBigDecimal(current.currentReading)
        val prevBD = BillingCalculator.parseBigDecimal(current.previousReading) ?: BigDecimal.ZERO

        if (currBD == null) {
            _formState.value = current.copy(
                calculatedUsage = BigDecimal.ZERO,
                calculatedTotalBill = BigDecimal.ZERO,
                calculationError = "Current reading must be a valid non-negative number."
            )
            return
        }

        if (currBD < prevBD && !current.isMeterReset) {
            _formState.value = current.copy(
                calculatedUsage = BigDecimal.ZERO,
                calculatedTotalBill = BigDecimal.ZERO,
                calculationError = "New reading ($currBD) is lower than previous reading ($prevBD). Select 'Meter Reset / Replaced' if the meter was reset."
            )
            return
        }

        val effectivePrevStr = if (current.isMeterReset) "0.0" else current.previousReading

        val result = if (current.rateType == RateType.FIXED) {
            BillingCalculator.calculateFixedRateBill(
                previousReadingStr = effectivePrevStr,
                currentReadingStr = current.currentReading,
                fixedRateStr = current.fixedRate
            )
        } else {
            val (dynRate, dynErr) = BillingCalculator.calculateDynamicRate(
                providerTotalBillStr = current.providerTotalBill,
                providerTotalUsageStr = current.providerTotalUsage
            )
            _formState.value = _formState.value.copy(
                dynamicCalculatedRate = dynRate?.setScale(2, RoundingMode.HALF_UP)?.toString()
            )

            if (dynErr != null) {
                CalculationResult.Error(dynErr)
            } else {
                BillingCalculator.calculateDynamicRateBill(
                    previousReadingStr = effectivePrevStr,
                    currentReadingStr = current.currentReading,
                    providerTotalBillStr = current.providerTotalBill,
                    providerTotalUsageStr = current.providerTotalUsage
                )
            }
        }

        when (result) {
            is CalculationResult.Success -> {
                _formState.value = _formState.value.copy(
                    calculatedUsage = result.usage,
                    calculatedRate = result.effectiveRate,
                    calculatedTotalBill = result.totalBill,
                    calculationError = null
                )
            }
            is CalculationResult.Error -> {
                _formState.value = _formState.value.copy(
                    calculatedUsage = BigDecimal.ZERO,
                    calculatedTotalBill = BigDecimal.ZERO,
                    calculationError = result.message
                )
            }
        }
    }

    fun saveBill() {
        val current = _formState.value
        if (current.isSavingBill) return

        val meter = current.meter ?: return

        recalculate()

        val updatedState = _formState.value

        // Priority 2: Block save if fewer than 2 actual readings exist
        if (current.actualReadings.size < 2) {
            _formState.value = updatedState.copy(
                calculationError = "At least two actual meter readings are required to calculate a bill."
            )
            return
        }

        if (updatedState.calculationError != null || updatedState.currentReading.isBlank()) {
            if (updatedState.currentReading.isBlank()) {
                _formState.value = updatedState.copy(calculationError = "Please enter current meter reading.")
            }
            return
        }

        val effectiveTenantId = current.selectedTenantId ?: meter.tenantId

        if (effectiveTenantId == null || effectiveTenantId <= 0) {
            onOpenAddProfileSheet()
            return
        }

        executeSaveBill(effectiveTenantId)
    }

    private fun executeSaveBill(tenantId: Long) {
        val current = _formState.value
        val meter = current.meter ?: return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSavingBill = true)

            try {
                val effectiveBillingDate = if (current.billingDateMillis > 0L) current.billingDateMillis else System.currentTimeMillis()

                val reading = MeterReading(
                    meterId = meter.id,
                    tenantId = tenantId,
                    billingDate = effectiveBillingDate,
                    previousReading = if (current.isMeterReset) "0.0" else current.previousReading,
                    currentReading = current.currentReading,
                    usage = current.calculatedUsage.toString(),
                    rate = current.calculatedRate.toString(),
                    rateType = current.rateType,
                    providerTotalBill = if (current.rateType == RateType.DYNAMIC) current.providerTotalBill else null,
                    providerTotalUsage = if (current.rateType == RateType.DYNAMIC) current.providerTotalUsage else null,
                    totalAmount = current.calculatedTotalBill.toString(),
                    notes = current.notes.takeIf { it.isNotBlank() },
                    photoPath = current.photoPath,
                    hasTime = current.hasTime,
                    isMeterReset = current.isMeterReset,
                    isPaid = false
                )

                val id = repository.closeTrackingPeriodAndSaveBill(reading, tenantId)
                _formState.value = _formState.value.copy(
                    isSavingBill = false,
                    savedReadingId = id,
                    savedSuccessTenantId = tenantId
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSavingBill = false,
                    calculationError = e.localizedMessage ?: "Failed to save bill. Please try again."
                )
            }
        }
    }
}
