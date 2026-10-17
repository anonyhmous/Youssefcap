package com.driveflow.driver.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.ocr.indrive.RouteRoadStateDetector
import com.driveflow.driver.ocr.model.AppOcrConfig
import com.driveflow.driver.ocr.model.Hint
import com.driveflow.driver.ocr.model.OcrEngine
import com.driveflow.driver.ocr.model.ScreenCrops
import com.driveflow.driver.reader.parser.ArabicTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class ScreenKind {
    LIST_CARD,
    DETAIL
}

class OcrReadCoordinator(
    private val screenshotProvider: ScreenshotProvider,
    private val ocrEngine: OcrEngine,
    private val configRepository: OcrConfigRepository,
    var experimentalRoadStateEnabled: Boolean = false
) {
    private val _readingFlow = MutableSharedFlow<List<ParsedRequest>>(replay = 1)
    val readingFlow: SharedFlow<List<ParsedRequest>> = _readingFlow.asSharedFlow()

    private val cachedRequests = ConcurrentHashMap<RideApp, List<ParsedRequest>>()

    fun getLatestRequests(app: RideApp): List<ParsedRequest> {
        return cachedRequests[app] ?: emptyList()
    }

    suspend fun processScreen(
        app: RideApp,
        screenKind: ScreenKind,
        isArabic: Boolean,
        windowBounds: Rect
    ): List<ParsedRequest> = withContext(Dispatchers.Default) {
        val config = configRepository.getConfig(app)
        if (!config.enabled) {
            return@withContext emptyList()
        }

        val crops = resolveCrops(config, screenKind, isArabic)
        if (crops == null) {
            return@withContext emptyList()
        }

        val captureResult = screenshotProvider.capture()
        val bitmap = when (captureResult) {
            is ScreenshotResult.Success -> captureResult.bitmap
            is ScreenshotResult.Failure -> return@withContext emptyList()
        }

        try {
            val requests = parseFromBitmap(bitmap, app, screenKind, crops, windowBounds)
            cachedRequests[app] = requests
            _readingFlow.emit(requests)
            requests
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun parseFromBitmap(
        bitmap: Bitmap,
        app: RideApp,
        screenKind: ScreenKind,
        crops: ScreenCrops,
        windowBounds: Rect
    ): List<ParsedRequest> {
        val width = bitmap.width
        val height = bitmap.height

        val fareOcr = ocrEngine.read(bitmap, crops.fareBox.toRectF(), Hint.FARE)
        val cleanedFareText = ArabicTextParser.cleanOcrText(fareOcr.text)
        val (parsedFare, currency) = ArabicTextParser.parseFare(cleanedFareText)

        val pickupOcr = ocrEngine.read(bitmap, crops.pickupBox.toRectF(), Hint.DISTANCE)
        val cleanedPickupText = ArabicTextParser.cleanOcrText(pickupOcr.text)
        val pickupKm = ArabicTextParser.parseDistanceKm(cleanedPickupText)
        val pickupMinutes = ArabicTextParser.parseMinutes(cleanedPickupText)

        val tripOcr = ocrEngine.read(bitmap, crops.tripBox.toRectF(), Hint.DISTANCE)
        val cleanedTripText = ArabicTextParser.cleanOcrText(tripOcr.text)
        val tripKm = ArabicTextParser.parseDistanceKm(cleanedTripText)

        val ratingOcr = crops.ratingBox?.let {
            ocrEngine.read(bitmap, it.toRectF(), Hint.TEXT)
        }
        val (riderRating, riderTrips) = if (ratingOcr != null) {
            val cleanedRating = ArabicTextParser.cleanOcrText(ratingOcr.text)
            ArabicTextParser.parseRatingAndTrips(cleanedRating)
        } else {
            Pair(null, null)
        }

        val productOcr = crops.productBox?.let {
            ocrEngine.read(bitmap, it.toRectF(), Hint.TEXT)
        }
        val productName = productOcr?.let {
            ArabicTextParser.parseProduct(it.text)
        } ?: if (app == RideApp.UBER) "Uber" else "DiDi"

        val totalKm = when {
            pickupKm != null && tripKm != null -> pickupKm + tripKm
            tripKm != null -> tripKm
            pickupKm != null -> pickupKm
            else -> null
        }

        val validFare = ArabicTextParser.isValidFare(parsedFare)
        val validDistance = totalKm == null || ArabicTextParser.isValidDistance(totalKm)
        val validRating = riderRating == null || ArabicTextParser.isValidRating(riderRating)

        val confidences = mutableListOf<Float>()
        if (fareOcr.confidence > 0f) confidences.add(fareOcr.confidence)
        if (pickupOcr.confidence > 0f) confidences.add(pickupOcr.confidence)
        if (tripOcr.confidence > 0f) confidences.add(tripOcr.confidence)
        ratingOcr?.confidence?.let { if (it > 0f) confidences.add(it) }

        var meanConfidence = if (confidences.isNotEmpty()) confidences.average().toFloat() else 0.5f

        if (!validFare) meanConfidence -= 0.35f
        if (!validDistance) meanConfidence -= 0.25f
        if (!validRating) meanConfidence -= 0.10f

        val finalConfidence = meanConfidence.coerceIn(0.0f, 1.0f)

        val cardBounds = if (crops.cardAnchor != null) {
            crops.cardAnchor.toScreenRect(width, height)
        } else {
            Rect(
                (width * 0.05f).toInt(),
                (height * 0.50f).toInt(),
                (width * 0.95f).toInt(),
                (height * 0.95f).toInt()
            )
        }

        val roadState = if (screenKind == ScreenKind.DETAIL && experimentalRoadStateEnabled) {
            RouteRoadStateDetector.detectRoadState(bitmap, enabled = true)
        } else {
            RoadState.UNKNOWN
        }

        val keySource = "${app.code}_${parsedFare}_${pickupKm}_${tripKm}_${screenKind.name}"
        val key = "${app.code}_${keySource.hashCode().toUInt().toString(16)}"

        val finalFare = if (validFare) parsedFare else null
        val finalTripKm = if (validDistance) tripKm else null
        val finalPickupKm = if (validDistance) pickupKm else null
        val finalTotalKm = if (validDistance) totalKm else null
        val isDetail = (screenKind == ScreenKind.DETAIL)

        val request = ParsedRequest(
            app = app,
            key = key,
            source = ReadSource.OCR,
            fare = finalFare,
            currency = currency ?: "EGP",
            pickupKm = finalPickupKm,
            tripKm = finalTripKm,
            totalKm = finalTotalKm,
            pickupMinutes = pickupMinutes,
            riderRating = if (validRating) riderRating else null,
            riderTrips = riderTrips,
            requestAgeSeconds = 0,
            pickupAddress = null,
            destinationAddress = null,
            extraStops = 0,
            paymentMethod = "CASH",
            riderNote = null,
            offerCount = null,
            roadState = roadState,
            productName = productName,
            bounds = cardBounds,
            isDetailOpen = isDetail,
            confidence = if (finalFare == null || finalTotalKm == null) 0.2f else finalConfidence
        )

        return listOf(request)
    }

    private fun resolveCrops(config: AppOcrConfig, kind: ScreenKind, isArabic: Boolean): ScreenCrops? {
        return when (kind) {
            ScreenKind.LIST_CARD -> if (isArabic) config.listCropsAr ?: config.listCropsEn else config.listCropsEn ?: config.listCropsAr
            ScreenKind.DETAIL -> if (isArabic) config.detailCropsAr ?: config.detailCropsEn else config.detailCropsEn ?: config.detailCropsAr
        }
    }
}
