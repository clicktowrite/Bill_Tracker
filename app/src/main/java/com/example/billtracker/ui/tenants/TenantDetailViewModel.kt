package com.example.billtracker.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.calculator.DailyInterpolationEngine
import com.example.billtracker.domain.calculator.InputReading
import com.example.billtracker.domain.calculator.InterpolatedDailyReading
import com.example.billtracker.domain.calculator.MissingReadingValidationResult
import com.example.billtracker.domain.calculator.MissingReadingValidator
import com.example.billtracker.domain.calculator.ReadingStatus
import com.example.billtracker.domain.model.DailyReading
import com.example.billtracker.domain.model.Meter
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class MeterWithLatestReading(
    val meter: Meter,
    val latestReading: MeterReading?
)

data class ProfileDailyReadingItem(
    val meterId: Long,
    val meterName: String,
    val utilityType: UtilityType,
    val reading: InterpolatedDailyReading
)

data class BillHistoryItemModel(
    val reading: MeterReading,
    val meterName: String,
    val utilityType: UtilityType,
    val cycleStartDateMillis: Long,
    val cycleEndDateMillis: Long,
    val firstReadingValue: String,
    val lastReadingValue: String,
    val cycleLogs: List<ProfileDailyReadingItem>
)

data class EditProfileReadingState(
    val isOpen: Boolean = false,
    val editingReadingId: Long? = null,
    val meterId: Long = 0L,
    val value: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val photoPath: String? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmDialog: Boolean = false
)

