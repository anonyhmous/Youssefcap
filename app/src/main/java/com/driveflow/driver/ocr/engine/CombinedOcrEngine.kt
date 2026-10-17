package com.driveflow.driver.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.OcrEngine
import com.driveflow.driver.ocr.model.OcrText
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

class CombinedOcrEngine(
    private val context: Context,
    private val tesseractHelper: TesseractHelper = TesseractHelper(context)
) : OcrEngine {

    private val mlKitRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun read(bitmap: Bitmap, crop: RectF, hint: Hint): OcrText = withContext(Dispatchers.Default) {
        val result = withTimeoutOrNull(700) {
            val cropped = ImagePreProcessor.crop(bitmap, crop)
            val preprocessed = ImagePreProcessor.preprocessForOcr(cropped)
            try {
                val mlKitResult = runMlKit(preprocessed)
                if (mlKitResult.text.isNotBlank() && (hint == Hint.DIGITS || mlKitResult.confidence > 0.6f)) {
                    mlKitResult
                } else {
                    val tessResult = tesseractHelper.recognize(preprocessed, hint)
                    if (tessResult.text.isNotBlank() && tessResult.confidence >= mlKitResult.confidence) {
                        tessResult
                    } else if (mlKitResult.text.isNotBlank()) {
                        mlKitResult
                    } else {
                        tessResult
                    }
                }
            } finally {
                if (preprocessed != cropped) {
                    preprocessed.recycle()
                }
                cropped.recycle()
            }
        }
        result ?: OcrText("", 0f)
    }

    private fun runMlKit(bitmap: Bitmap): OcrText {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = Tasks.await(mlKitRecognizer.process(inputImage), 350, TimeUnit.MILLISECONDS)
            val text = visionText.text.trim()
            var totalConf = 0f
            var blockCount = 0
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        element.confidence?.let {
                            totalConf += it
                            blockCount++
                        }
                    }
                }
            }
            val confidence = if (blockCount > 0) totalConf / blockCount else 0.85f
            OcrText(text, confidence)
        } catch (e: Exception) {
            Log.w(TAG, "ML Kit recognition error or timeout: ${e.message}")
            OcrText("", 0f)
        }
    }

    companion object {
        private const val TAG = "CombinedOcrEngine"
    }
}
