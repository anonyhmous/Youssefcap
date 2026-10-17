package com.driveflow.driver.overlay

import android.graphics.Rect
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.overlay.placement.CardMarkerPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CardMarkerPlacementTest {

    private val screenWidth = 1080
    private val screenHeight = 2340

    @Test
    fun getMarkerSizeDp_returnsCorrectValues() {
        assertEquals(20, CardMarkerPlacement.getMarkerSizeDp(MarkerSize.SMALL))
        assertEquals(28, CardMarkerPlacement.getMarkerSizeDp(MarkerSize.NORMAL))
        assertEquals(38, CardMarkerPlacement.getMarkerSizeDp(MarkerSize.LARGE))
    }

    @Test
    fun getMarkerSizePx_scalesWithDensity() {
        // Density = 2.0f
        assertEquals(40, CardMarkerPlacement.getMarkerSizePx(MarkerSize.SMALL, 2.0f))
        assertEquals(56, CardMarkerPlacement.getMarkerSizePx(MarkerSize.NORMAL, 2.0f))
        assertEquals(76, CardMarkerPlacement.getMarkerSizePx(MarkerSize.LARGE, 2.0f))

        // Density = 3.0f (xxhdpi like reference Samsung SM-A175F)
        assertEquals(60, CardMarkerPlacement.getMarkerSizePx(MarkerSize.SMALL, 3.0f))
        assertEquals(84, CardMarkerPlacement.getMarkerSizePx(MarkerSize.NORMAL, 3.0f))
        assertEquals(114, CardMarkerPlacement.getMarkerSizePx(MarkerSize.LARGE, 3.0f))
    }

    @Test
    fun getColorForTone_mapsCorrectColors() {
        assertEquals(CardMarkerPlacement.COLOR_GREEN, CardMarkerPlacement.getColorForTone(Tone.GREEN))
        assertEquals(CardMarkerPlacement.COLOR_ORANGE, CardMarkerPlacement.getColorForTone(Tone.ORANGE))
        assertEquals(CardMarkerPlacement.COLOR_RED, CardMarkerPlacement.getColorForTone(Tone.RED))
        assertEquals(CardMarkerPlacement.COLOR_GRAY, CardMarkerPlacement.getColorForTone(Tone.GRAY))
    }

    @Test
    fun getColorForTone_returnsRouteGreenWhenOpenForRoute() {
        assertEquals(
            CardMarkerPlacement.COLOR_ROUTE_GREEN,
            CardMarkerPlacement.getColorForTone(Tone.GREEN, isOpenForRoute = true)
        )
        // Non-green tone does not get route green
        assertEquals(
            CardMarkerPlacement.COLOR_RED,
            CardMarkerPlacement.getColorForTone(Tone.RED, isOpenForRoute = true)
        )
    }

    @Test
    fun calculateMarkerRect_placesBesideCardInRtl() {
        // Card located from left=150 to right=950, top=400, bottom=600
        val cardBounds = Rect(150, 400, 950, 600)
        val markerSizePx = 60
        val marginPx = 10

        val markerRect = CardMarkerPlacement.calculateMarkerRect(
            cardBounds = cardBounds,
            markerSizePx = markerSizePx,
            isRtl = true,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPx = marginPx
        )

        // In RTL: candidateLeft = cardBounds.left - size - margin = 150 - 60 - 10 = 80
        assertEquals(80, markerRect.left)
        assertEquals(140, markerRect.right)

        // Centered vertically: centerY = 500, rawTop = 500 - 30 = 470
        assertEquals(470, markerRect.top)
        assertEquals(530, markerRect.bottom)
    }

    @Test
    fun calculateMarkerRect_placesBesideCardInLtr() {
        val cardBounds = Rect(100, 300, 800, 500)
        val markerSizePx = 50
        val marginPx = 10

        val markerRect = CardMarkerPlacement.calculateMarkerRect(
            cardBounds = cardBounds,
            markerSizePx = markerSizePx,
            isRtl = false,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPx = marginPx
        )

        // In LTR: candidateLeft = cardBounds.right + margin = 800 + 10 = 810
        assertEquals(810, markerRect.left)
        assertEquals(860, markerRect.right)

        // Centered vertically: centerY = 400, rawTop = 400 - 25 = 375
        assertEquals(375, markerRect.top)
        assertEquals(425, markerRect.bottom)
    }

    @Test
    fun calculateMarkerRect_clampsToScreenEdges() {
        // Card touching top-left (0, 0, 300, 200)
        val cardBoundsTopLeft = Rect(0, 0, 300, 200)
        val markerRectTopLeft = CardMarkerPlacement.calculateMarkerRect(
            cardBounds = cardBoundsTopLeft,
            markerSizePx = 60,
            isRtl = true,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        // Must be clamped within screen
        assertTrue(markerRectTopLeft.left >= 0)
        assertTrue(markerRectTopLeft.top >= 0)
        assertTrue(markerRectTopLeft.right <= screenWidth)
        assertTrue(markerRectTopLeft.bottom <= screenHeight)

        // Card touching bottom-right
        val cardBoundsBottomRight = Rect(800, 2200, 1080, 2340)
        val markerRectBottomRight = CardMarkerPlacement.calculateMarkerRect(
            cardBounds = cardBoundsBottomRight,
            markerSizePx = 60,
            isRtl = false,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        assertTrue(markerRectBottomRight.left >= 0)
        assertTrue(markerRectBottomRight.top >= 0)
        assertTrue(markerRectBottomRight.right <= screenWidth)
        assertTrue(markerRectBottomRight.bottom <= screenHeight)
    }

    @Test
    fun mapOpacityToAlpha_clampsCorrectly() {
        assertEquals(0.20f, CardMarkerPlacement.mapOpacityToAlpha(10), 0.001f) // Clamped to 20%
        assertEquals(0.20f, CardMarkerPlacement.mapOpacityToAlpha(20), 0.001f)
        assertEquals(0.67f, CardMarkerPlacement.mapOpacityToAlpha(67), 0.001f)
        assertEquals(1.00f, CardMarkerPlacement.mapOpacityToAlpha(100), 0.001f)
        assertEquals(1.00f, CardMarkerPlacement.mapOpacityToAlpha(150), 0.001f) // Clamped to 100%
    }
}
