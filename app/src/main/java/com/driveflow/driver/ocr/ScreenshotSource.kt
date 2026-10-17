package com.driveflow.driver.ocr

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.SystemClock
import android.view.Display
import androidx.annotation.RequiresApi
import com.driveflow.driver.license.LicenseGate
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

interface ScreenshotProvider {
    suspend fun capture(): ScreenshotResult
}

sealed class ScreenshotResult {
    data class Success(val bitmap: Bitmap) : ScreenshotResult()
    data class Failure(val code: String) : ScreenshotResult()
}

/**
 * In-memory holder for the most recently captured screenshot bitmap.
 * Kept strictly in-memory (never logged, never saved to disk, never uploaded).
 * Used exclusively by the CalibrationScreen for measuring box fractions.
 */
object LastScreenshotHolder {
    @Volatile
    var lastBitmap: Bitmap? = null
        private set

    @Volatile
    var lastCapturedAt: Long = 0L
        private set

    fun update(bitmap: Bitmap) {
        val old = lastBitmap
        if (old != null && old != bitmap && !old.isRecycled) {
            old.recycle()
        }
        lastBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        lastCapturedAt = System.currentTimeMillis()
    }

    fun clear() {
        lastBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        lastBitmap = null
        lastCapturedAt = 0L
    }
}

/**
 * Captures screenshots from AccessibilityService.takeScreenshot (API 30+)
 * with 1000ms rate limiting and exponential backoff on secure windows.
 */
class AccessibilityScreenshotSource(
    private val serviceProvider: () -> AccessibilityService?,
    private val licenseGate: LicenseGate = LicenseGate.PERMISSIVE,
    private val isRideAppVisible: () -> Boolean = { true },
    private val isScreenReadingActive: () -> Boolean = { true }
) : ScreenshotProvider {

    private val executor = Executors.newSingleThreadExecutor()
    private var lastCaptureTimeMs: Long = 0L
    private var backoffUntilMs: Long = 0L
    private var consecutiveRefusals: Int = 0

    override suspend fun capture(): ScreenshotResult {
        val now = SystemClock.elapsedRealtime()
        if (now < backoffUntilMs) {
            return ScreenshotResult.Failure("capture_refused")
        }
        if (now - lastCaptureTimeMs < MIN_INTERVAL_MS) {
            return ScreenshotResult.Failure("rate_limited")
        }
        if (!licenseGate.canRead()) {
            return ScreenshotResult.Failure("license_disallowed")
        }
        if (!isRideAppVisible() || !isScreenReadingActive()) {
            return ScreenshotResult.Failure("not_active")
        }

        val service = serviceProvider() ?: return ScreenshotResult.Failure("service_null")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ScreenshotResult.Failure("unsupported_api")
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                service.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    executor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                            lastCaptureTimeMs = SystemClock.elapsedRealtime()
                            consecutiveRefusals = 0
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val softwareBitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                            } catch (e: Exception) {
                                null
                            } finally {
                                hardwareBuffer.close()
                            }

                            if (softwareBitmap != null) {
                                LastScreenshotHolder.update(softwareBitmap)
                                if (continuation.isActive) {
                                    continuation.resume(ScreenshotResult.Success(softwareBitmap))
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(ScreenshotResult.Failure("buffer_conversion_failed"))
                                }
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            lastCaptureTimeMs = SystemClock.elapsedRealtime()
                            val failureCode = if (errorCode == AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW) {
                                consecutiveRefusals++
                                val backoffDurationMs = if (consecutiveRefusals > 1) 30_000L else 5_000L
                                backoffUntilMs = lastCaptureTimeMs + backoffDurationMs
                                "capture_refused"
                            } else {
                                "capture_failed_$errorCode"
                            }
                            if (continuation.isActive) {
                                continuation.resume(ScreenshotResult.Failure(failureCode))
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(ScreenshotResult.Failure("exception: ${e.message}"))
                }
            }
        }
    }

    companion object {
        const val MIN_INTERVAL_MS = 1000L
    }
}
