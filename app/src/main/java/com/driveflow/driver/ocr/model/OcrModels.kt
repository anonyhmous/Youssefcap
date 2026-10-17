package com.driveflow.driver.ocr.model

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class CropRectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRectF(): RectF = RectF(left, top, right, bottom)

    fun toScreenRect(screenWidth: Int, screenHeight: Int): Rect {
        val l = (left * screenWidth).toInt().coerceIn(0, screenWidth)
        val t = (top * screenHeight).toInt().coerceIn(0, screenHeight)
        val r = (right * screenWidth).toInt().coerceIn(l, screenWidth)
        val b = (bottom * screenHeight).toInt().coerceIn(t, screenHeight)
        return Rect(l, t, r, b)
    }

    companion object {
        fun fromRect(rect: Rect, screenWidth: Int, screenHeight: Int): CropRectF {
            if (screenWidth <= 0 || screenHeight <= 0) return CropRectF(0f, 0f, 0f, 0f)
            return CropRectF(
                left = (rect.left.toFloat() / screenWidth).coerceIn(0f, 1f),
                top = (rect.top.toFloat() / screenHeight).coerceIn(0f, 1f),
                right = (rect.right.toFloat() / screenWidth).coerceIn(0f, 1f),
                bottom = (rect.bottom.toFloat() / screenHeight).coerceIn(0f, 1f)
            )
        }
    }
}

@Serializable
data class ScreenCrops(
    val status: String = "UNCALIBRATED", // "UNCALIBRATED" or "CALIBRATED"
    val cardAnchor: CropRectF? = null,
    val fareBox: CropRectF,
    val pickupBox: CropRectF,
    val tripBox: CropRectF,
    val ratingBox: CropRectF? = null,
    val productBox: CropRectF? = null
)

@Serializable
data class ChipConfig(
    val targetHexColor: String = "#C5F01A",
    val colorTolerance: Float = 0.18f,
    val minAreaPx: Int = 2000,
    val maxAreaPx: Int = 75000,
    val minAspectRatio: Float = 1.2f,
    val maxAspectRatio: Float = 5.0f
)

@Serializable
data class AppOcrConfig(
    val app: String,
    val enabled: Boolean = true,
    val revision: Int = 1,
    val mapFraction: CropRectF = CropRectF(0.0f, 0.085f, 1.0f, 0.545f),
    val listCropsAr: ScreenCrops? = null,
    val listCropsEn: ScreenCrops? = null,
    val detailCropsAr: ScreenCrops? = null,
    val detailCropsEn: ScreenCrops? = null,
    val tripChip: ChipConfig? = null,
    val pickupChip: ChipConfig? = null
)

enum class Hint {
    DIGITS,
    FARE,
    DISTANCE,
    TEXT
}

data class OcrText(
    val text: String,
    val confidence: Float
)

interface OcrEngine {
    suspend fun read(bitmap: Bitmap, crop: RectF, hint: Hint): OcrText
}
