package com.example.billtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.ForecastCalculator
import com.example.billtracker.domain.calculator.ForecastInputReading
import com.example.billtracker.domain.calculator.ForecastResult
import com.example.billtracker.domain.calculator.PaymentCalculator
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode

data class RecentCalculationUiModel(
    val reading: MeterReading,
    val meterName: String,
    val utilityType: UtilityType,
    val tenantName: String?
)

data class TenantBalanceUiModel(
    val tenant: Tenant,
    val electricityAmount: BigDecimal,
    val waterAmount: BigDecimal,
    val totalDue: BigDecimal
)

data class MeterForecastDashboardUiModel(
    val meterName: String,
    val utilityType: UtilityType,
    val forecastResult: ForecastResult
)

data class HomeUiState(
    val activeElectricityMeterCount: Int = 0,
    val activeWaterMeterCount: Int = 0,
    val monthElectricityTotal: BigDecimal = BigDecimal.ZERO,
    val monthWaterTotal: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val totalCollected: BigDecimal = BigDecimal.ZERO,
    val totalUnpaidBalance: BigDecimal = BigDecimal.ZERO,
    val projectedElectricityBill: BigDecimal = BigDecimal.ZERO,
    val projectedWaterBill: BigDecimal = BigDecimal.ZERO,
    val projectedGrandTotal: BigDecimal = BigDecimal.ZERO,
    val meterForecasts: List<MeterForecastDashboardUiModel> = emptyList(),
    val recentCalculations: List<RecentCalculationUiModel> = emptyList(),
    val tenantBalances: List<TenantBalanceUiModel> = emptyList(),
    val currencySymbol: String = "₱",
    val isLoading: Boolean = true
)

class HomeViewModel(
    repository: BillTrackerRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allMetersFlow,
        repository.allReadingsFlow,
        repository.allTenantsFlow,
        repository.allPaymentsFlow,
        repository.currencySymbolFlow
    ) { meters, readings, tenants, payments, currencySymbol ->

        val activeElecCount = meters.count { it.utilityType == UtilityType.ELECTRICITY }
        val activeWaterCount = meters.count { it.utilityType == UtilityType.WATER }

        val metersMap = meters.associateBy { it.id }
        val tenantsMap = tenants.associateBy { it.id }

        // Electricity and Water monthly totals
        var elecTotal = BigDecimal.ZERO
        var waterTotal = BigDecimal.ZERO

        readings.forEach { reading ->
            val meter = metersMap[reading.meterId]
            val amount = try {
                BigDecimal(reading.totalAmount.replace(",", ""))
            } catch (_: Exception) {
                BigDecimal.ZERO
            }

            if (meter?.utilityType == UtilityType.ELECTRICITY) {
                elecTotal = elecTotal.add(amount)
            } else if (meter?.utilityType == UtilityType.WATER) {
                waterTotal = waterTotal.add(amount)
            }
        }

        val grandTotal = elecTotal.add(waterTotal).setScale(2, RoundingMode.HALF_UP)

        // Payments total collected
        var totalCollected = BigDecimal.ZERO
        payments.forEach { p ->
            val amt = try { BigDecimal(p.amount.replace(",", "")) } catch (_: Exception) { BigDecimal.ZERO }
            totalCollected = totalCollected.add(amt)
        }
        totalCollected = totalCollected.setScale(2, RoundingMode.HALF_UP)

        var totalUnpaid = BigDecimal.ZERO
        readings.forEach { reading ->
            val readingPayments = payments.filter { it.readingId == reading.id }
            val summary = PaymentCalculator.calculateSummary(reading.totalAmount, readingPayments, reading.dueDate)
            totalUnpaid = totalUnpaid.add(summary.remainingBalance)
        }
        totalUnpaid = totalUnpaid.setScale(2, RoundingMode.HALF_UP)

        // Forecast Calculations per meter where isForecastEnabled == true
        val meterForecastsList = mutableListOf<MeterForecastDashboardUiModel>()
        var projElecBill = BigDecimal.ZERO
        var projWaterBill = BigDecimal.ZERO

        meters.filter { it.isForecastEnabled }.forEach { meter ->
            val meterReadings = readings.filter { it.meterId == meter.id }
            if (meterReadings.size >= 2) {
                val forecastInputs = meterReadings.map { r ->
                    ForecastInputReading(
                        timestamp = r.billingDate,
                        value = r.currentReading,
                        hasTime = r.hasTime
                    )
                }
                val result = ForecastCalculator.calculateForecast(
                    readings = forecastInputs,
                    utilityType = meter.utilityType,
                    rateStr = meter.fixedRate,
                    totalBillingDays = 30
                )

                if (result is ForecastResult.Success) {
                    val pBill = result.projectedBill ?: BigDecimal.ZERO
                    if (meter.utilityType == UtilityType.ELECTRICITY) {
                        projElecBill = projElecBill.add(pBill)
                    } else {
                        projWaterBill = projWaterBill.add(pBill)
                    }
                    meterForecastsList.add(MeterForecastDashboardUiModel(meter.name, meter.utilityType, result))
                }
            }
        }

        val projGrandTotal = projElecBill.add(projWaterBill).setScale(2, RoundingMode.HALF_UP)

        // Recent 5 calculations
        val recentCalculations = readings.take(5).map { reading ->
            val meter = metersMap[reading.meterId]
            val tenant = reading.tenantId?.let { tenantsMap[it] }
            RecentCalculationUiModel(
                reading = reading,
                meterName = meter?.name ?: "Unknown Meter",
                utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY,
                tenantName = tenant?.name
            )
        }

        // Tenant balances
        val tenantBalances = tenants.map { tenant ->
            val tenantReadings = readings.filter { it.tenantId == tenant.id }
            val latestElecReading = tenantReadings
                .filter { metersMap[it.meterId]?.utilityType == UtilityType.ELECTRICITY }
                .maxByOrNull { it.billingDate }
            val latestWaterReading = tenantReadings
                .filter { metersMap[it.meterId]?.utilityType == UtilityType.WATER }
                .maxByOrNull { it.billingDate }

            val elecAmt = latestElecReading?.let { r ->
                val rPayments = payments.filter { it.readingId == r.id }
                PaymentCalculator.calculateSummary(r.totalAmount, rPayments, r.dueDate).remainingBalance
            } ?: BigDecimal.ZERO

            val waterAmt = latestWaterReading?.let { r ->
                val rPayments = payments.filter { it.readingId == r.id }
                PaymentCalculator.calculateSummary(r.totalAmount, rPayments, r.dueDate).remainingBalance
            } ?: BigDecimal.ZERO

            val total = elecAmt.add(waterAmt).setScale(2, RoundingMode.HALF_UP)

            TenantBalanceUiModel(
                tenant = tenant,
                electricityAmount = elecAmt,
                waterAmount = waterAmt,
                totalDue = total
            )
        }

        HomeUiState(
            activeElectricityMeterCount = activeElecCount,
            activeWaterMeterCount = activeWaterCount,
            monthElectricityTotal = elecTotal.setScale(2, RoundingMode.HALF_UP),
            monthWaterTotal = waterTotal.setScale(2, RoundingMode.HALF_UP),
            grandTotal = grandTotal,
            totalCollected = totalCollected,
            totalUnpaidBalance = totalUnpaid,
            projectedElectricityBill = projElecBill.setScale(2, RoundingMode.HALF_UP),
            projectedWaterBill = projWaterBill.setScale(2, RoundingMode.HALF_UP),
            projectedGrandTotal = projGrandTotal,
            meterForecasts = meterForecastsList,
            recentCalculations = recentCalculations,
            tenantBalances = tenantBalances,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
