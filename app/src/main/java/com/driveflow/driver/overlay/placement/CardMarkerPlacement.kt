package com.driveflow.driver.overlay.placement

import android.graphics.Rect
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.data.prefs.MarkerSize

/**
 * Pure Kotlin placement and dimension calculations for card markers drawn over ride apps.
 */
object CardMarkerPlacement {

    const val COLOR_GREEN = 0xFF22C55E.toInt()
    const val COLOR_ORANGE = 0xFFF59E0B.toInt()
    const val COLOR_RED = 0xFFEF4444.toInt()
    const val COLOR_GRAY = 0xFF64748B.toInt()
    const val COLOR_ROUTE_GREEN = 0xFF4ADE80.toInt() // Brighter green for OPEN_FOR_ROUTE

    fun getMarkerSizeDp(size: MarkerSize): Int = when (size) {
        MarkerSize.SMALL -> 20
        MarkerSize.NORMAL -> 28
        MarkerSize.LARGE -> 38
    }

    fun getMarkerSizePx(size: MarkerSize, density: Float): Int {
        val dp = getMarkerSizeDp(size)
        return (dp * density + 0.5f).toInt().coerceAtLeast(1)
    }

    fun getColorForTone(tone: Tone, isOpenForRoute: Boolean = false): Int {
        return if (isOpenForRoute && tone == Tone.GREEN) {
            COLOR_ROUTE_GREEN
        } else {
            when (tone) {
                Tone.GREEN -> COLOR_GREEN
                Tone.ORANGE -> COLOR_ORANGE
                Tone.RED -> COLOR_RED
                Tone.GRAY -> COLOR_GRAY
            }
        }
    }

    /**
     * Calculates the bounding rectangle of the marker beside the parsed card.
     *
     * @param cardBounds The screen-space bounds of the parsed ride request card.
     * @param markerSizePx Diameter/side of the marker in pixels.
     * @param isRtl True if the UI/locale is RTL (Arabic). In RTL, end edge is the left edge.
     * @param screenWidth Total width of the screen.
     * @param screenHeight Total height of the screen.
     * @param marginPx Spacing between the card edge and the marker.
     * @return Clamped screen-space [Rect] for the marker.
     */
    fun calculateMarkerRect(
        cardBounds: Rect,
        markerSizePx: Int,
        isRtl: Boolean,
        screenWidth: Int,
        screenHeight: Int,
        marginPx: Int = 8
    ): Rect {
        val size = markerSizePx.coerceAtLeast(1)
        val centerY = cardBounds.centerY()
        val rawTop = centerY - size / 2

        val rawLeft = if (isRtl) {
            // In RTL, end edge is on the left
            val candidateLeft = cardBounds.left - size - marginPx
            if (candidateLeft >= 0) {
                candidateLeft
            } else {
                // If not enough space on left, place on right edge or inside card edge
                (cardBounds.right + marginPx).takeIf { it + size <= screenWidth }
                    ?: (cardBounds.left + marginPx)
            }
        } else {
            // In LTR, end edge is on the right
            val candidateLeft = cardBounds.right + marginPx
            if (candidateLeft + size <= screenWidth) {
                candidateLeft
            } else {
                // If not enough space on right, place on left edge or inside card edge
                (cardBounds.left - size - marginPx).takeIf { it >= 0 }
                    ?: (cardBounds.right - size - marginPx)
            }
        }

        // Clamp inside screen bounds
        val maxLeft = (screenWidth - size).coerceAtLeast(0)
        val maxTop = (screenHeight - size).coerceAtLeast(0)

        val clampedLeft = rawLeft.coerceIn(0, maxLeft)
        val clampedTop = rawTop.coerceIn(0, maxTop)

        return Rect(clampedLeft, clampedTop, clampedLeft + size, clampedTop + size)
    }

    /**
     * Maps opacity percentage (20..100) to float alpha (0.20f..1.0f).
     */
    fun mapOpacityToAlpha(opacityPercent: Int): Float {
        val clamped = opacityPercent.coerceIn(20, 100)
        return clamped / 100.0f
    }
}
