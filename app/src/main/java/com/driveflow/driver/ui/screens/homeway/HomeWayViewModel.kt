package com.driveflow.driver.ui.screens.homeway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.data.prefs.HomeWay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeWayFormState(
    val enabled: Boolean = false,
    val placeMode: String = "HOME",
    val locationInput: String = "",
    val label: String = "",
    val isSavedSuccess: Boolean = false
)

class HomeWayViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _formState = MutableStateFlow(HomeWayFormState())
    val formState: StateFlow<HomeWayFormState> = _formState.asStateFlow()

    init {
        loadHomeWay()
    }

    private fun loadHomeWay() {
        viewModelScope.launch {
            val homeWay = settingsStore.homeWayFlow.first()
            val coordStr = if (homeWay.lat != null && homeWay.lon != null) {
                "${homeWay.lat}, ${homeWay.lon}"
            } else ""
            _formState.value = HomeWayFormState(
                enabled = homeWay.enabled,
                placeMode = homeWay.placeMode ?: "HOME",
                locationInput = coordStr,
                label = homeWay.label ?: "",
                isSavedSuccess = false
            )
        }
    }

    fun updateForm(transform: (HomeWayFormState) -> HomeWayFormState) {
        _formState.value = transform(_formState.value).copy(isSavedSuccess = false)
    }

    fun clearLocation() {
        _formState.value = _formState.value.copy(
            locationInput = "",
            label = "",
            enabled = false
        )
        saveCurrentState()
    }

    fun saveCurrentState() {
        val state = _formState.value
        var lat: Double? = null
        var lon: Double? = null
        val parts = state.locationInput.split(",", " ")
            .filter { it.isNotBlank() }
        if (parts.size >= 2) {
            lat = parts[0].toDoubleOrNull()
            lon = parts[1].toDoubleOrNull()
        }

        val updated = HomeWay(
            enabled = state.enabled,
            lat = lat,
            lon = lon,
            label = state.label.ifBlank { null },
            placeMode = state.placeMode
        )
        viewModelScope.launch {
            settingsStore.setHomeWay(updated)
            _formState.value = state.copy(isSavedSuccess = true)
        }
    }
}
