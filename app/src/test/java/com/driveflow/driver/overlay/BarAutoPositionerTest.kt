package com.driveflow.driver.overlay

import android.graphics.Rect
import com.driveflow.driver.overlay.placement.BarAutoPositioner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class BarAutoPositionerTest {

    private val screenWidth = 1080
    private val screenHeight = 2340
    private val barWidth = 600
    private val barHeight = 150
    private val statusBarHeight = 80

    @Test
    fun calculateAutoPosition_withMapBounds_centersOnMapAndAvoidsBottom() {
        // Detected map from top=200 to bottom=1400, width=screenWidth
        val mapBounds = Rect(0, 200, 1080, 1400)
        val (x, y) = BarAutoPositioner.calculateAutoPosition(
            mapBounds = mapBounds,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            barWidth = barWidth,
            barHeight = barHeight,
            statusBarHeight = statusBarHeight
        )

        // Centered horizontally: (1080 - 600) / 2 = 240
        assertEquals(240, x)

        // Y positioned at mapBounds.top + 24 = 224
        assertEquals(224, y)

        // Must avoid bottom 32% (screenHeight * 0.32 = 748, max allowed top = 2340 - 748 - 150 = 1442)
        assertTrue(y + barHeight <= (screenHeight - (screenHeight * 0.32f)))
    }

    @Test
    fun calculateAutoPosition_withoutMapBounds_defaultsToTopCenter() {
        val (x, y) = BarAutoPositioner.calculateAutoPosition(
            mapBounds = null,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            barWidth = barWidth,
            barHeight = barHeight,
            statusBarHeight = statusBarHeight
        )

        // Centered horizontally: (1080 - 600) / 2 = 240
        assertEquals(240, x)

        // Below status bar: statusBarHeight + 16 + 16 = 112
        assertEquals(112, y)
    }

    @Test
    fun calculateAutoPosition_withEmptyMapBounds_defaultsToTopCenter() {
        val emptyBounds = Rect(0, 0, 0, 0)
        val (x, y) = BarAutoPositioner.calculateAutoPosition(
            mapBounds = emptyBounds,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            barWidth = barWidth,
            barHeight = barHeight,
            statusBarHeight = statusBarHeight
        )

        assertEquals(240, x)
        assertEquals(112, y)
    }

    @Test
    fun clampManualPosition_clampsWithinScreenMargins() {
        // Dragged way off-screen left/top (-200, -50)
        val (clampedX1, clampedY1) = BarAutoPositioner.clampManualPosition(
            x = -200,
            y = -50,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            barWidth = barWidth,
            barHeight = barHeight,
            statusBarHeight = statusBarHeight
        )
        assertEquals(8, clampedX1)
        assertEquals(statusBarHeight + 8, clampedY1)

        // Dragged way off-screen right/bottom (2000, 3000)
        val (clampedX2, clampedY2) = BarAutoPositioner.clampManualPosition(
            x = 2000,
            y = 3000,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            barWidth = barWidth,
            barHeight = barHeight,
            statusBarHeight = statusBarHeight
        )
        assertEquals(screenWidth - barWidth - 8, clampedX2)
        assertEquals(screenHeight - barHeight - 16, clampedY2)
    }
}
