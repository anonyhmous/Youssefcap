package com.driveflow.driver.ocr

import android.content.Context
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.SettingsStore
import com.driveflow.driver.ocr.model.AppOcrConfig
import com.driveflow.driver.ocr.model.ScreenCrops
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class OcrConfigRepository(
    private val context: Context,
    private val settingsStore: SettingsStore
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun getConfig(app: RideApp): AppOcrConfig {
        val localCalibJson = settingsStore.ocrCalibrationFlow(app).firstOrNull()
        if (!localCalibJson.isNullOrBlank()) {
            try {
                return json.decodeFromString<AppOcrConfig>(localCalibJson)
            } catch (_: Exception) {}
        }

        val serverConfigJson = settingsStore.readerConfigFlow(app).firstOrNull()
        if (!serverConfigJson.isNullOrBlank()) {
            try {
                return json.decodeFromString<AppOcrConfig>(serverConfigJson)
            } catch (_: Exception) {}
        }

        return loadBundledDefault(app)
    }

    fun loadBundledDefault(app: RideApp): AppOcrConfig {
        val assetFileName = when (app) {
            RideApp.INDRIVE -> "reader_default_indrive.json"
            RideApp.UBER -> "reader_default_uber.json"
            RideApp.DIDI -> "reader_default_didi.json"
        }
        return try {
            context.assets.open(assetFileName).use { input ->
                InputStreamReader(input, Charsets.UTF_8).use { reader ->
                    json.decodeFromString<AppOcrConfig>(reader.readText())
                }
            }
        } catch (_: Exception) {
            AppOcrConfig(app = app.code, enabled = app != RideApp.UBER)
        }
    }

    suspend fun saveCalibration(app: RideApp, config: AppOcrConfig) {
        val serialized = json.encodeToString(AppOcrConfig.serializer(), config)
        settingsStore.setOcrCalibration(app, serialized)
    }

    fun serializeConfig(config: AppOcrConfig): String {
        return json.encodeToString(AppOcrConfig.serializer(), config)
    }
}
