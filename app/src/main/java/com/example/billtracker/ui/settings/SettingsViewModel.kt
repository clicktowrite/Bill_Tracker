package com.example.billtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencySymbol: String = "₱",
    val recordReadingTime: Boolean = true
)

class SettingsViewModel(
    private val repository: BillTrackerRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.themeModeFlow,
        repository.currencySymbolFlow,
        repository.recordReadingTimeFlow
    ) { theme, currency, recordTime ->
        SettingsUiState(
            themeMode = theme,
            currencySymbol = currency,
            recordReadingTime = recordTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            repository.setCurrencySymbol(symbol)
        }
    }

    fun setRecordReadingTime(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRecordReadingTime(enabled)
        }
    }
}
