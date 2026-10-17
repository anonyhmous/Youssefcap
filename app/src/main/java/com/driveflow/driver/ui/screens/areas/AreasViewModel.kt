package com.driveflow.driver.ui.screens.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.ui.components.BUNDLED_DISTRICTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AreasUiState(
    val avoidedAreas: List<String> = emptyList(),
    val preferredAreas: List<String> = emptyList(),
    val governorate: String? = null,
    val suggestions: List<String> = emptyList()
)

class AreasViewModel : ViewModel() {
    private val settingsStore = AppGraph.settingsStore

    private val _uiState = MutableStateFlow(AreasUiState())
    val uiState: StateFlow<AreasUiState> = _uiState.asStateFlow()

    init {
        loadAreas()
    }

    private fun loadAreas() {
        viewModelScope.launch {
            val policy = settingsStore.policyFlow.first()
            val gov = policy.governorate
            val districts = if (gov != null) BUNDLED_DISTRICTS[gov] ?: emptyList() else emptyList()
            _uiState.value = AreasUiState(
                avoidedAreas = policy.avoidedAreas,
                preferredAreas = policy.preferredAreas,
                governorate = gov,
                suggestions = districts
            )
        }
    }

    fun addArea(area: String, isAvoided: Boolean) {
        val trimmed = area.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val policy = settingsStore.policyFlow.first()
            val updatedPolicy = if (isAvoided) {
                if (policy.avoidedAreas.contains(trimmed)) return@launch
                policy.copy(
                    avoidedAreas = policy.avoidedAreas + trimmed,
                    preferredAreas = policy.preferredAreas - trimmed
                )
            } else {
                if (policy.preferredAreas.contains(trimmed)) return@launch
                policy.copy(
                    preferredAreas = policy.preferredAreas + trimmed,
                    avoidedAreas = policy.avoidedAreas - trimmed
                )
            }
            settingsStore.setPolicy(updatedPolicy)
            _uiState.value = _uiState.value.copy(
                avoidedAreas = updatedPolicy.avoidedAreas,
                preferredAreas = updatedPolicy.preferredAreas
            )
        }
    }

    fun removeArea(area: String, isAvoided: Boolean) {
        viewModelScope.launch {
            val policy = settingsStore.policyFlow.first()
            val updatedPolicy = if (isAvoided) {
                policy.copy(avoidedAreas = policy.avoidedAreas - area)
            } else {
                policy.copy(preferredAreas = policy.preferredAreas - area)
            }
            settingsStore.setPolicy(updatedPolicy)
            _uiState.value = _uiState.value.copy(
                avoidedAreas = updatedPolicy.avoidedAreas,
                preferredAreas = updatedPolicy.preferredAreas
            )
        }
    }

    fun moveArea(area: String, fromAvoided: Boolean) {
        viewModelScope.launch {
            val policy = settingsStore.policyFlow.first()
            val updatedPolicy = if (fromAvoided) {
                policy.copy(
                    avoidedAreas = policy.avoidedAreas - area,
                    preferredAreas = policy.preferredAreas + area
                )
            } else {
                policy.copy(
                    preferredAreas = policy.preferredAreas - area,
                    avoidedAreas = policy.avoidedAreas + area
                )
            }
            settingsStore.setPolicy(updatedPolicy)
            _uiState.value = _uiState.value.copy(
                avoidedAreas = updatedPolicy.avoidedAreas,
                preferredAreas = updatedPolicy.preferredAreas
            )
        }
    }
}
