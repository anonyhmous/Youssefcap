package com.driveflow.driver.ui.screens.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.ocr.LastScreenshotHolder
import com.driveflow.driver.ocr.ScreenKind
import com.driveflow.driver.ocr.model.AppOcrConfig
import com.driveflow.driver.ocr.model.CropRectF
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.ScreenCrops
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalibrationUiState(
    val selectedApp: RideApp = RideApp.DIDI,
    val selectedKind: ScreenKind = ScreenKind.LIST_CARD,
    val selectedBox: String = "fareBox",
    val config: AppOcrConfig? = null,
    val currentCrops: ScreenCrops? = null,
    val detectedPackageName: String = "com.didiglobal.driver",
    val experimentalRoadState: Boolean = false,
    val liveOcrOutputs: Map<String, String> = emptyMap(),
    val isSavedSuccess: Boolean = false,
    val copiedJson: String? = null
)

class CalibrationViewModel : ViewModel() {
    private val configRepository = AppGraph.ocrConfigRepository
    private val ocrEngine = AppGraph.ocrEngine

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    init {
        loadConfigForApp(_uiState.value.selectedApp)
    }

    fun selectApp(app: RideApp) {
        val detectedPkg = when (app) {
            RideApp.INDRIVE -> RideApp.INDRIVE.pkg
            RideApp.UBER -> RideApp.UBER.pkg
            RideApp.DIDI -> RideApp.DIDI.pkg
        }
        _uiState.value = _uiState.value.copy(
            selectedApp = app,
            detectedPackageName = detectedPkg,
            isSavedSuccess = false,
            copiedJson = null
        )
        loadConfigForApp(app)
    }

    fun selectKind(kind: ScreenKind) {
        _uiState.value = _uiState.value.copy(selectedKind = kind, isSavedSuccess = false)
        updateCurrentCrops()
    }

    fun selectBox(boxName: String) {
        _uiState.value = _uiState.value.copy(selectedBox = boxName)
    }

    private fun loadConfigForApp(app: RideApp) {
        viewModelScope.launch {
            val cfg = configRepository.getConfig(app)
            _uiState.value = _uiState.value.copy(config = cfg)
            updateCurrentCrops()
            triggerLiveOcr()
        }
    }

    private fun updateCurrentCrops() {
        val cfg = _uiState.value.config ?: return
        val kind = _uiState.value.selectedKind
        val crops = when (kind) {
            ScreenKind.LIST_CARD -> cfg.listCropsAr ?: cfg.listCropsEn
            ScreenKind.DETAIL -> cfg.detailCropsAr ?: cfg.detailCropsEn
        }
        _uiState.value = _uiState.value.copy(currentCrops = crops)
    }

    fun updateSelectedBox(left: Float, top: Float, right: Float, bottom: Float) {
        val crops = _uiState.value.currentCrops ?: return
        val newRect = CropRectF(
            left = left.coerceIn(0f, 1f),
            top = top.coerceIn(0f, 1f),
            right = right.coerceIn(left, 1f),
            bottom = bottom.coerceIn(top, 1f)
        )
        val updatedCrops = when (_uiState.value.selectedBox) {
            "fareBox" -> crops.copy(fareBox = newRect, status = "CALIBRATED")
            "pickupBox" -> crops.copy(pickupBox = newRect, status = "CALIBRATED")
            "tripBox" -> crops.copy(tripBox = newRect, status = "CALIBRATED")
            "ratingBox" -> crops.copy(ratingBox = newRect, status = "CALIBRATED")
            "productBox" -> crops.copy(productBox = newRect, status = "CALIBRATED")
            "cardAnchor" -> crops.copy(cardAnchor = newRect, status = "CALIBRATED")
            else -> crops
        }

        val cfg = _uiState.value.config ?: return
        val updatedConfig = if (_uiState.value.selectedKind == ScreenKind.LIST_CARD) {
            cfg.copy(listCropsAr = updatedCrops, enabled = true)
        } else {
            cfg.copy(detailCropsAr = updatedCrops, enabled = true)
        }
        _uiState.value = _uiState.value.copy(
            config = updatedConfig,
            currentCrops = updatedCrops,
            isSavedSuccess = false
        )
        triggerLiveOcr()
    }

    fun triggerLiveOcr() {
        val bitmap = LastScreenshotHolder.lastBitmap ?: return
        val crops = _uiState.value.currentCrops ?: return
        viewModelScope.launch {
            val outputs = mutableMapOf<String, String>()
            val fareText = ocrEngine.read(bitmap, crops.fareBox.toRectF(), Hint.FARE).text
            outputs["fareBox"] = fareText.ifBlank { "(no text detected)" }
            val pickupText = ocrEngine.read(bitmap, crops.pickupBox.toRectF(), Hint.DISTANCE).text
            outputs["pickupBox"] = pickupText.ifBlank { "(no text detected)" }
            val tripText = ocrEngine.read(bitmap, crops.tripBox.toRectF(), Hint.DISTANCE).text
            outputs["tripBox"] = tripText.ifBlank { "(no text detected)" }
            crops.ratingBox?.let {
                val ratingText = ocrEngine.read(bitmap, it.toRectF(), Hint.TEXT).text
                outputs["ratingBox"] = ratingText.ifBlank { "(no text detected)" }
            }
            crops.productBox?.let {
                val productText = ocrEngine.read(bitmap, it.toRectF(), Hint.TEXT).text
                outputs["productBox"] = productText.ifBlank { "(no text detected)" }
            }
            _uiState.value = _uiState.value.copy(liveOcrOutputs = outputs)
        }
    }

    fun toggleExperimentalRoadState(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(experimentalRoadState = enabled)
        AppGraph.ocrReadCoordinator.experimentalRoadStateEnabled = enabled
    }

    fun saveCalibration() {
        val cfg = _uiState.value.config ?: return
        viewModelScope.launch {
            configRepository.saveCalibration(_uiState.value.selectedApp, cfg)
            _uiState.value = _uiState.value.copy(isSavedSuccess = true)
        }
    }

    fun copyConfigJson(): String {
        val cfg = _uiState.value.config ?: return "{}"
        val json = configRepository.serializeConfig(cfg)
        _uiState.value = _uiState.value.copy(copiedJson = json)
        return json
    }
}
