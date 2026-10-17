package com.driveflow.driver.overlay.placement

import android.graphics.Rect

/**
 * Pure Kotlin positioning calculator for the floating control bar.
 */
object BarAutoPositioner {

    /**
     * Calculates the automatic (x, y) coordinates for the floating control bar.
     *
     * @param mapBounds Bounding box of the ride app's map view, if detected by [AppReader].
     * @param screenWidth Total screen width in pixels.
     * @param screenHeight Total screen height in pixels.
     * @param barWidth Layout width of the control bar in pixels.
     * @param barHeight Layout height of the control bar in pixels.
     * @param statusBarHeight Estimated or actual status bar height in pixels.
     * @param bottomAvoidHeight Height of the bottom sheet/accept area to strictly avoid.
     * @return Pair of (x, y) coordinates clamped within visible screen boundaries.
     */
    fun calculateAutoPosition(
        mapBounds: Rect?,
        screenWidth: Int,
        screenHeight: Int,
        barWidth: Int,
        barHeight: Int,
        statusBarHeight: Int = 80,
        bottomAvoidHeight: Int = (screenHeight * 0.32f).toInt()
    ): Pair<Int, Int> {
        val safeTop = statusBarHeight + 16
        val safeBottom = (screenHeight - bottomAvoidHeight - barHeight).coerceAtLeast(safeTop)
        val maxX = (screenWidth - barWidth).coerceAtLeast(0)

        if (mapBounds != null && !mapBounds.isEmpty) {
            // Center horizontally within map bounds, or center of screen
            val targetX = if (mapBounds.width() >= barWidth) {
                mapBounds.centerX() - (barWidth / 2)
            } else {
                (screenWidth - barWidth) / 2
            }

            // Position at the top edge of the map, below status bar
            val targetY = (mapBounds.top + 24).coerceIn(safeTop, safeBottom)
            val clampedX = targetX.coerceIn(12, (maxX - 12).coerceAtLeast(12))

            return Pair(clampedX, targetY)
        }

        // Fallback: top-center (below status bar)
        val defaultX = (screenWidth - barWidth) / 2
        val defaultY = safeTop + 16
        return Pair(defaultX.coerceIn(0, maxX), defaultY.coerceIn(0, safeBottom))
    }

    /**
     * Clamps a manually dragged (x, y) within allowable screen margins.
     */
    fun clampManualPosition(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        barWidth: Int,
        barHeight: Int,
        statusBarHeight: Int = 80
    ): Pair<Int, Int> {
        val minX = 8
        val maxX = (screenWidth - barWidth - 8).coerceAtLeast(minX)
        val minY = statusBarHeight + 8
        val maxY = (screenHeight - barHeight - 16).coerceAtLeast(minY)

        return Pair(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
    }
}
