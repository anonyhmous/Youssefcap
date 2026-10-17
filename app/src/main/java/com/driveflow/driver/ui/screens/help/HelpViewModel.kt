package com.driveflow.driver.ui.screens.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HelpUiState(
    val driverName: String = "",
    val driverPhone: String = "",
    val governorate: String = "",
    val deviceId: String = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    val supportWhatsapp: String = "+201000000000",
    val appVersion: String = "1.0.0"
)

class HelpViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val name = settingsStore.contactNameFlow.first() ?: ""
            val phone = settingsStore.contactPhoneFlow.first() ?: ""
            val policy = settingsStore.policyFlow.first()
            val gov = policy.governorate ?: "cairo"
            val installId = settingsStore.getOrCreateInstallId()

            _uiState.value = HelpUiState(
                driverName = name,
                driverPhone = phone,
                governorate = gov,
                deviceId = installId.replace("-", "").take(32),
                supportWhatsapp = "+201000000000",
                appVersion = "1.0.0"
            )
        }
    }
}
