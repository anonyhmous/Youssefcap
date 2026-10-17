package com.driveflow.driver.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.data.prefs.BarPosition
import com.driveflow.driver.data.prefs.MarkerSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val markerSize: MarkerSize = MarkerSize.NORMAL,
    val barOpacityPercent: Int = 67,
    val barPosition: BarPosition = BarPosition(isAuto = true),
    val autoOffHours: Int = 3,
    val language: String = "ar"
)

class SettingsViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.markerSizeFlow,
        settingsStore.barOpacityPercentFlow,
        settingsStore.barPositionFlow,
        settingsStore.autoOffHoursFlow,
        settingsStore.languageFlow
    ) { markerSize, opacity, barPos, autoOff, lang ->
        SettingsUiState(
            markerSize = markerSize,
            barOpacityPercent = opacity,
            barPosition = barPos,
            autoOffHours = autoOff,
            language = lang
        )
    }.let { flow ->
        val state = MutableStateFlow(SettingsUiState())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    fun setMarkerSize(size: MarkerSize) {
        viewModelScope.launch { settingsStore.setMarkerSize(size) }
    }

    fun setBarOpacityPercent(opacity: Int) {
        viewModelScope.launch { settingsStore.setBarOpacityPercent(opacity) }
    }

    fun setBarPositionAuto(isAuto: Boolean) {
        viewModelScope.launch {
            settingsStore.setBarPosition(BarPosition(isAuto = isAuto, x = 0, y = 0))
        }
    }

    fun setAutoOffHours(hours: Int) {
        viewModelScope.launch { settingsStore.setAutoOffHours(hours) }
    }

    fun toggleLanguage() {
        val next = if (uiState.value.language == "ar") "en" else "ar"
        viewModelScope.launch { settingsStore.setLanguage(next) }
    }
}
