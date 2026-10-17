package com.driveflow.driver.engine

import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure Kotlin Decision Engine for DriveFlow.
 * Evaluates a [ParsedRequest] against driver's [Policy] and [HomeWayState],
 * producing an actionable, color-coded [Verdict] with reason codes and flags.
 */
object DecisionEngine {

    /**
     * Evaluates a ride request and produces a [Verdict].
     *
     * Decision order (first match wins):
     * 1. APP_DISABLED (GRAY)
     * 2. DATA_MISSING (GRAY)
     * 3. Hard REDs
     * 4. ORANGE (Think first / Warnings)
     * 5. GREEN otherwise
     */
    fun decide(
        req: ParsedRequest,
        policy: Policy,
        home: HomeWayState? = null,
        context: DecisionContext = DecisionContext(),
        geoResolver: GeoResolver = StubGeoResolver
    ): Verdict {
        // --- 1. Base Distance & Financial Definitions ---
        // totalKm = req.totalKm ?: (pickupKm + tripKm) when both exist
        val totalKm: Double? = req.totalKm ?: if (req.pickupKm != null && req.tripKm != null) {
            req.pickupKm + req.tripKm
        } else {
            null
        }

        // billableKm = totalKm minus min(pickupKm, policy.freePickupKm ?: 0)
        val pickupKm = req.pickupKm ?: 0.0
        val freePickupAllowance = policy.freePickupKm ?: 0.0
        val pickupDeduction = if (totalKm != null && pickupKm > 0.0 && freePickupAllowance > 0.0) {
            min(pickupKm, freePickupAllowance)
        } else {
            0.0
        }
        val billableKm: Double? = if (totalKm != null) {
            totalKm - pickupDeduction
        } else {
            null
        }

        // grossPerKm = fare / billableKm (if billableKm <= 0 then null)
        val grossPerKm: Double? = if (req.fare != null && billableKm != null && billableKm > 0.0) {
            req.fare / billableKm
        } else {
            null
        }

        // carCost = policy.fuelCostPerKm + policy.upkeepCostPerKm (nulls = 0) times totalKm
        val carCostPerKm = (policy.fuelCostPerKm ?: 0.0) + (policy.upkeepCostPerKm ?: 0.0)
        val carCost: Double? = if (totalKm != null) {
            carCostPerKm * totalKm
        } else {
            null
        }

        // commission = fare * pct/100 when commissionEnabled[app] is true, else 0
        val isCommissionEnabled = policy.commissionEnabled[req.app] == true
        val commPct = if (isCommissionEnabled) (policy.commissionPct[req.app] ?: 0.0) else 0.0
        val commissionRate = commPct / 100.0
        val commission: Double = if (req.fare != null) {
            req.fare * commissionRate
        } else {
            0.0
        }

        // stopCost = extraStops * (policy.stopFee ?: 0)
        val stopFee = policy.stopFee ?: 0.0
        val stopCost = req.extraStops * stopFee

        // netProfit = fare - commission - carCost - stopCost (null if fare or totalKm missing)
        val netProfit: Double? = if (req.fare != null && totalKm != null && carCost != null) {
            req.fare - commission - carCost - stopCost
        } else {
            null
        }

        // netPerKm = netProfit / totalKm
        val netPerKm: Double? = if (netProfit != null && totalKm != null && totalKm > 0.0) {
            netProfit / totalKm
        } else {
            null
        }

        // The measured value for the price test is netPerKm when policy.basisNet is true, else grossPerKm
        val measuredValue: Double? = if (policy.basisNet) netPerKm else grossPerKm

        // suggestedPrice calculation
        val suggestedPrice: Double? = calculateSuggestedPrice(
            req = req,
            policy = policy,
            totalKm = totalKm,
            billableKm = billableKm,
            carCost = carCost,
            stopCost = stopCost,
            commissionRate = commissionRate,
            measuredValue = measuredValue
        )

        // Helper to construct verdict with all flags attached
        fun createVerdict(
            tone: Tone,
            reasonCode: String,
            titleAr: String,
            titleEn: String,
            detailAr: String,
            detailEn: String
        ): Verdict {
            val flags = mutableListOf<String>()

            // Quota awareness flag
            if (context.licenseStatus == LicenseStatus.FREE) {
                flags.add(DecisionFlags.FREE_PLAN)
            }

            // Evaluation flags (attached to any non-APP_DISABLED result where applicable)
            if (reasonCode != ReasonCodes.APP_DISABLED) {
                // PREFERRED_AREA
                if (policy.preferredAreas.isNotEmpty() &&
                    (TextNormalizer.matchesAnyArea(req.pickupAddress, policy.preferredAreas) ||
                     TextNormalizer.matchesAnyArea(req.destinationAddress, policy.preferredAreas))
                ) {
                    flags.add(DecisionFlags.PREFERRED_AREA)
                }

                // STOPS_PREFERRED
                if (policy.stopsMode == StopsMode.PREFER && req.extraStops > 0) {
                    flags.add(DecisionFlags.STOPS_PREFERRED)
                }

                // HOME_DIRECTION
                if (home != null && home.enabled && isHeadingHome(req, home, geoResolver)) {
                    flags.add(DecisionFlags.HOME_DIRECTION)
                }

                // FREE_PICKUP_USED
                if (pickupDeduction > 0.0) {
                    flags.add(DecisionFlags.FREE_PICKUP_USED)
                }

                // NET_BASIS
                if (policy.basisNet) {
                    flags.add(DecisionFlags.NET_BASIS)
                }

                // OPEN_FOR_ROUTE: when verdict is GREEN or ORANGE in LIST mode and route could still change decision (tripKm null)
                if ((tone == Tone.GREEN || tone == Tone.ORANGE) && !req.isDetailOpen && req.tripKm == null) {
                    flags.add(DecisionFlags.OPEN_FOR_ROUTE)
                }
            }

            return Verdict(
                tone = tone,
                reasonCode = reasonCode,
                titleAr = titleAr,
                titleEn = titleEn,
                detailAr = detailAr,
                detailEn = detailEn,
                flags = flags,
                totalKm = totalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = suggestedPrice
            )
        }

        // =====================================================================
        // Decision order (first match wins)
        // =====================================================================

        // 1. APP_DISABLED (GRAY, no mark) if policy.appEnabled[app] == false
        if (policy.appEnabled[req.app] == false) {
            return createVerdict(
                tone = Tone.GRAY,
                reasonCode = ReasonCodes.APP_DISABLED,
                titleAr = "التطبيق معطل",
                titleEn = "App Disabled",
                detailAr = "تم تعطيل متابعة هذا التطبيق في الإعدادات",
                detailEn = "Monitoring for this app is disabled in settings"
            )
        }

        // 2. DATA_MISSING (GRAY)
        // if fare is null, or distance data is not enough to decide and detailAvailable is false -> "open it to see the details";
        // if the request is in LIST mode and only the trip distance is missing, say so.
        if (req.fare == null) {
            return createVerdict(
                tone = Tone.GRAY,
                reasonCode = ReasonCodes.DATA_MISSING,
                titleAr = "بيانات ناقصة",
                titleEn = "Data Missing",
                detailAr = "افتح الطلب لعرض السعر والتفاصيل",
                detailEn = "Open the request to view fare and details"
            )
        }

        val distanceDataMissing = totalKm == null && req.tripKm == null
        if (distanceDataMissing && !context.detailAvailable) {
            val isOnlyTripMissing = !req.isDetailOpen && req.pickupKm != null
            val detailAr = if (isOnlyTripMissing) {
                "افتح الطلب لمعرفة مسافة المشوار"
            } else {
                "افتح الطلب لعرض تفاصيل المسافة"
            }
            val detailEn = if (isOnlyTripMissing) {
                "Open request to see trip distance"
            } else {
                "Open request to view distance details"
            }
            return createVerdict(
                tone = Tone.GRAY,
                reasonCode = ReasonCodes.DATA_MISSING,
                titleAr = "بيانات ناقصة",
                titleEn = "Data Missing",
                detailAr = detailAr,
                detailEn = detailEn
            )
        }

        // 3. Hard REDs, each its own code
        // PICKUP_TOO_FAR (pickupKm > maxPickupKm)
        if (req.pickupKm != null && policy.maxPickupKm != null && req.pickupKm > policy.maxPickupKm) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.PICKUP_TOO_FAR,
                titleAr = "بيك أب بعيد",
                titleEn = "Pickup Too Far",
                detailAr = "مسافة الوصول للزبون (${formatDecimal(req.pickupKm)} كم) تتجاوز الحد الأقصى (${formatDecimal(policy.maxPickupKm)} كم)",
                detailEn = "Pickup distance (${formatDecimal(req.pickupKm)} km) exceeds maximum limit (${formatDecimal(policy.maxPickupKm)} km)"
            )
        }

        // PICKUP_TOO_LONG (pickupMinutes > maxPickupMinutes)
        if (req.pickupMinutes != null && policy.maxPickupMinutes != null && req.pickupMinutes > policy.maxPickupMinutes) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.PICKUP_TOO_LONG,
                titleAr = "وقت وصول طويل",
                titleEn = "Pickup Too Long",
                detailAr = "وقت الوصول للزبون (${req.pickupMinutes} دقيقة) يتجاوز الحد الأقصى (${policy.maxPickupMinutes} دقيقة)",
                detailEn = "Pickup duration (${req.pickupMinutes} min) exceeds maximum limit (${policy.maxPickupMinutes} min)"
            )
        }

        // TRIP_TOO_LONG (tripKm > maxTripKm)
        if (req.tripKm != null && policy.maxTripKm != null && req.tripKm > policy.maxTripKm) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.TRIP_TOO_LONG,
                titleAr = "مشوار طويل جداً",
                titleEn = "Trip Too Long",
                detailAr = "مسافة المشوار (${formatDecimal(req.tripKm)} كم) تتجاوز الحد الأقصى (${formatDecimal(policy.maxTripKm)} كم)",
                detailEn = "Trip distance (${formatDecimal(req.tripKm)} km) exceeds maximum limit (${formatDecimal(policy.maxTripKm)} km)"
            )
        }

        // TOTAL_TOO_SHORT (totalKm < minTotalKm)
        if (totalKm != null && policy.minTotalKm != null && totalKm < policy.minTotalKm) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.TOTAL_TOO_SHORT,
                titleAr = "مشوار قصير جداً",
                titleEn = "Trip Too Short",
                detailAr = "إجمالي المسافة (${formatDecimal(totalKm)} كم) أقل من الحد الأدنى (${formatDecimal(policy.minTotalKm)} كم)",
                detailEn = "Total distance (${formatDecimal(totalKm)} km) is below minimum limit (${formatDecimal(policy.minTotalKm)} km)"
            )
        }

        // TOTAL_TOO_LONG (totalKm > maxTotalKm)
        if (totalKm != null && policy.maxTotalKm != null && totalKm > policy.maxTotalKm) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.TOTAL_TOO_LONG,
                titleAr = "إجمالي مسافة كبير",
                titleEn = "Total Distance Too High",
                detailAr = "إجمالي المسافة (${formatDecimal(totalKm)} كم) يتجاوز الحد الأقصى (${formatDecimal(policy.maxTotalKm)} كم)",
                detailEn = "Total distance (${formatDecimal(totalKm)} km) exceeds maximum limit (${formatDecimal(policy.maxTotalKm)} km)"
            )
        }

        // RATING_LOW (riderRating < minRiderRating)
        if (req.riderRating != null && policy.minRiderRating != null && req.riderRating < policy.minRiderRating) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.RATING_LOW,
                titleAr = "تقييم منخفض",
                titleEn = "Low Rating",
                detailAr = "تقييم الراكب (${formatDecimal(req.riderRating)}) أقل من الحد الأدنى (${formatDecimal(policy.minRiderRating)})",
                detailEn = "Rider rating (${formatDecimal(req.riderRating)}) is below minimum threshold (${formatDecimal(policy.minRiderRating)})"
            )
        }

        // TRIPS_FEW (riderTrips < minRiderTrips)
        if (req.riderTrips != null && policy.minRiderTrips != null && req.riderTrips < policy.minRiderTrips) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.TRIPS_FEW,
                titleAr = "رحلات قليلة",
                titleEn = "Few Trips",
                detailAr = "عدد رحلات الراكب السابقة (${req.riderTrips}) غير كافٍ (الحد: ${policy.minRiderTrips})",
                detailEn = "Rider completed trips (${req.riderTrips}) is below minimum (${policy.minRiderTrips})"
            )
        }

        // REQUEST_OLD (age > maxRequestAgeMinutes)
        if (req.requestAgeSeconds != null && policy.maxRequestAgeMinutes != null &&
            req.requestAgeSeconds > (policy.maxRequestAgeMinutes * 60)
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.REQUEST_OLD,
                titleAr = "طلب قديم",
                titleEn = "Request Expired",
                detailAr = "الطلب مر عليه وقت طويل منذ إرساله (${req.requestAgeSeconds / 60} دقيقة)",
                detailEn = "Request is too old (${req.requestAgeSeconds / 60} min elapsed)"
            )
        }

        // PAYMENT_NOT_ALLOWED
        if (policy.allowedPayments.isNotEmpty() && req.paymentMethod != null &&
            !policy.allowedPayments.contains(req.paymentMethod)
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.PAYMENT_NOT_ALLOWED,
                titleAr = "طريقة دفع مرفوضة",
                titleEn = "Payment Not Allowed",
                detailAr = "طريقة الدفع (${req.paymentMethod}) غير مسموح بها في إعداداتك",
                detailEn = "Payment method (${req.paymentMethod}) is not allowed by your rules"
            )
        }

        // NOTE_BANNED_WORD (normalized Arabic/English match)
        if (policy.bannedNoteWords.isNotEmpty() && req.riderNote != null &&
            TextNormalizer.containsBannedWord(req.riderNote, policy.bannedNoteWords)
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.NOTE_BANNED_WORD,
                titleAr = "ملاحظة محظورة",
                titleEn = "Banned Note Word",
                detailAr = "ملاحظة الراكب تحتوي على كلمات محظورة في إعداداتك",
                detailEn = "Rider note contains a banned keyword"
            )
        }

        // AREA_AVOIDED (pickup or destination matches an avoided area)
        if (policy.avoidedAreas.isNotEmpty() &&
            (TextNormalizer.matchesAnyArea(req.pickupAddress, policy.avoidedAreas) ||
             TextNormalizer.matchesAnyArea(req.destinationAddress, policy.avoidedAreas))
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.AREA_AVOIDED,
                titleAr = "منطقة محظورة",
                titleEn = "Avoided Area",
                detailAr = "نقطة الانطلاق أو الوجهة تقع في منطقة محظورة",
                detailEn = "Pickup or destination is in an avoided area"
            )
        }

        // STOPS_AVOID (stopsMode AVOID and extraStops > 0)
        if (policy.stopsMode == StopsMode.AVOID && req.extraStops > 0) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.STOPS_AVOID,
                titleAr = "وقفات مرفوضة",
                titleEn = "Extra Stops Avoided",
                detailAr = "الطلب يحتوي على ${req.extraStops} وقفات إضافية وقاعدتك ترفض الوقفات",
                detailEn = "Request has ${req.extraStops} extra stop(s) and your policy avoids stops"
            )
        }

        // LONG_TRIP_AVOID (longTripMode AVOID and tripKm >= longTripKm)
        val tripDistanceForLongCheck = req.tripKm ?: totalKm
        if (policy.longTripMode == LongTripMode.AVOID && policy.longTripKm != null &&
            tripDistanceForLongCheck != null && tripDistanceForLongCheck >= policy.longTripKm
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.LONG_TRIP_AVOID,
                titleAr = "سفر مرفوض",
                titleEn = "Long Trip Avoided",
                detailAr = "مسافة المشوار (${formatDecimal(tripDistanceForLongCheck)} كم) تعتبر سفراً وقاعدتك تتجنب السفر",
                detailEn = "Trip distance (${formatDecimal(tripDistanceForLongCheck)} km) qualifies as a long trip and your policy avoids it"
            )
        }

        // PRICE_LOW (measured < minPricePerKm, more than 5% below)
        // If measuredValue is within 5% below (i.e. >= 0.95 * minPricePerKm), it's handled as ORANGE PRICE_BORDERLINE in step 4
        if (measuredValue != null && policy.minPricePerKm > 0.0 &&
            measuredValue < (policy.minPricePerKm * 0.95)
        ) {
            return createVerdict(
                tone = Tone.RED,
                reasonCode = ReasonCodes.PRICE_LOW,
                titleAr = "السعر ضعيف",
                titleEn = "Price Too Low",
                detailAr = "السعر (${formatDecimal(measuredValue)} ج/كم) أقل من الحد الأدنى المطلوب (${formatDecimal(policy.minPricePerKm)})",
                detailEn = "Rate (${formatDecimal(measuredValue)}/km) is below minimum required (${formatDecimal(policy.minPricePerKm)})"
            )
        }

        // 4. ORANGE (think first)
        // STOPS_WARN (stopsMode WARN and extraStops > 0)
        if (policy.stopsMode == StopsMode.WARN && req.extraStops > 0) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.STOPS_WARN,
                titleAr = "وقفات إضافية",
                titleEn = "Extra Stops",
                detailAr = "الطلب يحتوي على ${req.extraStops} وقفات إضافية تحتاج تأكيد",
                detailEn = "Request contains ${req.extraStops} extra stops that need review"
            )
        }

        // LONG_TRIP_WARN (longTripMode WARN and tripKm >= longTripKm)
        if (policy.longTripMode == LongTripMode.WARN && policy.longTripKm != null &&
            tripDistanceForLongCheck != null && tripDistanceForLongCheck >= policy.longTripKm
        ) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.LONG_TRIP_WARN,
                titleAr = "مشوار طويل",
                titleEn = "Long Trip",
                detailAr = "مشوار طويل (${formatDecimal(tripDistanceForLongCheck)} كم) يتجاوز الحد المحدد (${formatDecimal(policy.longTripKm)} كم)",
                detailEn = "Long trip (${formatDecimal(tripDistanceForLongCheck)} km) exceeding your threshold (${formatDecimal(policy.longTripKm)} km)"
            )
        }

        // UBER_SAVER (warnOnUberSaver and product is Saver)
        if (policy.warnOnUberSaver && isUberSaver(req.productName)) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.UBER_SAVER,
                titleAr = "أوبر سيفر",
                titleEn = "Uber Saver",
                detailAr = "طلب أوبر سيفر بتسعيرة منخفضة",
                detailEn = "Uber Saver request with discounted fare"
            )
        }

        // PRICE_BORDERLINE (a value within 5% below the price limit)
        if (measuredValue != null && policy.minPricePerKm > 0.0 &&
            measuredValue < policy.minPricePerKm && measuredValue >= (policy.minPricePerKm * 0.95)
        ) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.PRICE_BORDERLINE,
                titleAr = "سعر قريب من الحد",
                titleEn = "Borderline Price",
                detailAr = "السعر (${formatDecimal(measuredValue)} ج/كم) أقل من الحد بنسبة بسيطة (ضمن 5%)",
                detailEn = "Price (${formatDecimal(measuredValue)}/km) is slightly below limit (within 5%)"
            )
        }

        // LOW_CONFIDENCE (low confidence < 0.6 on a decisive field)
        if (req.confidence < 0.6f) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.LOW_CONFIDENCE,
                titleAr = "قراءة غير مؤكدة",
                titleEn = "Low Confidence",
                detailAr = "دقة قراءة الشاشة منخفضة (${(req.confidence * 100).toInt()}%)، راجع الأرقام بنفسك",
                detailEn = "Screen reading confidence is low (${(req.confidence * 100).toInt()}%), check manually"
            )
        }

        // AREA_UNKNOWN (when governorate is not set but areas are configured)
        if (policy.governorate.isNullOrBlank() &&
            (policy.avoidedAreas.isNotEmpty() || policy.preferredAreas.isNotEmpty())
        ) {
            return createVerdict(
                tone = Tone.ORANGE,
                reasonCode = ReasonCodes.AREA_UNKNOWN,
                titleAr = "المحافظة غير محددة",
                titleEn = "Governorate Not Set",
                detailAr = "المحافظة غير محددة مع وجود مناطق مخصصة في الإعدادات",
                detailEn = "Governorate is not set while custom areas are configured"
            )
        }

        // 5. GREEN otherwise
        val rateFormatted = if (measuredValue != null) formatDecimal(measuredValue) else "-"
        return createVerdict(
            tone = Tone.GREEN,
            reasonCode = ReasonCodes.ACCEPTABLE,
            titleAr = "طلب ممتاز",
            titleEn = "Great Request",
            detailAr = "الطلب يطابق شروطك وسعره مناسب ($rateFormatted ج/كم)",
            detailEn = "Request matches your rules with acceptable price ($rateFormatted/km)"
        )
    }

    /**
     * Calculates the suggested counter-offer price:
     * Smallest price that makes the measured value reach policy.minPricePerKm (invert the formula),
     * rounded UP to a multiple of policy.negotiationStep, capped by policy.maxOffer;
     * null if already satisfied or impossible.
     */
    private fun calculateSuggestedPrice(
        req: ParsedRequest,
        policy: Policy,
        totalKm: Double?,
        billableKm: Double?,
        carCost: Double?,
        stopCost: Double,
        commissionRate: Double,
        measuredValue: Double?
    ): Double? {
        val targetMinPrice = policy.minPricePerKm
        if (targetMinPrice <= 0.0) return null

        // If already satisfied, no suggested counter-offer needed
        if (measuredValue != null && measuredValue >= targetMinPrice) {
            return null
        }

        // Invert the formula to find the minimum raw required price P_req
        val rawRequiredPrice: Double = if (!policy.basisNet) {
            // Gross: grossPerKm = P / billableKm >= targetMinPrice => P >= targetMinPrice * billableKm
            if (billableKm == null || billableKm <= 0.0) return null
            targetMinPrice * billableKm
        } else {
            // Net: netPerKm = (P * (1 - commissionRate) - carCost - stopCost) / totalKm >= targetMinPrice
            // P * (1 - commissionRate) >= targetMinPrice * totalKm + carCost + stopCost
            if (totalKm == null || totalKm <= 0.0 || carCost == null || commissionRate >= 1.0) return null
            val effectiveCarCost = carCost
            (targetMinPrice * totalKm + effectiveCarCost + stopCost) / (1.0 - commissionRate)
        }

        // If already satisfied by current fare
        if (req.fare != null && req.fare >= rawRequiredPrice) {
            return null
        }

        // Check if impossible under maxOffer cap
        if (policy.maxOffer != null && rawRequiredPrice > policy.maxOffer) {
            return null
        }

        // Round UP to a multiple of policy.negotiationStep
        val step = if (policy.negotiationStep > 0) policy.negotiationStep else 1
        val roundedPrice = ceil(rawRequiredPrice / step.toDouble()) * step.toDouble()

        // Cap by policy.maxOffer
        val cappedPrice = if (policy.maxOffer != null) {
            min(roundedPrice, policy.maxOffer)
        } else {
            roundedPrice
        }

        // If capped price is below raw required price (i.e. cap prevented reaching minPricePerKm), impossible
        if (cappedPrice < rawRequiredPrice) {
            return null
        }

        // Ensure suggested counter-offer is strictly greater than the current fare
        if (req.fare != null && cappedPrice <= req.fare) {
            return null
        }

        return cappedPrice
    }

    /**
     * Checks if product indicates Uber Saver.
     */
    private fun isUberSaver(product: String?): Boolean {
        if (product.isNullOrBlank()) return false
        val norm = TextNormalizer.normalize(product)
        return norm.contains("saver") || norm.contains("سيفر") || norm.contains("اوبر سيفر")
    }

    /**
     * Checks if destination is heading toward home.
     */
    private fun isHeadingHome(
        req: ParsedRequest,
        home: HomeWayState,
        geoResolver: GeoResolver
    ): Boolean {
        if (!home.enabled || home.lat == null || home.lon == null) return false
        val destAddr = req.destinationAddress ?: return false
        val destCoord = geoResolver.resolve(destAddr) ?: return false

        val pickupAddr = req.pickupAddress ?: return false
        val originCoord = geoResolver.resolve(pickupAddr) ?: return false

        val homeCoord = LatLon(home.lat, home.lon)
        return GeoUtils.isTowardHome(originCoord, destCoord, homeCoord)
    }

    /**
     * Rounds a double to 1 decimal place without platform-dependent locale formatting.
     */
    private fun formatDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()}"
        } else {
            "$rounded"
        }
    }
}
