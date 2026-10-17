package com.driveflow.driver.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.data.prefs.HomeWay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isReaderRunning: Boolean = false,
    val policy: Policy = Policy(),
    val isAlertEnabled: Boolean = true,
    val alertMode: AlertMode = AlertMode.TONE,
    val homeWay: HomeWay = HomeWay(),
    val licenseStatus: LicenseStatus = LicenseStatus.TRIAL,
    val daysLeft: Int = 7,
    val isAccessibilityGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isRulesConfigured: Boolean = false
) {
    val remainingSteps: Int
        get() = (if (!isAccessibilityGranted) 1 else 0) +
                (if (!isOverlayGranted) 1 else 0) +
                (if (!isRulesConfigured) 1 else 0)

    val isSetupComplete: Boolean
        get() = remainingSteps == 0
}

class HomeViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _isReaderRunning = MutableStateFlow(false)
    val isReaderRunning = _isReaderRunning.asStateFlow()

    private val _isAccessibilityGranted = MutableStateFlow(false)
    private val _isOverlayGranted = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _isReaderRunning,
        settingsStore.policyFlow,
        settingsStore.alertEnabledFlow,
        settingsStore.alertModeFlow,
        settingsStore.homeWayFlow
    ) { running, policy, alertEnabled, alertMode, homeWay ->
        val rulesConfigured = policy.minPricePerKm > 0.0
        HomeUiState(
            isReaderRunning = running,
            policy = policy,
            isAlertEnabled = alertEnabled,
            alertMode = alertMode,
            homeWay = homeWay,
            licenseStatus = LicenseStatus.TRIAL,
            daysLeft = 7,
            isAccessibilityGranted = _isAccessibilityGranted.value,
            isOverlayGranted = _isOverlayGranted.value,
            isRulesConfigured = rulesConfigured
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleReader(enable: Boolean) {
        _isReaderRunning.value = enable
    }

    fun toggleHomeWay(enable: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.homeWay
            settingsStore.setHomeWay(current.copy(enabled = enable))
        }
    }

    fun toggleApp(app: RideApp, enabled: Boolean) {
        viewModelScope.launch {
            val currentPolicy = uiState.value.policy
            val updatedMap = currentPolicy.appEnabled.toMutableMap()
            updatedMap[app] = enabled
            settingsStore.setPolicy(currentPolicy.copy(appEnabled = updatedMap))
        }
    }

    fun markStepAccessibilityGranted() {
        _isAccessibilityGranted.value = true
    }

    fun markStepOverlayGranted() {
        _isOverlayGranted.value = true
    }
}
