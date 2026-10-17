package com.driveflow.driver.reader.indrive

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.reader.parser.ArabicTextParser

class InDriveDetailReader(
    private val config: InDriveReaderConfig = InDriveReaderConfig.DEFAULT
) {
    fun isDetailOpen(root: AccessibilityNodeInfo): Boolean {
        val detailContainer = InDriveNodeUtils.findFirstNodeByViewIds(root, config.detailContainerIds)
        if (detailContainer != null) {
            val b = InDriveNodeUtils.getBounds(detailContainer)
            if (b.height() > 300) return true
        }

        val allTexts = InDriveNodeUtils.collectAllTextsWithBounds(root, maxDepth = 10)
        return allTexts.any { (txt, _) ->
            txt.contains("عرض سعرك") || txt.contains("اعرض سعرك") ||
            txt.contains("قبول الطلب") || txt.contains("اقبل الطلب") ||
            txt.contains("Offer your fare", ignoreCase = true) ||
            txt.contains("Counter offer", ignoreCase = true)
        }
    }

    fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        if (!isDetailOpen(root)) return null

        val detailContainer = InDriveNodeUtils.findFirstNodeByViewIds(root, config.detailContainerIds) ?: root
        val bounds = InDriveNodeUtils.getBounds(detailContainer)
        val textNodes = InDriveNodeUtils.collectAllTextsWithBounds(detailContainer)
        if (textNodes.isEmpty()) return null

        var fareStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.fareViewIds)
        if (fareStr.isNullOrBlank()) {
            fareStr = textNodes.firstOrNull { (txt, _) ->
                txt.contains("ج.م") || txt.contains("EGP") || txt.contains("جنيه")
            }?.first
        }
        val (fare, currency) = ArabicTextParser.parseFare(fareStr)

        var pickupDistStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.pickupDistanceIds)
        var tripDistStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.tripDistanceIds)

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

        var pickupAddr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.pickupAddressIds)
        var destAddr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.destinationAddressIds)

        if (pickupAddr.isNullOrBlank() || destAddr.isNullOrBlank()) {
            val addressCandidates = textNodes.filter { (txt, _) ->
                txt != fareStr && txt != pickupDistStr && txt != tripDistStr &&
                !txt.contains("ج.م") && !txt.contains("EGP") &&
                !txt.contains("دقيقة") && !txt.contains("د") &&
                !txt.contains("★") && txt.length > 3
            }.sortedBy { it.second.top }

            if (pickupAddr.isNullOrBlank() && addressCandidates.isNotEmpty()) {
                pickupAddr = addressCandidates[0].first
            }
            if (destAddr.isNullOrBlank() && addressCandidates.size > 1) {
                destAddr = addressCandidates[1].first
            }
        }

        pickupAddr = ArabicTextParser.cleanAddress(pickupAddr)
        destAddr = ArabicTextParser.cleanAddress(destAddr)

        val extraStopsStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.extraStopsIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("وقفة") || txt.contains("وقفات") || txt.contains("+")
            }?.first
        val extraStops = ArabicTextParser.parseExtraStops(extraStopsStr)

        var ratingStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.ratingIds)
        var tripsStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.tripsCountIds)

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

        val ageStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.requestAgeIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("ثانية") || txt.contains("دقيقة")
            }?.first
        val requestAgeSeconds = ArabicTextParser.parseAgeSeconds(ageStr)

        val paymentStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.paymentMethodIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("كاش") || txt.contains("نقدي") || txt.contains("نقدًا")
            }?.first
        val paymentMethod = ArabicTextParser.parsePaymentMethod(paymentStr) ?: "CASH"

        val noteStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.riderNoteIds)
        val riderNote = noteStr?.takeIf { it.isNotBlank() }

        val offersStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.offersCountIds)
            ?: textNodes.firstOrNull { (txt, _) ->
                txt.contains("عروض") || txt.contains("عرض")
            }?.first
        val offerCount = ArabicTextParser.extractFirstInt(offersStr)

        val roadStr = InDriveNodeUtils.findTextByViewIds(detailContainer, config.roadStateIds)
        val roadState = ArabicTextParser.parseRoadState(roadStr)

        val keySource = "indrive_detail_${fare}_${pickupAddr.orEmpty().take(20)}_${destAddr.orEmpty().take(20)}"
        val key = "indrive_${keySource.hashCode().toUInt().toString(16)}"

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
            isDetailOpen = true,
            confidence = 1.0f
        )
    }

    fun findMapBounds(root: AccessibilityNodeInfo): Rect? {
        val mapNode = InDriveNodeUtils.findFirstNodeByViewIds(root, config.mapContainerIds)
        if (mapNode != null) {
            val b = InDriveNodeUtils.getBounds(mapNode)
            if (b.width() > 100 && b.height() > 100) {
                return b
            }
        }
        return findMapBoundsRecursive(root, 0)
    }

    private fun findMapBoundsRecursive(node: AccessibilityNodeInfo, depth: Int): Rect? {
        if (depth > 10) return null
        val cls = node.className?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty().lowercase()
        val bounds = InDriveNodeUtils.getBounds(node)
        val isMap = cls.contains("MapView", ignoreCase = true) ||
                cls.contains("SurfaceView", ignoreCase = true) ||
                desc.contains("google map", ignoreCase = true) ||
                desc.contains("خريطة") ||
                desc.contains("map", ignoreCase = true)

        if (isMap && bounds.width() > 200 && bounds.height() > 200) {
            return bounds
        }

        val childCount = try { node.childCount } catch (_: Exception) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            val found = findMapBoundsRecursive(child, depth + 1)
            if (found != null) return found
        }
        return null
    }
}
