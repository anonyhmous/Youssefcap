package com.driveflow.driver.ocr.indrive

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.TripDistanceOcr
import com.driveflow.driver.core.model.TripKmResult
import com.driveflow.driver.ocr.ScreenshotProvider
import com.driveflow.driver.ocr.ScreenshotResult
import com.driveflow.driver.ocr.engine.ImagePreProcessor
import com.driveflow.driver.ocr.model.AppOcrConfig
import com.driveflow.driver.ocr.model.ChipConfig
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.OcrEngine
import com.driveflow.driver.reader.parser.ArabicTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class InDriveTripDistanceOcr(
    private val screenshotProvider: ScreenshotProvider,
    private val ocrEngine: OcrEngine,
    private val configProvider: () -> AppOcrConfig,
    private val hideOverlayForCapture: (() -> Unit)? = null,
    private val restoreOverlayAfterCapture: (() -> Unit)? = null
) : TripDistanceOcr {

    private val resultCache = ConcurrentHashMap<String, TripKmResult>()

    override suspend fun readTripKm(mapBounds: Rect): TripKmResult = withContext(Dispatchers.Default) {
        if (mapBounds.width() < 100 || mapBounds.height() < 100) {
            return@withContext TripKmResult(null, "trip_box_missing")
        }

        hideOverlayForCapture?.invoke()
        val screenshotResult = try {
            screenshotProvider.capture()
        } finally {
            restoreOverlayAfterCapture?.invoke()
        }

        val screenshot = when (screenshotResult) {
            is ScreenshotResult.Success -> screenshotResult.bitmap
            is ScreenshotResult.Failure -> {
                return@withContext TripKmResult(null, if (screenshotResult.code == "capture_refused") "capture_refused" else "trip_box_missing")
            }
        }

        try {
            val mapBitmap = ImagePreProcessor.crop(screenshot, mapBounds)
            try {
                val config = configProvider()
                val tripChipConfig = config.tripChip ?: ChipConfig(
                    targetHexColor = "#C5F01A",
                    colorTolerance = 0.18f,
                    minAreaPx = 2000,
                    maxAreaPx = 75000,
                    minAspectRatio = 1.2f,
                    maxAspectRatio = 5.0f
                )

                val chipRect = findColorBlob(mapBitmap, tripChipConfig)
                    ?: return@withContext TripKmResult(null, "trip_box_missing")

                if (chipRect.left <= 4 || chipRect.top <= 4 ||
                    chipRect.right >= mapBitmap.width - 4 ||
                    chipRect.bottom >= mapBitmap.height - 4
                ) {
                    return@withContext TripKmResult(null, "trip_km_clipped")
                }

                if (chipRect.top < mapBitmap.height * 0.10f) {
                    return@withContext TripKmResult(null, "trip_km_covered")
                }

                val chipBitmap = ImagePreProcessor.crop(mapBitmap, chipRect)
                try {
                    val ocrResult = ocrEngine.read(
                        bitmap = chipBitmap,
                        crop = RectF(0f, 0f, 1f, 1f),
                        hint = Hint.DISTANCE
                    )
                    val cleanedText = ArabicTextParser.cleanOcrText(ocrResult.text)
                    val km = ArabicTextParser.parseDistanceKm(cleanedText)

                    return@withContext if (km != null && km in 0.2..300.0) {
                        TripKmResult(km, null)
                    } else {
                        TripKmResult(null, "digits_unclear")
                    }
                } finally {
                    chipBitmap.recycle()
                }
            } finally {
                mapBitmap.recycle()
            }
        } finally {
            screenshot.recycle()
        }
    }

    fun findColorBlob(bitmap: Bitmap, config: ChipConfig): Rect? {
        val targetColor = try {
            Color.parseColor(config.targetHexColor)
        } catch (_: Exception) {
            Color.rgb(197, 240, 26)
        }
        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)
        val maxDist = config.colorTolerance * 441.67f

        val w = bitmap.width
        val h = bitmap.height
        val stride = 2
        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        var matchCount = 0

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h step stride) {
            val rowOffset = y * w
            for (x in 0 until w step stride) {
                val pixel = pixels[rowOffset + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val dr = r - targetR
                val dg = g - targetG
                val db = b - targetB
                val dist = sqrt((dr * dr + dg * dg + db * db).toFloat())

                if (dist <= maxDist) {
                    matchCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (matchCount < (config.minAreaPx / (stride * stride))) {
            return null
        }

        val blobWidth = maxX - minX
        val blobHeight = maxY - minY
        val area = blobWidth * blobHeight
        if (area < config.minAreaPx || area > config.maxAreaPx) {
            return null
        }

        val aspectRatio = if (blobHeight > 0) blobWidth.toFloat() / blobHeight else 0f
        if (aspectRatio < config.minAspectRatio || aspectRatio > config.maxAspectRatio) {
            return null
        }

        return Rect(minX, minY, maxX, maxY)
    }

    fun clearCache() {
        resultCache.clear()
    }
}
