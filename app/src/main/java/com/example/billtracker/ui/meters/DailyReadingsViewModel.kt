package com.example.billtracker.ui.meters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.calculator.DailyInterpolationEngine
import com.example.billtracker.domain.calculator.ForecastCalculator
import com.example.billtracker.domain.calculator.ForecastInputReading
import com.example.billtracker.domain.calculator.ForecastResult
import com.example.billtracker.domain.calculator.InputReading
import com.example.billtracker.domain.calculator.InterpolatedDailyReading
import com.example.billtracker.domain.calculator.ReadingStatus
import com.example.billtracker.domain.model.DailyReading
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.RateType
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyReadingsUiState(
    val meter: Meter? = null,
    val interpolatedReadings: List<InterpolatedDailyReading> = emptyList(),
    val forecastResult: ForecastResult = ForecastResult.InsufficientData(),
    val isForecastEnabled: Boolean = true, // Estimation enabled setting
    val isForecastExpanded: Boolean = false, // Monthly forecast card expansion toggle
    val recordReadingTimeEnabled: Boolean = true,
    val availableTenants: List<Tenant> = emptyList(),
    val isBottomSheetOpen: Boolean = false,
    val editingReadingId: Long? = null, // Non-null when editing an existing actual reading
    val isSaving: Boolean = false,
    val newReadingValue: String = "",
    val selectedDateMillis: Long = 0L,
    val cycleStartDateMillis: Long? = null,
    val cycleEndDateMillis: Long? = null,
    val cycleDateRangeText: String = "", // Permitted billing cycle date range
    val hasTime: Boolean = false,
    val selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val isMeterReset: Boolean = false,
    val isLastReading: Boolean = false,
    val navigateToCalculateBillMeterId: Long? = null,
    val selectedDestinationTenantId: Long? = null,
    val showClosePeriodConfirmationDialog: Boolean = false,
    val periodClosedSuccess: Boolean = false,
    val notes: String = "",
    val error: String? = null,
    val readingWarning: String? = null,
    val successMessage: String? = null,
    val currencySymbol: String = "₱",
    val isLoading: Boolean = true
)

