package com.driveflow.driver.ui.screens.subscribe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.LicenseStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscribeUiState(
    val licenseStatus: LicenseStatus = LicenseStatus.TRIAL,
    val planDaysLeft: Int = 7,
    val priceText: String = "الاشتراك الشهري: 150 ج.م فقط",
    val supportWhatsapp: String = "+201000000000",
    val deviceId: String = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    val licenseKey: String = "",
    val affiliateCode: String = "",
    val isActivating: Boolean = false,
    val activationSuccess: Boolean = false,
    val errorCode: String? = null
)

class SubscribeViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _uiState = MutableStateFlow(SubscribeUiState())
    val uiState: StateFlow<SubscribeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val installId = settingsStore.getOrCreateInstallId()
            _uiState.value = _uiState.value.copy(
                deviceId = installId.replace("-", "").take(32)
            )
        }
    }

    fun setLicenseKey(input: String) {
        val formatted = formatLicenseKey(input)
        _uiState.value = _uiState.value.copy(licenseKey = formatted, errorCode = null)
    }

    fun setAffiliateCode(code: String) {
        _uiState.value = _uiState.value.copy(affiliateCode = code.uppercase(), errorCode = null)
    }

    private fun formatLicenseKey(raw: String): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val clean = raw.uppercase().filter { it in allowedChars }
        val prefix = if (clean.startsWith("DF")) clean else "DF$clean"
        val trimmed = prefix.take(14)
        val sb = StringBuilder()
        for (i in trimmed.indices) {
            sb.append(trimmed[i])
            if ((i == 1 || i == 5 || i == 9) && i != trimmed.lastIndex) {
                sb.append("-")
            }
        }
        return sb.toString()
    }

    fun activateLicense() {
        val current = _uiState.value
        val clean = current.licenseKey.replace("-", "")
        if (clean.length < 14) {
            _uiState.value = current.copy(errorCode = "INVALID_KEY")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isActivating = true, errorCode = null)
            kotlinx.coroutines.delay(1200)
            _uiState.value = current.copy(
                isActivating = false,
                licenseStatus = LicenseStatus.ACTIVE,
                planDaysLeft = 30,
                activationSuccess = true,
                errorCode = null
            )
        }
    }
}
