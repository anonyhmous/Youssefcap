package com.driveflow.driver.ocr.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.OcrEngine
import com.driveflow.driver.ocr.model.OcrText

class FakeOcrEngine(
    var defaultText: String = "",
    var defaultConfidence: Float = 0.95f
) : OcrEngine {
    private val responsesByHint = mutableMapOf<Hint, OcrText>()
    private val responsesByCrop = mutableMapOf<String, OcrText>()

    fun setResponse(hint: Hint, text: String, confidence: Float = 0.95f) {
        responsesByHint[hint] = OcrText(text, confidence)
    }

    fun setResponse(cropKey: String, text: String, confidence: Float = 0.95f) {
        responsesByCrop[cropKey] = OcrText(text, confidence)
    }

    override suspend fun read(bitmap: Bitmap, crop: RectF, hint: Hint): OcrText {
        val key = "${crop.left}_${crop.top}_${crop.right}_${crop.bottom}"
        responsesByCrop[key]?.let { return it }
        responsesByHint[hint]?.let { return it }
        return OcrText(defaultText, defaultConfidence)
    }
}