class DailyReadingsViewModel(
    private val repository: BillTrackerRepository,
    private val meterId: Long
) : ViewModel() {

    private val _formState = MutableStateFlow(DailyReadingsUiState())
    private var successMessageJob: Job? = null
    private val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

    private fun isSameCalendarDay(timeMillis1: Long, timeMillis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timeMillis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timeMillis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    val uiState: StateFlow<DailyReadingsUiState> = combine(
        _formState,
        repository.allMetersIncludingClosedFlow,
        repository.getDailyReadingsForMeter(meterId),
        repository.allTenantsFlow,
        repository.currencySymbolFlow
    ) { form, meters, actualDailyReadings, tenants, currencySymbol ->

        val meter = meters.find { it.id == meterId }
        val utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY
        val isForecastEnabled = form.isForecastEnabled.takeIf { _formState.value.isForecastEnabled != meter?.isForecastEnabled }
            ?: (meter?.isForecastEnabled ?: true)

        val inputReadings = actualDailyReadings.map { dr ->
            InputReading(
                id = dr.id,
                timestamp = dr.readingDate,
                value = dr.readingValue,
                hasTime = dr.hasTime,
                isMeterReset = dr.isMeterReset,
                notes = dr.notes
            )
        }

        val interpolatedList = if (isForecastEnabled) {
            DailyInterpolationEngine.interpolateReadings(
                actualReadings = inputReadings,
                forecastDays = 0,
                utilityType = utilityType
            ).filter { it.status == ReadingStatus.ACTUAL || it.status == ReadingStatus.ESTIMATED }
             .reversed()
        } else {
            val sortedInputs = inputReadings
                .mapNotNull { r ->
                    val bd = BillingCalculator.parseBigDecimal(r.value)
                    if (bd != null) r to bd else null
                }
                .sortedBy { it.first.timestamp }

            var prevVal: BigDecimal? = null
            val actuals = sortedInputs.map { (reading, actualVal) ->
                val consumption = if (prevVal != null) {
                    if (reading.isMeterReset || actualVal < prevVal) {
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    } else {
                        actualVal.subtract(prevVal).setScale(2, RoundingMode.HALF_UP)
                    }
                } else {
                    null // Baseline starting reading has no preceding actual reading
                }
                prevVal = actualVal

                InterpolatedDailyReading(
                    dateMillis = reading.timestamp,
                    readingValue = actualVal,
                    status = ReadingStatus.ACTUAL,
                    dailyConsumption = consumption,
                    hasTime = reading.hasTime,
                    actualReadingId = reading.id,
                    notes = reading.notes
                )
            }.sortedByDescending { it.dateMillis }
            actuals
        }

        // Calculate billing cycle date range for current tracking period
        val latestActualDate = actualDailyReadings.maxByOrNull { it.readingDate }?.readingDate
            ?: (meter?.createdAt ?: System.currentTimeMillis())

        val cycleStart = actualDailyReadings.minByOrNull { it.readingDate }?.readingDate ?: latestActualDate
        val cycleEnd = Calendar.getInstance().apply {
            timeInMillis = cycleStart
            add(Calendar.DAY_OF_MONTH, 30)
        }.timeInMillis

        val cycleRangeStr = "Permitted Cycle: ${dateFormatter.format(Date(cycleStart))} – ${dateFormatter.format(Date(cycleEnd))}"

        val forecastResult = if (actualDailyReadings.size >= 2) {
            try {
                val forecastInputs = actualDailyReadings.map { dr ->
                    ForecastInputReading(
                        timestamp = dr.readingDate,
                        value = dr.readingValue,
                        hasTime = dr.hasTime,
                        isMeterReset = dr.isMeterReset
                    )
                }
                ForecastCalculator.calculateForecast(
                    readings = forecastInputs,
                    utilityType = utilityType,
                    rateStr = meter?.fixedRate,
                    isRateDynamic = meter?.rateType == RateType.DYNAMIC,
                    totalBillingDays = 30
                )
            } catch (_: Exception) {
                ForecastResult.InsufficientData("Not enough valid readings to calculate forecast.")
            }
        } else {
            ForecastResult.InsufficientData("At least two valid actual meter readings are required to estimate a bill.")
        }

        form.copy(
            meter = meter,
            interpolatedReadings = interpolatedList,
            forecastResult = forecastResult,
            isForecastEnabled = isForecastEnabled,
            availableTenants = tenants,
            selectedDestinationTenantId = form.selectedDestinationTenantId ?: meter?.tenantId,
            selectedDateMillis = if (form.selectedDateMillis == 0L) latestActualDate else form.selectedDateMillis,
            cycleStartDateMillis = cycleStart,
            cycleEndDateMillis = cycleEnd,
            cycleDateRangeText = cycleRangeStr,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyReadingsUiState()
    )

    fun onOpenAddReadingForm() {
        viewModelScope.launch {
            val meter = repository.getMeterById(meterId)
            val actualReadings = repository.getDailyReadingsForMeter(meterId).first()
            val latestActual = actualReadings.maxByOrNull { it.readingDate }

            val latestActualValue = latestActual?.readingValue ?: meter?.currentReading ?: ""

            // Fixed Bug 2: Next reading date defaults to latest reading date + 1 day so adding a new reading creates a new entry on the next date!
            val baseDateMillis = latestActual?.readingDate ?: (meter?.createdAt ?: System.currentTimeMillis())
            val nextDateMillis = Calendar.getInstance().apply {
                timeInMillis = baseDateMillis
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis

            val cal = Calendar.getInstance().apply { timeInMillis = nextDateMillis }

            _formState.value = _formState.value.copy(
                isBottomSheetOpen = true,
                editingReadingId = null,
                newReadingValue = "",
                hasTime = false,
                selectedHour = cal.get(Calendar.HOUR_OF_DAY),
                selectedMinute = cal.get(Calendar.MINUTE),
                selectedDateMillis = nextDateMillis,
                error = null,
                readingWarning = null,
                navigateToCalculateBillMeterId = null,
                successMessage = null
            )
        }
    }

    fun onOpenEditReadingForm(readingId: Long) {
        viewModelScope.launch {
            val actualReadings = repository.getDailyReadingsForMeter(meterId).first()
            val targetReading = actualReadings.find { it.id == readingId } ?: return@launch

            val cal = Calendar.getInstance().apply { timeInMillis = targetReading.readingDate }

            _formState.value = _formState.value.copy(
                isBottomSheetOpen = true,
                editingReadingId = targetReading.id,
                newReadingValue = targetReading.readingValue,
                selectedDateMillis = targetReading.readingDate,
                hasTime = targetReading.hasTime,
                selectedHour = cal.get(Calendar.HOUR_OF_DAY),
                selectedMinute = cal.get(Calendar.MINUTE),
                isMeterReset = targetReading.isMeterReset,
                notes = targetReading.notes ?: "",
                error = null,
                readingWarning = null,
                navigateToCalculateBillMeterId = null,
                successMessage = null
            )
        }
    }

    fun onCloseAddReadingForm() {
        successMessageJob?.cancel()
        _formState.value = _formState.value.copy(
            isBottomSheetOpen = false,
            editingReadingId = null,
            newReadingValue = "",
            notes = "",
            isMeterReset = false,
            isLastReading = false,
            hasTime = false,
            error = null,
            readingWarning = null,
            navigateToCalculateBillMeterId = null,
            successMessage = null
        )
    }

    fun toggleForecastExpanded() {
        _formState.value = _formState.value.copy(
            isForecastExpanded = !_formState.value.isForecastExpanded
        )
    }

    fun toggleForecastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isForecastEnabled = enabled)
            val meter = repository.getMeterById(meterId) ?: return@launch
            repository.updateMeter(meter.copy(isForecastEnabled = enabled))
        }
    }

    fun onNewReadingValueChange(value: String) {
        val current = _formState.value
        val parsed = BillingCalculator.parseBigDecimal(value)

        val existingReadings = current.interpolatedReadings
            .filter { it.actualReadingId != null && it.readingValue != null && it.actualReadingId != current.editingReadingId }

        val precedingActual = existingReadings
            .filter { it.dateMillis <= current.selectedDateMillis }
            .maxByOrNull { it.dateMillis }

        val startingValue = current.meter?.currentReading?.let { BillingCalculator.parseBigDecimal(it) }

        // Bug 3 Fix: Reading MUST be strictly higher than preceding or starting reading
        val warning = if (parsed != null) {
            val baseline = precedingActual?.readingValue ?: startingValue
            if (baseline != null && parsed <= baseline && !current.isMeterReset) {
                "The new meter reading must be higher than the previous reading ($baseline)."
            } else null
        } else null

        _formState.value = current.copy(
            newReadingValue = value,
            error = null,
            readingWarning = warning
        )
    }

    fun onDateSelected(dateMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        if (_formState.value.hasTime) {
            cal.set(Calendar.HOUR_OF_DAY, _formState.value.selectedHour)
            cal.set(Calendar.MINUTE, _formState.value.selectedMinute)
        }
        val current = _formState.value
        val parsed = BillingCalculator.parseBigDecimal(current.newReadingValue)

        val existingReadings = current.interpolatedReadings
            .filter { it.actualReadingId != null && it.readingValue != null && it.actualReadingId != current.editingReadingId }

        val precedingActual = existingReadings
            .filter { it.dateMillis <= cal.timeInMillis }
            .maxByOrNull { it.dateMillis }

        val startingValue = current.meter?.currentReading?.let { BillingCalculator.parseBigDecimal(it) }

        var dateErr: String? = null

        val warning = if (parsed != null) {
            val baseline = precedingActual?.readingValue ?: startingValue
            if (baseline != null && parsed <= baseline && !current.isMeterReset) {
                "The new meter reading must be higher than the previous reading ($baseline)."
            } else null
        } else null

        _formState.value = current.copy(
            selectedDateMillis = cal.timeInMillis,
            error = dateErr,
            readingWarning = warning
        )
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _formState.value.selectedDateMillis }
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        _formState.value = _formState.value.copy(
            selectedHour = hour,
            selectedMinute = minute,
            selectedDateMillis = cal.timeInMillis,
            error = null
        )
    }

    fun onHasTimeToggle(hasTime: Boolean) {
        val cal = Calendar.getInstance().apply { timeInMillis = _formState.value.selectedDateMillis }
        if (hasTime) {
            cal.set(Calendar.HOUR_OF_DAY, _formState.value.selectedHour)
            cal.set(Calendar.MINUTE, _formState.value.selectedMinute)
        }
        _formState.value = _formState.value.copy(
            hasTime = hasTime,
            selectedDateMillis = cal.timeInMillis,
            error = null
        )
    }

    fun onMeterResetToggle(isReset: Boolean) {
        val current = _formState.value
        val warning = if (isReset) null else current.readingWarning
        _formState.value = current.copy(
            isMeterReset = isReset,
            error = null,
            readingWarning = warning
        )
    }

    fun onIsLastReadingToggle(isLast: Boolean) {
        _formState.value = _formState.value.copy(isLastReading = isLast, error = null)
    }

    fun onDestinationTenantSelect(tenantId: Long?) {
        _formState.value = _formState.value.copy(selectedDestinationTenantId = tenantId)
    }

    fun onDismissClosePeriodDialog() {
        _formState.value = _formState.value.copy(showClosePeriodConfirmationDialog = false)
    }

    fun onNotesChange(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun onNavigationHandled() {
        _formState.value = _formState.value.copy(navigateToCalculateBillMeterId = null)
    }

    fun logDailyReading() {
        val current = _formState.value
        if (current.isSaving) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, error = null, successMessage = null)

            val meter = repository.getMeterById(meterId)
            if (meter == null) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = "Meter not found."
                )
                return@launch
            }

            val parsed = BillingCalculator.parseBigDecimal(current.newReadingValue)
            if (parsed == null) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = "Please enter a valid non-negative meter reading."
                )
                return@launch
            }

            val existingReadings = current.interpolatedReadings
                .filter { it.actualReadingId != null && it.readingValue != null && it.actualReadingId != current.editingReadingId }

            val precedingActual = existingReadings
                .filter { it.dateMillis <= current.selectedDateMillis }
                .maxByOrNull { it.dateMillis }

            val startingValue = meter.currentReading.let { BillingCalculator.parseBigDecimal(it) }

            // Bug 3 Fix: Reading MUST be strictly higher than preceding or starting reading
            val baseline = precedingActual?.readingValue ?: startingValue
            if (baseline != null && parsed <= baseline && !current.isMeterReset) {
                val warnMsg = "The new meter reading must be higher than the previous reading ($baseline)."
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    readingWarning = warnMsg,
                    error = warnMsg
                )
                return@launch
            }

            val followingActual = existingReadings
                .filter { it.dateMillis > current.selectedDateMillis }
                .minByOrNull { it.dateMillis }

            if (followingActual != null && followingActual.readingValue != null) {
                if (parsed >= followingActual.readingValue) {
                    val warnMsg = "Reading ($parsed) cannot be higher than subsequent reading (${followingActual.readingValue})."
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        error = warnMsg
                    )
                    return@launch
                }
            }

            try {
                if (current.editingReadingId != null) {
                    val dr = DailyReading(
                        id = current.editingReadingId,
                        meterId = meter.id,
                        readingDate = current.selectedDateMillis,
                        readingValue = parsed.toString(),
                        hasTime = current.hasTime,
                        isMeterReset = current.isMeterReset,
                        notes = current.notes.trim().takeIf { it.isNotBlank() }
                    )
                    repository.updateDailyReading(dr)

                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        isBottomSheetOpen = true,
                        newReadingValue = parsed.toString(),
                        error = null,
                        readingWarning = null
                    )
                    showTemporarySuccessMessage("Reading updated successfully.")
                } else {
                    val dr = DailyReading(
                        meterId = meter.id,
                        readingDate = current.selectedDateMillis,
                        readingValue = parsed.toString(),
                        hasTime = current.hasTime,
                        isMeterReset = current.isMeterReset,
                        notes = current.notes.trim().takeIf { it.isNotBlank() }
                    )
                    repository.saveDailyReading(dr)

                    val isLast = current.isLastReading
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        isBottomSheetOpen = !isLast,
                        newReadingValue = parsed.toString(),
                        notes = "",
                        isMeterReset = false,
                        isLastReading = false,
                        error = null,
                        readingWarning = null,
                        navigateToCalculateBillMeterId = if (isLast) meter.id else null
                    )
                    if (!isLast) {
                        showTemporarySuccessMessage("Reading logged successfully.")
                    }
                }
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = e.localizedMessage ?: "Failed to save reading. Please try again."
                )
            }
        }
    }

    private fun showTemporarySuccessMessage(msg: String) {
        successMessageJob?.cancel()
        _formState.value = _formState.value.copy(successMessage = msg)
        successMessageJob = viewModelScope.launch {
            delay(2000L)
            _formState.value = _formState.value.copy(successMessage = null)
        }
    }

    fun confirmCloseTrackingPeriodAndTransfer() {
        val current = _formState.value
        val parsed = BillingCalculator.parseBigDecimal(current.newReadingValue) ?: return

        viewModelScope.launch {
            try {
                val meter = repository.getMeterById(meterId) ?: return@launch
                repository.closeTrackingPeriodAndTransfer(
                    meterId = meter.id,
                    finalReadingValue = parsed.toString(),
                    readingDate = current.selectedDateMillis,
                    hasTime = current.hasTime,
                    notes = current.notes.trim().takeIf { it.isNotBlank() },
                    destinationTenantId = current.selectedDestinationTenantId
                )

                _formState.value = _formState.value.copy(
                    showClosePeriodConfirmationDialog = false,
                    isBottomSheetOpen = false,
                    periodClosedSuccess = true,
                    newReadingValue = "",
                    notes = "",
                    isLastReading = false,
                    error = null,
                    readingWarning = null
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    showClosePeriodConfirmationDialog = false,
                    error = e.message ?: "Failed to close tracking period."
                )
            }
        }
    }

    fun deleteDailyReading(id: Long) {
        viewModelScope.launch {
            repository.deleteDailyReading(id)
        }
    }
}
