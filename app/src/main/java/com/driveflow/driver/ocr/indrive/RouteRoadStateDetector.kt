package com.driveflow.driver.ocr.indrive

import android.graphics.Bitmap
import android.graphics.Color
import com.driveflow.driver.core.model.RoadState

object RouteRoadStateDetector {
    fun detectRoadState(mapBitmap: Bitmap, enabled: Boolean = false): RoadState {
        if (!enabled) return RoadState.UNKNOWN
        val w = mapBitmap.width
        val h = mapBitmap.height
        val pixels = IntArray(w * h)
        mapBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var easyCount = 0
        var slowCount = 0
        var hardCount = 0
        val stride = 4

        for (i in 0 until (w * h) step stride) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val hsv = FloatArray(3)
            Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val value = hsv[2]

            if (sat > 0.4f && value > 0.3f) {
                when {
                    (hue in 350f..360f || hue in 0f..15f) && r > 150 && g < 70 -> {
                        hardCount++
                    }
                    hue in 16f..45f && r > 180 && g in 70..160 && b < 70 -> {
                        slowCount++
                    }
                    (hue in 90f..160f && g > 150) || (hue in 190f..240f && b > 180) -> {
                        easyCount++
                    }
                }
            }
        }

        return when {
            hardCount > 100 && hardCount > slowCount -> RoadState.HARD
            slowCount > 100 -> RoadState.SLOW
            easyCount > 100 -> RoadState.EASY
            else -> RoadState.UNKNOWN
        }
    }
}
