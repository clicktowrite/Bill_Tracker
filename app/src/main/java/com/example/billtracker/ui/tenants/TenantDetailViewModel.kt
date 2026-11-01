package com.example.billtracker.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.BillingCalculator
import com.example.billtracker.domain.calculator.DailyInterpolationEngine
import com.example.billtracker.domain.calculator.InputReading
import com.example.billtracker.domain.calculator.InterpolatedDailyReading
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

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

data class HistoricalReadingFormState(
    val isAddHistoricalReadingOpen: Boolean = false,
    val historicalTargetBillId: Long? = null,
    val historicalMeterId: Long? = null,
    val historicalFirstReadingVal: BigDecimal = BigDecimal.ZERO,
    val historicalLastReadingVal: BigDecimal = BigDecimal.ZERO,
    val historicalMinDateMillis: Long = 0L,
    val historicalMaxDateMillis: Long = 0L,
    val newHistoricalValue: String = "",
    val selectedHistoricalDateMillis: Long = System.currentTimeMillis(),
    val historicalError: String? = null,
    val isSavingHistorical: Boolean = false
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
    val historicalForm: HistoricalReadingFormState = HistoricalReadingFormState(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class TenantDetailViewModel(
    private val repository: BillTrackerRepository,
    private val tenantId: Long
) : ViewModel() {

    private val _historicalForm = MutableStateFlow(HistoricalReadingFormState())

    // Bug 2 Fix: Query ALL meters (including closed tracking sessions) for this tenant
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

                    val interpolated = if (meter.isForecastEnabled) {
                        DailyInterpolationEngine.interpolateReadings(
                            actualReadings = inputs,
                            forecastDays = 0,
                            utilityType = meter.utilityType
                        ).filter { it.status == ReadingStatus.ACTUAL || it.status == ReadingStatus.ESTIMATED }
                    } else {
                        val sortedInputs = inputs
                            .mapNotNull { r ->
                                val bd = BillingCalculator.parseBigDecimal(r.value)
                                if (bd != null) r to bd else null
                            }
                            .sortedBy { it.first.timestamp }

                        var prevVal: BigDecimal? = null
                        sortedInputs.map { (reading, actualVal) ->
                            val consumption = if (prevVal != null) {
                                if (reading.isMeterReset || actualVal < prevVal) {
                                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                                } else {
                                    actualVal.subtract(prevVal).setScale(2, RoundingMode.HALF_UP)
                                }
                            } else null
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
                        }
                    }

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

        // Bug 2 Fix: Match every saved bill to its EXACT daily log history (First Reading Date -> Last Reading Date)
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
        _historicalForm
    ) { base, formState ->
        base.copy(historicalForm = formState)
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

    // Bug 3 Fix: Open form to add a missed historical reading inside a completed billing cycle
    fun onOpenAddHistoricalReading(
        billId: Long,
        meterId: Long,
        firstValStr: String,
        lastValStr: String,
        startDateMillis: Long,
        endDateMillis: Long
    ) {
        val firstVal = BillingCalculator.parseBigDecimal(firstValStr) ?: BigDecimal.ZERO
        val lastVal = BillingCalculator.parseBigDecimal(lastValStr) ?: BigDecimal.ZERO

        _historicalForm.value = HistoricalReadingFormState(
            isAddHistoricalReadingOpen = true,
            historicalTargetBillId = billId,
            historicalMeterId = meterId,
            historicalFirstReadingVal = firstVal,
            historicalLastReadingVal = lastVal,
            historicalMinDateMillis = startDateMillis,
            historicalMaxDateMillis = endDateMillis,
            newHistoricalValue = "",
            selectedHistoricalDateMillis = (startDateMillis + endDateMillis) / 2,
            historicalError = null
        )
    }

    fun onCloseAddHistoricalReading() {
        _historicalForm.value = HistoricalReadingFormState()
    }

    fun onNewHistoricalValueChange(value: String) {
        _historicalForm.value = _historicalForm.value.copy(newHistoricalValue = value, historicalError = null)
    }

    fun onHistoricalDateSelected(dateMillis: Long) {
        _historicalForm.value = _historicalForm.value.copy(selectedHistoricalDateMillis = dateMillis, historicalError = null)
    }

    // Bug 3 Fix: Validate newly added historical reading (> First Reading and < Last Reading)
    fun saveHistoricalReading() {
        val current = _historicalForm.value
        val meterId = current.historicalMeterId ?: return
        val parsed = BillingCalculator.parseBigDecimal(current.newHistoricalValue)

        if (parsed == null) {
            _historicalForm.value = current.copy(historicalError = "Please enter a valid non-negative reading value.")
            return
        }

        // Bug 3 Rule: Reading MUST be > First Reading AND < Last Reading
        val firstVal = current.historicalFirstReadingVal
        val lastVal = current.historicalLastReadingVal

        if (parsed <= firstVal || parsed >= lastVal) {
            _historicalForm.value = current.copy(
                historicalError = "Historical reading must be greater than the first reading ($firstVal) and less than the last reading ($lastVal)."
            )
            return
        }

        // Date must be strictly between start date and end date
        if (current.selectedHistoricalDateMillis <= current.historicalMinDateMillis ||
            current.selectedHistoricalDateMillis >= current.historicalMaxDateMillis
        ) {
            _historicalForm.value = current.copy(
                historicalError = "Selected date must be between the billing cycle start and end dates."
            )
            return
        }

        viewModelScope.launch {
            _historicalForm.value = current.copy(isSavingHistorical = true)

            try {
                val dr = DailyReading(
                    meterId = meterId,
                    readingDate = current.selectedHistoricalDateMillis,
                    readingValue = parsed.toString(),
                    hasTime = false,
                    isMeterReset = false,
                    notes = "Forgotten Reading Added to Completed Cycle"
                )
                repository.saveDailyReading(dr)

                _historicalForm.value = HistoricalReadingFormState()
            } catch (e: Exception) {
                _historicalForm.value = current.copy(
                    isSavingHistorical = false,
                    historicalError = e.localizedMessage ?: "Failed to save historical reading."
                )
            }
        }
    }
}