data class AddMissingReadingState(
    val isOpen: Boolean = false,
    val billId: Long = 0L,
    val meterId: Long = 0L,
    val value: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val photoPath: String? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class TenantDetailUiState(
    val tenant: Tenant? = null,
    val meters: List<MeterWithLatestReading> = emptyList(),
    val readingsHistory: List<BillHistoryItemModel> = emptyList(),
    val dailyReadingsHistory: List<ProfileDailyReadingItem> = emptyList(),
    val latestElectricityBill: BigDecimal = BigDecimal.ZERO,
    val latestWaterBill: BigDecimal = BigDecimal.ZERO,
    val totalDue: BigDecimal = BigDecimal.ZERO,
    val currencySymbol: String = "₱",
    val cycleEstimationMap: Map<Long, Boolean> = emptyMap(), // Eye icon toggle state per billing cycle (default true)
    val addMissingReadingState: AddMissingReadingState = AddMissingReadingState(),
    val editReadingState: EditProfileReadingState = EditProfileReadingState(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class TenantDetailViewModel(
    private val repository: BillTrackerRepository,
    private val tenantId: Long
) : ViewModel() {

    private val _cycleEstimationMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val _editReadingState = MutableStateFlow(EditProfileReadingState())
    private val _addMissingReadingState = MutableStateFlow(AddMissingReadingState())

    // Query ALL meters (including closed tracking sessions) for this tenant
    private val allTenantMetersFlow = repository.getAllMetersForTenant(tenantId)

    private val dailyReadingsForTenantFlow = allTenantMetersFlow.flatMapLatest { meters ->
        if (meters.isEmpty()) {
            flowOf(emptyList<ProfileDailyReadingItem>())
        } else {
            val flows = meters.map { meter ->
                repository.getDailyReadingsForMeter(meter.id).map { drs ->
                    val inputs = drs.map { dr ->
                        InputReading(
                            id = dr.id,
                            timestamp = dr.readingDate,
                            value = dr.readingValue,
                            hasTime = dr.hasTime,
                            isMeterReset = dr.isMeterReset,
                            notes = dr.notes
                        )
                    }

                    val interpolated = DailyInterpolationEngine.interpolateReadings(
                        actualReadings = inputs,
                        forecastDays = 0,
                        utilityType = meter.utilityType
                    ).filter { it.status == ReadingStatus.ACTUAL || it.status == ReadingStatus.ESTIMATED }

                    interpolated.map { ir ->
                        ProfileDailyReadingItem(
                            meterId = meter.id,
                            meterName = meter.name,
                            utilityType = meter.utilityType,
                            reading = ir
                        )
                    }
                }
            }

            combine(flows) { arrayOfLists ->
                arrayOfLists.flatMap { it }.sortedByDescending { it.reading.dateMillis }
            }
        }
    }

    private val baseUiState = combine(
        repository.allTenantsFlow,
        allTenantMetersFlow,
        repository.getReadingsForTenant(tenantId),
        dailyReadingsForTenantFlow,
        repository.currencySymbolFlow
    ) { tenants, allMeters, readings, dailyLogHistory, currencySymbol ->

        val tenant = tenants.find { it.id == tenantId }
        val activeMeters = allMeters.filter { !it.isClosed }

        val metersWithReadings = activeMeters.map { meter ->
            val latest = readings.filter { it.meterId == meter.id }.maxByOrNull { it.billingDate }
            MeterWithLatestReading(meter, latest)
        }

        val latestElec = readings
            .filter { r -> activeMeters.any { m -> m.id == r.meterId && m.utilityType == UtilityType.ELECTRICITY } }
            .maxByOrNull { it.billingDate }

        val latestWater = readings
            .filter { r -> activeMeters.any { m -> m.id == r.meterId && m.utilityType == UtilityType.WATER } }
            .maxByOrNull { it.billingDate }

        val elecAmt = latestElec?.let {
            try { BigDecimal(it.totalAmount.replace(",", "")) } catch (_: Exception) { BigDecimal.ZERO }
        } ?: BigDecimal.ZERO

        val waterAmt = latestWater?.let {
            try { BigDecimal(it.totalAmount.replace(",", "")) } catch (_: Exception) { BigDecimal.ZERO }
        } ?: BigDecimal.ZERO

        val totalDue = elecAmt.add(waterAmt).setScale(2, RoundingMode.HALF_UP)

        // Match every saved bill to its EXACT daily log history (First Reading Date -> Last Reading Date)
        val billHistoryModels = readings.sortedByDescending { it.billingDate }.map { reading ->
            val meter = allMeters.find { it.id == reading.meterId }
            val meterName = meter?.name ?: "Meter #${reading.meterId}"
            val utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY

            val meterLogs = dailyLogHistory.filter { it.meterId == reading.meterId && it.reading.dateMillis <= reading.billingDate }

            val minLog = meterLogs.minByOrNull { it.reading.dateMillis }
            val maxLog = meterLogs.maxByOrNull { it.reading.dateMillis }

            val cycleStart = minLog?.reading?.dateMillis ?: reading.billingDate
            val cycleEnd = maxLog?.reading?.dateMillis ?: reading.billingDate

            val firstValStr = minLog?.reading?.readingValue?.toString() ?: reading.previousReading
            val lastValStr = maxLog?.reading?.readingValue?.toString() ?: reading.currentReading

            BillHistoryItemModel(
                reading = reading,
                meterName = meterName,
                utilityType = utilityType,
                cycleStartDateMillis = cycleStart,
                cycleEndDateMillis = cycleEnd,
                firstReadingValue = firstValStr,
                lastReadingValue = lastValStr,
                cycleLogs = meterLogs.sortedBy { it.reading.dateMillis }
            )
        }

        TenantDetailUiState(
            tenant = tenant,
            meters = metersWithReadings,
            readingsHistory = billHistoryModels,
            dailyReadingsHistory = dailyLogHistory,
            latestElectricityBill = elecAmt.setScale(2, RoundingMode.HALF_UP),
            latestWaterBill = waterAmt.setScale(2, RoundingMode.HALF_UP),
            totalDue = totalDue,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }

    val uiState: StateFlow<TenantDetailUiState> = combine(
        baseUiState,
        _cycleEstimationMap,
        _editReadingState,
        _addMissingReadingState
    ) { base, estimationMap, editState, missingState ->
        base.copy(
            cycleEstimationMap = estimationMap,
            editReadingState = editState,
            addMissingReadingState = missingState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantDetailUiState()
    )

    fun togglePaymentStatus(readingId: Long, isPaid: Boolean) {
        viewModelScope.launch {
            repository.updatePaymentStatus(readingId, isPaid)
        }
    }

    // Toggle Eye Icon estimation visibility for a specific billing cycle
    fun toggleCycleEstimation(billId: Long, enabled: Boolean) {
        val current = _cycleEstimationMap.value.toMutableMap()
        current[billId] = enabled
        _cycleEstimationMap.value = current
    }

    fun onOpenEditReading(actualReadingId: Long, meterId: Long) {
        viewModelScope.launch {
            val drs = repository.getDailyReadingsForMeter(meterId).first()
            val target = drs.find { it.id == actualReadingId } ?: return@launch

            _editReadingState.value = EditProfileReadingState(
                isOpen = true,
                editingReadingId = target.id,
                meterId = meterId,
                value = target.readingValue,
                dateMillis = target.readingDate,
                photoPath = target.photoPath,
                notes = target.notes ?: "",
                error = null,
                showDeleteConfirmDialog = false
            )
        }
    }

    fun onCloseEditReading() {
        _editReadingState.value = EditProfileReadingState()
    }

    fun onEditValueChange(value: String) {
        _editReadingState.value = _editReadingState.value.copy(value = value, error = null)
    }

    fun onEditDateChange(dateMillis: Long) {
        _editReadingState.value = _editReadingState.value.copy(dateMillis = dateMillis, error = null)
    }

    fun onEditNotesChange(notes: String) {
        _editReadingState.value = _editReadingState.value.copy(notes = notes)
    }

    fun onEditPhotoPathChange(path: String?) {
        _editReadingState.value = _editReadingState.value.copy(photoPath = path)
    }

    fun requestDeleteReading() {
        _editReadingState.value = _editReadingState.value.copy(showDeleteConfirmDialog = true)
    }

    fun dismissDeleteReading() {
        _editReadingState.value = _editReadingState.value.copy(showDeleteConfirmDialog = false)
    }

    fun confirmDeleteReading() {
        val current = _editReadingState.value
        val readingId = current.editingReadingId ?: return

        viewModelScope.launch {
            try {
                val drs = repository.getDailyReadingsForMeter(current.meterId).first()
                val target = drs.find { it.id == readingId }
                if (target != null) {
                    val readings = repository.getReadingsForTenant(tenantId).first()
                    val isBelongingToFinalizedBill = readings.any { r ->
                        r.isFinalizedBill && r.meterId == current.meterId &&
                        (r.billingDate == target.readingDate || (r.currentReading == target.readingValue && r.billingDate >= target.readingDate))
                    }
                    if (isBelongingToFinalizedBill) {
                        _editReadingState.value = current.copy(
                            error = "This reading is part of a finalized bill and cannot be deleted.",
                            showDeleteConfirmDialog = false
                        )
                        return@launch
                    }
                }

                repository.deleteDailyReading(readingId)
                _editReadingState.value = EditProfileReadingState()
            } catch (e: Exception) {
                _editReadingState.value = current.copy(
                    error = e.localizedMessage ?: "Failed to delete reading.",
                    showDeleteConfirmDialog = false
                )
            }
        }
    }

    // --- Add Missing Reading ---
    fun onOpenAddMissingReading(billId: Long, meterId: Long, initialDateMillis: Long) {
        _addMissingReadingState.value = AddMissingReadingState(
            isOpen = true,
            billId = billId,
            meterId = meterId,
            dateMillis = initialDateMillis,
            value = "",
            photoPath = null,
            notes = "",
            isSaving = false,
            error = null,
            successMessage = null
        )
    }

    fun onCloseAddMissingReading() {
        _addMissingReadingState.value = AddMissingReadingState()
    }

    fun onMissingReadingValueChange(value: String) {
        _addMissingReadingState.value = _addMissingReadingState.value.copy(value = value, error = null)
    }

    fun onMissingReadingDateChange(dateMillis: Long) {
        _addMissingReadingState.value = _addMissingReadingState.value.copy(dateMillis = dateMillis, error = null)
    }

    fun onMissingReadingNotesChange(notes: String) {
        _addMissingReadingState.value = _addMissingReadingState.value.copy(notes = notes)
    }

    fun onMissingReadingPhotoPathChange(path: String?) {
        _addMissingReadingState.value = _addMissingReadingState.value.copy(photoPath = path)
    }

    fun saveMissingReading() {
        val current = _addMissingReadingState.value
        val parsed = BillingCalculator.parseBigDecimal(current.value)

        if (parsed == null) {
            _addMissingReadingState.value = current.copy(error = "Please enter a valid non-negative reading value.")
            return
        }

        viewModelScope.launch {
            _addMissingReadingState.value = current.copy(isSaving = true)

            try {
                // Fetch existing actual readings for this meter
                val drs = repository.getDailyReadingsForMeter(current.meterId).first()
                val actualPoints = drs.mapNotNull { dr ->
                    BillingCalculator.parseBigDecimal(dr.readingValue)?.let { bd ->
                        MissingReadingValidator.ActualReadingPoint(
                            id = dr.id,
                            timestamp = dr.readingDate,
                            value = bd
                        )
                    }
                }

                // Strict neighboring validation
                val validation = MissingReadingValidator.validate(
                    newTimestamp = current.dateMillis,
                    newValue = parsed,
                    actualReadings = actualPoints
                )

                if (validation is MissingReadingValidationResult.Invalid) {
                    _addMissingReadingState.value = current.copy(
                        isSaving = false,
                        error = validation.message
                    )
                    return@launch
                }

                val newReading = DailyReading(
                    meterId = current.meterId,
                    readingDate = current.dateMillis,
                    readingValue = parsed.toString(),
                    hasTime = false,
                    isMeterReset = false,
                    photoPath = current.photoPath,
                    notes = current.notes.trim().takeIf { it.isNotBlank() }
                )

                repository.saveDailyReading(newReading)

                _addMissingReadingState.value = AddMissingReadingState()
            } catch (e: Exception) {
                _addMissingReadingState.value = current.copy(
                    isSaving = false,
                    error = e.localizedMessage ?: "Failed to save missing reading."
                )
            }
        }
    }

    fun saveEditedReading() {
        val current = _editReadingState.value
        val readingId = current.editingReadingId ?: return
        val parsed = BillingCalculator.parseBigDecimal(current.value)

        if (parsed == null) {
            _editReadingState.value = current.copy(error = "Please enter a valid non-negative reading value.")
            return
        }

        viewModelScope.launch {
            _editReadingState.value = current.copy(isSaving = true)

            try {
                val drs = repository.getDailyReadingsForMeter(current.meterId).first()
                val target = drs.find { it.id == readingId } ?: return@launch

                // Exclude current reading when performing neighbor validation
                val actualPoints = drs.mapNotNull { dr ->
                    BillingCalculator.parseBigDecimal(dr.readingValue)?.let { bd ->
                        MissingReadingValidator.ActualReadingPoint(
                            id = dr.id,
                            timestamp = dr.readingDate,
                            value = bd
                        )
                    }
                }

                // Protection: Check if this reading is part of a finalized bill
                val readings = repository.getReadingsForTenant(tenantId).first()
                val isBelongingToFinalizedBill = readings.any { r ->
                    r.isFinalizedBill && r.meterId == current.meterId &&
                    (r.billingDate == target.readingDate || (r.currentReading == target.readingValue && r.billingDate >= target.readingDate))
                }

                val isValueOrDateChanged = parsed.toString() != target.readingValue || current.dateMillis != target.readingDate
                if (isBelongingToFinalizedBill && isValueOrDateChanged) {
                    _editReadingState.value = current.copy(
                        isSaving = false,
                        error = "This reading is part of a finalized bill and its reading value/date cannot be modified."
                    )
                    return@launch
                }

                val validation = MissingReadingValidator.validate(
                    newTimestamp = current.dateMillis,
                    newValue = parsed,
                    actualReadings = actualPoints,
                    isEditMode = true,
                    excludeId = readingId
                )

                if (validation is MissingReadingValidationResult.Invalid) {
                    _editReadingState.value = current.copy(
                        isSaving = false,
                        error = validation.message
                    )
                    return@launch
                }

                val updatedDr = target.copy(
                    readingValue = parsed.toString(),
                    readingDate = current.dateMillis,
                    photoPath = current.photoPath,
                    notes = current.notes.trim().takeIf { it.isNotBlank() }
                )

                repository.updateDailyReading(updatedDr)

                _editReadingState.value = EditProfileReadingState()
            } catch (e: Exception) {
                _editReadingState.value = current.copy(
                    isSaving = false,
                    error = e.localizedMessage ?: "Failed to update reading."
                )
            }
        }
    }
}
