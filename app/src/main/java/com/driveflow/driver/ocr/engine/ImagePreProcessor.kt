package com.driveflow.driver.ocr.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

object ImagePreProcessor {
    fun crop(source: Bitmap, cropF: RectF): Bitmap {
        val width = source.width
        val height = source.height
        val l = (cropF.left * width).toInt().coerceIn(0, width - 1)
        val t = (cropF.top * height).toInt().coerceIn(0, height - 1)
        val r = (cropF.right * width).toInt().coerceIn(l + 1, width)
        val b = (cropF.bottom * height).toInt().coerceIn(t + 1, height)
        val cropWidth = (r - l).coerceAtLeast(1)
        val cropHeight = (b - t).coerceAtLeast(1)
        return Bitmap.createBitmap(source, l, t, cropWidth, cropHeight)
    }

    fun crop(source: Bitmap, rect: Rect): Bitmap {
        val l = rect.left.coerceIn(0, source.width - 1)
        val t = rect.top.coerceIn(0, source.height - 1)
        val r = rect.right.coerceIn(l + 1, source.width)
        val b = rect.bottom.coerceIn(t + 1, source.height)
        val cropWidth = (r - l).coerceAtLeast(1)
        val cropHeight = (b - t).coerceAtLeast(1)
        return Bitmap.createBitmap(source, l, t, cropWidth, cropHeight)
    }

    fun preprocessForOcr(input: Bitmap): Bitmap {
        var scale = 1.0f
        if (input.width < 250 || input.height < 80) {
            scale = 2.5f
        } else if (input.width < 400 || input.height < 120) {
            scale = 2.0f
        }

        val targetWidth = (input.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (input.height * scale).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = ColorMatrix().apply {
            setSaturation(0.0f)
            val scaleMatrix = floatArrayOf(
                1.4f, 0f, 0f, 0f, -30f,
                0f, 1.4f, 0f, 0f, -30f,
                0f, 0f, 1.4f, 0f, -30f,
                0f, 0f, 0f, 1f, 0f
            )
            postConcat(ColorMatrix(scaleMatrix))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        val srcRect = Rect(0, 0, input.width, input.height)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(input, srcRect, dstRect, paint)
        return output
    }
}
