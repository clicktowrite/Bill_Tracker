package com.example.billtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.calculator.PaymentCalculator
import com.example.billtracker.domain.calculator.PaymentSummary
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Payment
import com.example.billtracker.domain.model.PaymentStatus
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class BillDetailUiState(
    val reading: MeterReading? = null,
    val meterName: String = "",
    val utilityType: UtilityType = UtilityType.ELECTRICITY,
    val tenant: Tenant? = null,
    val payments: List<Payment> = emptyList(),
    val paymentSummary: PaymentSummary = PaymentSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, PaymentStatus.UNPAID),
    val currencySymbol: String = "₱",
    val isDeleted: Boolean = false,
    val isLoading: Boolean = true
)

class BillDetailViewModel(
    private val repository: BillTrackerRepository,
    private val readingId: Long
) : ViewModel() {

    val uiState: StateFlow<BillDetailUiState> = combine(
        repository.allReadingsFlow,
        repository.allMetersFlow,
        repository.allTenantsFlow,
        repository.getPaymentsForReading(readingId),
        repository.currencySymbolFlow
    ) { readings, meters, tenants, payments, currencySymbol ->

        val reading = readings.find { it.id == readingId }
        val meter = reading?.let { r -> meters.find { it.id == r.meterId } }
        val tenant = reading?.tenantId?.let { tid -> tenants.find { it.id == tid } }

        val summary = PaymentCalculator.calculateSummary(
            totalBillAmountStr = reading?.totalAmount,
            payments = payments,
            dueDate = reading?.dueDate
        )

        BillDetailUiState(
            reading = reading,
            meterName = meter?.name ?: "Meter",
            utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY,
            tenant = tenant,
            payments = payments,
            paymentSummary = summary,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillDetailUiState()
    )

    fun recordPayment(payment: Payment) {
        viewModelScope.launch {
            repository.savePayment(payment)
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    fun deleteReading() {
        viewModelScope.launch {
            repository.deleteReading(readingId)
        }
    }
}
