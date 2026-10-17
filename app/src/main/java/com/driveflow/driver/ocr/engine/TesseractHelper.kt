package com.driveflow.driver.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.OcrText
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TesseractHelper(private val context: Context) {
    private var tessBaseApi: TessBaseAPI? = null
    private var isInitialized = false

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        try {
            val tessDir = File(context.filesDir, "tessdata")
            if (!tessDir.exists()) {
                tessDir.mkdirs()
            }
            copyAssetIfNeeded("tessdata/ara.traineddata", File(tessDir, "ara.traineddata"))
            copyAssetIfNeeded("tessdata/eng.traineddata", File(tessDir, "eng.traineddata"))

            val api = TessBaseAPI()
            val success = try {
                api.init(context.filesDir.absolutePath, "ara+eng")
            } catch (e: Exception) {
                Log.w(TAG, "Multi-language init failed, trying eng: ${e.message}")
                try {
                    api.init(context.filesDir.absolutePath, "eng")
                } catch (e2: Exception) {
                    Log.w(TAG, "eng init failed: ${e2.message}")
                    false
                }
            }

            if (success) {
                tessBaseApi = api
                isInitialized = true
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Tesseract: ${e.message}")
            false
        }
    }

    private fun copyAssetIfNeeded(assetPath: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0) return
        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy asset $assetPath: ${e.message}")
        }
    }

    suspend fun recognize(bitmap: Bitmap, hint: Hint): OcrText = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            initialize()
        }
        val api = tessBaseApi ?: return@withContext OcrText("", 0f)
        try {
            when (hint) {
                Hint.DIGITS, Hint.FARE, Hint.DISTANCE -> {
                    api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789٠١٢٣٤٥٦٧٨٩.,٫EGPLEkmكمج.م")
                }
                Hint.TEXT -> {
                    api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "")
                }
            }
            api.setImage(bitmap)
            val utf8Text = api.utF8Text.orEmpty().trim()
            val conf = (api.meanConfidence() / 100f).coerceIn(0f, 1f)
            OcrText(utf8Text, conf)
        } catch (e: Exception) {
            Log.w(TAG, "Tesseract recognition error: ${e.message}")
            OcrText("", 0f)
        }
    }

    fun close() {
        try {
            tessBaseApi?.recycle()
            tessBaseApi = null
            isInitialized = false
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "TesseractHelper"
    }
}
