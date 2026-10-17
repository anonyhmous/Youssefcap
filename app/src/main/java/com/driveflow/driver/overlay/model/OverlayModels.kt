package com.driveflow.driver.overlay.model

import android.graphics.Rect
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict

/**
 * Window type active for the overlay system.
 */
enum class OverlayWindowType(val code: Int) {
    ACCESSIBILITY_OVERLAY(2032), // WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    APPLICATION_OVERLAY(2038),   // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    NONE(0)
}

/**
 * Visual mode of the floating control bar.
 */
enum class BarMode {
    READING,
    WAITING_DETAILS,
    VERDICT,
    LICENSE_PROBLEM,
    READER_PAUSED,
    UPDATE_AVAILABLE,
    HIDDEN
}

/**
 * State of a single card mark on screen.
 */
data class MarkerItem(
    val requestKey: String,
    val bounds: Rect,
    val tone: Tone,
    val isOpenForRoute: Boolean,
    var colorHex: Int,
    var targetColorHex: Int = colorHex,
    var animProgress: Float = 1.0f
)

/**
 * State backing the floating control bar.
 */
data class FloatingBarState(
    val mode: BarMode = BarMode.READING,
    val request: ParsedRequest? = null,
    val verdict: Verdict? = null,
    val isCollapsed: Boolean = false,
    val opacityPercent: Int = 67,
    val isAutoPosition: Boolean = true,
    val positionX: Int = 0,
    val positionY: Int = 0,
    val statusMessage: String? = null
) {
    val alpha: Float
        get() = (opacityPercent.coerceIn(20, 100) / 100f)
}
