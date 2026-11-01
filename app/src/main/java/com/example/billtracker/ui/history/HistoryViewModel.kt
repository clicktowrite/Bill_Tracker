package com.example.billtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.UtilityType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryItemUiModel(
    val reading: MeterReading,
    val meterName: String,
    val utilityType: UtilityType,
    val tenantName: String?
)

data class HistoryUiState(
    val historyItems: List<HistoryItemUiModel> = emptyList(),
    val currencySymbol: String = "₱",
    val isLoading: Boolean = true
)

class HistoryViewModel(
    private val repository: BillTrackerRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allReadingsFlow,
        repository.allMetersFlow,
        repository.allTenantsFlow,
        repository.currencySymbolFlow
    ) { readings, meters, tenants, currencySymbol ->

        val metersMap = meters.associateBy { it.id }
        val tenantsMap = tenants.associateBy { it.id }

        val items = readings.map { reading ->
            val meter = metersMap[reading.meterId]
            val tenant = reading.tenantId?.let { tenantsMap[it] }
            HistoryItemUiModel(
                reading = reading,
                meterName = meter?.name ?: "Meter #${reading.meterId}",
                utilityType = meter?.utilityType ?: UtilityType.ELECTRICITY,
                tenantName = tenant?.name
            )
        }

        HistoryUiState(
            historyItems = items,
            currencySymbol = currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun deleteReading(readingId: Long) {
        viewModelScope.launch {
            repository.deleteReading(readingId)
        }
    }
}
