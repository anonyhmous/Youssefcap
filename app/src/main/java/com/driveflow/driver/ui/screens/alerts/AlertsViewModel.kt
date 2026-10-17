package com.driveflow.driver.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.data.prefs.AlertMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isAlertEnabled: Boolean = true,
    val alertMode: AlertMode = AlertMode.TONE,
    val isTestingAlert: Boolean = false
)

class AlertsViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore
    private val _isTestingAlert = MutableStateFlow(false)

    val uiState: StateFlow<AlertsUiState> = combine(
        settingsStore.alertEnabledFlow,
        settingsStore.alertModeFlow,
        _isTestingAlert
    ) { enabled, mode, testing ->
        AlertsUiState(
            isAlertEnabled = enabled,
            alertMode = mode,
            isTestingAlert = testing
        )
    }.let { flow ->
        val state = MutableStateFlow(AlertsUiState())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    fun setAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAlertEnabled(enabled)
        }
    }

    fun setAlertMode(mode: AlertMode) {
        viewModelScope.launch {
            settingsStore.setAlertMode(mode)
        }
    }

    fun playTestAlert() {
        viewModelScope.launch {
            _isTestingAlert.value = true
            kotlinx.coroutines.delay(1200)
            _isTestingAlert.value = false
        }
    }
}
