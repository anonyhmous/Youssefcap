package com.driveflow.driver.core.di

import android.content.Context
import com.driveflow.driver.data.prefs.SettingsStore
import com.driveflow.driver.ocr.AccessibilityScreenshotSource
import com.driveflow.driver.ocr.OcrConfigRepository
import com.driveflow.driver.ocr.OcrReadCoordinator
import com.driveflow.driver.ocr.ScreenshotProvider
import com.driveflow.driver.ocr.engine.CombinedOcrEngine
import com.driveflow.driver.ocr.indrive.InDriveTripDistanceOcr
import com.driveflow.driver.ocr.model.OcrEngine
import com.driveflow.driver.reader.didi.DiDiReader
import com.driveflow.driver.reader.indrive.InDriveReader
import com.driveflow.driver.reader.uber.UberReader
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

interface Clock {
    fun currentTimeMillis(): Long
    fun currentIso8601(): String
}

class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun currentIso8601(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}

interface ServerApi

object AppGraph {
    private var appContext: Context? = null
    private var _settingsStore: SettingsStore? = null

    val clock: Clock = SystemClock()

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val serverApi: ServerApi by lazy {
        object : ServerApi {}
    }

    val ocrConfigRepository: OcrConfigRepository by lazy {
        OcrConfigRepository(context, settingsStore)
    }

    var screenshotProvider: ScreenshotProvider = AccessibilityScreenshotSource(
        serviceProvider = { null }
    )

    private var _customOcrEngine: OcrEngine? = null
    var ocrEngine: OcrEngine
        get() = _customOcrEngine ?: _defaultOcrEngine
        set(value) { _customOcrEngine = value }

    private val _defaultOcrEngine: OcrEngine by lazy {
        CombinedOcrEngine(context)
    }

    val ocrReadCoordinator: OcrReadCoordinator by lazy {
        OcrReadCoordinator(
            screenshotProvider = screenshotProvider,
            ocrEngine = ocrEngine,
            configRepository = ocrConfigRepository
        )
    }

    val inDriveReader: InDriveReader by lazy {
        InDriveReader()
    }

    val uberReader: UberReader by lazy {
        UberReader(coordinator = ocrReadCoordinator)
    }

    val diDiReader: DiDiReader by lazy {
        DiDiReader(coordinator = ocrReadCoordinator)
    }

    val inDriveTripDistanceOcr: InDriveTripDistanceOcr by lazy {
        InDriveTripDistanceOcr(
            screenshotProvider = screenshotProvider,
            ocrEngine = ocrEngine,
            configProvider = { ocrConfigRepository.loadBundledDefault(com.driveflow.driver.core.model.RideApp.INDRIVE) }
        )
    }

    fun init(context: Context) {
        if (appContext == null) {
            val app = context.applicationContext
            appContext = app
            _settingsStore = SettingsStore(app)
        }
    }

    val context: Context
        get() = appContext ?: error("AppGraph.init(context) must be called first")

    val settingsStore: SettingsStore
        get() = _settingsStore ?: error("AppGraph.init(context) must be called before accessing settingsStore")
}
