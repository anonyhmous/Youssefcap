package com.driveflow.driver.reader.indrive

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.reader.parser.ArabicTextParser

class InDriveListReader(
    private val config: InDriveReaderConfig = InDriveReaderConfig.DEFAULT
) {
    fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        val cardNodes = InDriveNodeUtils.findCandidateCardNodes(root, config)
        if (cardNodes.isEmpty()) return emptyList()

        val parsedRequests = mutableListOf<ParsedRequest>()
        val seenKeys = mutableSetOf<String>()

        for (card in cardNodes) {
            val bounds = InDriveNodeUtils.getBounds(card)
            if (bounds.width() <= 0 || bounds.height() <= 0) continue
            val request = parseSingleCard(card, bounds) ?: continue
            if (seenKeys.add(request.key)) {
                parsedRequests.add(request)
            }
        }
        return parsedRequests
    }

    private fun parseSingleCard(card: AccessibilityNodeInfo, bounds: Rect): ParsedRequest? {
        val textNodes = InDriveNodeUtils.collectAllTextsWithBounds(card)
        if (textNodes.isEmpty()) return null

        var fareStr = InDriveNodeUtils.findTextByViewIds(card, config.fareViewIds)
        if (fareStr.isNullOrBlank()) {
            fareStr = textNodes.firstOrNull { (txt, _) ->
                txt.contains("ج.م") || txt.contains("EGP") || txt.contains("جنيه") || txt.contains("LE")
            }?.first
        }
        val (fare, currency) = ArabicTextParser.parseFare(fareStr)
        if (fare == null) return null

        var pickupDistStr = InDriveNodeUtils.findTextByViewIds(card, config.pickupDistanceIds)
        var tripDistStr = InDriveNodeUtils.findTextByViewIds(card, config.tripDistanceIds)

        val distanceTexts = textNodes.filter { (txt, _) ->
            (txt.contains("كم") || txt.contains("km", ignoreCase = true) ||
             txt.contains("متر") || txt.endsWith("م") || txt.contains(" m", ignoreCase = true)) &&
            !txt.contains("ج.م") && !txt.contains("EGP")
        }.map { it.first }

        if (pickupDistStr.isNullOrBlank() && distanceTexts.isNotEmpty()) {
            pickupDistStr = distanceTexts[0]
            if (distanceTexts.size > 1 && tripDistStr.isNullOrBlank()) {
                tripDistStr = distanceTexts[1]
            }
        }

        val pickupKm = ArabicTextParser.parseDistanceKm(pickupDistStr)
        val tripKm = ArabicTextParser.parseDistanceKm(tripDistStr)
        val totalKm = when {
            pickupKm != null && tripKm != null -> pickupKm + tripKm
            tripKm != null -> tripKm
            pickupKm != null -> pickupKm
            else -> null
        }

        val timeNode = textNodes.firstOrNull { (txt, _) ->
            (txt.contains("دقيقة") || txt.contains("د") || txt.contains("min", ignoreCase = true)) &&
            !txt.contains("متر")
        }
        val pickupMinutes = ArabicTextParser.parseMinutes(timeNode?.first)

        var pickupAddr = InDriveNodeUtils.findTextByViewIds(card, config.pickupAddressIds)
        var destAddr = InDriveNodeUtils.findTextByViewIds(card, config.destinationAddressIds)

        if (pickupAddr.isNullOrBlank() || destAddr.isNullOrBlank()) {
            val nonMetricsTexts = textNodes.filter { (txt, _) ->
                txt != fareStr && txt != pickupDistStr && txt != tripDistStr &&
                !txt.contains("ج.م") && !txt.contains("EGP") &&
                !txt.contains("دقيقة") && !txt.contains("د") && !txt.contains("★") &&
                txt.length > 2
            }.sortedBy { it.second.top }

            if (pickupAddr.isNullOrBlank() && nonMetricsTexts.isNotEmpty()) {
                pickupAddr = nonMetricsTexts[0].first
            }
            if (destAddr.isNullOrBlank() && nonMetricsTexts.size > 1) {
                destAddr = nonMetricsTexts[1].first
            }
        }

        pickupAddr = ArabicTextParser.cleanAddress(pickupAddr)
        destAddr = ArabicTextParser.cleanAddress(destAddr)

        val extraStopsStr = InDriveNodeUtils.findTextByViewIds(card, config.extraStopsIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("وقفة") || txt.contains("وقفات") || txt.contains("+")
            }?.first
        val extraStops = ArabicTextParser.parseExtraStops(extraStopsStr)

        var ratingStr = InDriveNodeUtils.findTextByViewIds(card, config.ratingIds)
        var tripsStr = InDriveNodeUtils.findTextByViewIds(card, config.tripsCountIds)

        if (ratingStr.isNullOrBlank()) {
            ratingStr = textNodes.firstOrNull { (txt, _) ->
                txt.contains("★") || txt.matches(Regex(""".*[1-5]\.\d{1,2}.*"""))
            }?.first
        }
        if (tripsStr.isNullOrBlank()) {
            tripsStr = textNodes.firstOrNull { (txt, _) ->
                txt.contains("رحلة") || txt.contains("rides", ignoreCase = true) ||
                txt.matches(Regex(""".*\(\d+\).*"""))
            }?.first
        }

        val (parsedRating, parsedTripsFromRating) = ArabicTextParser.parseRatingAndTrips(ratingStr)
        val (_, parsedTripsFromStr) = ArabicTextParser.parseRatingAndTrips(tripsStr)
        val riderRating = parsedRating
        val riderTrips = parsedTripsFromStr ?: parsedTripsFromRating

        val ageStr = InDriveNodeUtils.findTextByViewIds(card, config.requestAgeIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("ثانية") || txt.contains("دقيقة") || txt.contains("ago", ignoreCase = true)
            }?.first
        val requestAgeSeconds = ArabicTextParser.parseAgeSeconds(ageStr)

        val paymentStr = InDriveNodeUtils.findTextByViewIds(card, config.paymentMethodIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("كاش") || txt.contains("نقدي") || txt.contains("نقدًا") || txt.contains("cash", ignoreCase = true)
            }?.first
        val paymentMethod = ArabicTextParser.parsePaymentMethod(paymentStr) ?: "CASH"

        val noteStr = InDriveNodeUtils.findTextByViewIds(card, config.riderNoteIds)
        val riderNote = noteStr?.takeIf { it.isNotBlank() }

        val offersStr = InDriveNodeUtils.findTextByViewIds(card, config.offersCountIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("عروض") || txt.contains("عرض") || txt.contains("offers", ignoreCase = true)
            }?.first
        val offerCount = ArabicTextParser.extractFirstInt(offersStr)

        val roadStr = InDriveNodeUtils.findTextByViewIds(card, config.roadStateIds)
        val roadState = ArabicTextParser.parseRoadState(roadStr)

        val keySource = "indrive_${fare}_${pickupAddr.orEmpty().take(20)}_${destAddr.orEmpty().take(20)}_${pickupKm ?: 0.0}"
        val key = "indrive_${keySource.hashCode().toUInt().toString(16)}"

        var confidence = 0.5f
        if (fare > 0) confidence += 0.2f
        if (!pickupAddr.isNullOrBlank()) confidence += 0.15f
        if (!destAddr.isNullOrBlank()) confidence += 0.15f

        return ParsedRequest(
            app = RideApp.INDRIVE,
            key = key,
            source = ReadSource.ACCESSIBILITY,
            fare = fare,
            currency = currency ?: "EGP",
            pickupKm = pickupKm,
            tripKm = tripKm,
            totalKm = totalKm,
            pickupMinutes = pickupMinutes,
            riderRating = riderRating,
            riderTrips = riderTrips,
            requestAgeSeconds = requestAgeSeconds,
            pickupAddress = pickupAddr,
            destinationAddress = destAddr,
            extraStops = extraStops,
            paymentMethod = paymentMethod,
            riderNote = riderNote,
            offerCount = offerCount,
            roadState = roadState,
            productName = "inDrive",
            bounds = bounds,
            isDetailOpen = false,
            confidence = confidence.coerceIn(0.0f, 1.0f)
        )
    }
}
