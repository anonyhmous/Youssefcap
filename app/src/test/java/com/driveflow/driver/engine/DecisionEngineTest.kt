package com.driveflow.driver.engine

import android.graphics.Rect
import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class DecisionEngineTest {

    private fun sampleRequest(
        app: RideApp = RideApp.INDRIVE,
        fare: Double? = 100.0,
        currency: String? = "EGP",
        pickupKm: Double? = 1.0,
        tripKm: Double? = 9.0,
        totalKm: Double? = 10.0,
        pickupMinutes: Int? = 5,
        riderRating: Double? = 4.9,
        riderTrips: Int? = 50,
        requestAgeSeconds: Int? = 30,
        pickupAddress: String? = "المعادي",
        destinationAddress: String? = "مدينة نصر",
        extraStops: Int = 0,
        paymentMethod: String? = "CASH",
        riderNote: String? = null,
        productName: String? = "inDrive",
        isDetailOpen: Boolean = true,
        confidence: Float = 0.95f
    ): ParsedRequest {
        return ParsedRequest(
            app = app,
            key = "req_test",
            source = ReadSource.ACCESSIBILITY,
            fare = fare,
            currency = currency,
            pickupKm = pickupKm,
            tripKm = tripKm,
            totalKm = totalKm,
            pickupMinutes = pickupMinutes,
            riderRating = riderRating,
            riderTrips = riderTrips,
            requestAgeSeconds = requestAgeSeconds,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress,
            extraStops = extraStops,
            paymentMethod = paymentMethod,
            riderNote = riderNote,
            offerCount = null,
            roadState = RoadState.EASY,
            productName = productName,
            bounds = Rect(0, 0, 1080, 200),
            isDetailOpen = isDetailOpen,
            confidence = confidence
        )
    }

    private fun samplePolicy(
        minPricePerKm: Double = 8.0,
        governorate: String? = "cairo"
    ): Policy {
        return Policy(
            minPricePerKm = minPricePerKm,
            governorate = governorate,
            currency = "EGP",
            appEnabled = mapOf(
                RideApp.INDRIVE to true,
                RideApp.UBER to true,
                RideApp.DIDI to true
            )
        )
    }

    // =========================================================================
    // 1. One test per ReasonCode (All 23 reason codes)
    // =========================================================================

    @Test
    fun reasonCode_1_APP_DISABLED() {
        val policy = samplePolicy().copy(
            appEnabled = mapOf(RideApp.INDRIVE to false)
        )
        val verdict = DecisionEngine.decide(sampleRequest(app = RideApp.INDRIVE), policy)
        assertEquals(Tone.GRAY, verdict.tone)
        assertEquals(ReasonCodes.APP_DISABLED, verdict.reasonCode)
        assertEquals("التطبيق معطل", verdict.titleAr)
        assertEquals("App Disabled", verdict.titleEn)
    }

    @Test
    fun reasonCode_2a_DATA_MISSING_nullFare() {
        val policy = samplePolicy()
        val verdict = DecisionEngine.decide(sampleRequest(fare = null), policy)
        assertEquals(Tone.GRAY, verdict.tone)
        assertEquals(ReasonCodes.DATA_MISSING, verdict.reasonCode)
        assertEquals("بيانات ناقصة", verdict.titleAr)
        assertEquals("Data Missing", verdict.titleEn)
    }

    @Test
    fun reasonCode_2b_DATA_MISSING_distanceMissingAndDetailNotAvailable() {
        val policy = samplePolicy()
        val req = sampleRequest(fare = 80.0, totalKm = null, tripKm = null, pickupKm = 2.0, isDetailOpen = false)
        val context = DecisionContext(detailAvailable = false)
        val verdict = DecisionEngine.decide(req, policy, context = context)
        assertEquals(Tone.GRAY, verdict.tone)
        assertEquals(ReasonCodes.DATA_MISSING, verdict.reasonCode)
        assertTrue(verdict.detailAr.contains("مسافة المشوار"))
    }

    @Test
    fun reasonCode_3_PICKUP_TOO_FAR() {
        val policy = samplePolicy().copy(maxPickupKm = 3.0)
        val req = sampleRequest(pickupKm = 4.5)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.PICKUP_TOO_FAR, verdict.reasonCode)
        assertEquals("بيك أب بعيد", verdict.titleAr)
    }

    @Test
    fun reasonCode_4_PICKUP_TOO_LONG() {
        val policy = samplePolicy().copy(maxPickupMinutes = 10)
        val req = sampleRequest(pickupMinutes = 15)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.PICKUP_TOO_LONG, verdict.reasonCode)
        assertEquals("وقت وصول طويل", verdict.titleAr)
    }

    @Test
    fun reasonCode_5_TRIP_TOO_LONG() {
        val policy = samplePolicy().copy(maxTripKm = 25.0)
        val req = sampleRequest(tripKm = 32.0, totalKm = 35.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.TRIP_TOO_LONG, verdict.reasonCode)
        assertEquals("مشوار طويل جداً", verdict.titleAr)
    }

    @Test
    fun reasonCode_6_TOTAL_TOO_SHORT() {
        val policy = samplePolicy().copy(minTotalKm = 5.0)
        val req = sampleRequest(totalKm = 3.2, tripKm = 2.5, pickupKm = 0.7)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.TOTAL_TOO_SHORT, verdict.reasonCode)
        assertEquals("مشوار قصير جداً", verdict.titleAr)
    }

    @Test
    fun reasonCode_7_TOTAL_TOO_LONG() {
        val policy = samplePolicy().copy(maxTotalKm = 40.0)
        val req = sampleRequest(totalKm = 45.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.TOTAL_TOO_LONG, verdict.reasonCode)
        assertEquals("إجمالي مسافة كبير", verdict.titleAr)
    }

    @Test
    fun reasonCode_8_RATING_LOW() {
        val policy = samplePolicy().copy(minRiderRating = 4.75)
        val req = sampleRequest(riderRating = 4.60)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.RATING_LOW, verdict.reasonCode)
        assertEquals("تقييم منخفض", verdict.titleAr)
    }

    @Test
    fun reasonCode_9_TRIPS_FEW() {
        val policy = samplePolicy().copy(minRiderTrips = 20)
        val req = sampleRequest(riderTrips = 5)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.TRIPS_FEW, verdict.reasonCode)
        assertEquals("رحلات قليلة", verdict.titleAr)
    }

    @Test
    fun reasonCode_10_REQUEST_OLD() {
        val policy = samplePolicy().copy(maxRequestAgeMinutes = 2)
        val req = sampleRequest(requestAgeSeconds = 180) // 3 minutes
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.REQUEST_OLD, verdict.reasonCode)
        assertEquals("طلب قديم", verdict.titleAr)
    }

    @Test
    fun reasonCode_11_PAYMENT_NOT_ALLOWED() {
        val policy = samplePolicy().copy(allowedPayments = setOf("CASH"))
        val req = sampleRequest(paymentMethod = "WALLET")
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.PAYMENT_NOT_ALLOWED, verdict.reasonCode)
        assertEquals("طريقة دفع مرفوضة", verdict.titleAr)
    }

    @Test
    fun reasonCode_12_NOTE_BANNED_WORD() {
        val policy = samplePolicy().copy(bannedNoteWords = listOf("شحنة", "طرد", "كرتونة"))
        val req = sampleRequest(riderNote = "معايا طرد صغير وكرتونة")
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.NOTE_BANNED_WORD, verdict.reasonCode)
        assertEquals("ملاحظة محظورة", verdict.titleAr)
    }

    @Test
    fun reasonCode_13_AREA_AVOIDED() {
        val policy = samplePolicy().copy(avoidedAreas = listOf("شبرا", "المطرية"))
        val req = sampleRequest(destinationAddress = "المطرية بجوار المحطة")
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.AREA_AVOIDED, verdict.reasonCode)
        assertEquals("منطقة محظورة", verdict.titleAr)
    }

    @Test
    fun reasonCode_14_STOPS_AVOID() {
        val policy = samplePolicy().copy(stopsMode = StopsMode.AVOID)
        val req = sampleRequest(extraStops = 1)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.STOPS_AVOID, verdict.reasonCode)
        assertEquals("وقفات مرفوضة", verdict.titleAr)
    }

    @Test
    fun reasonCode_15_LONG_TRIP_AVOID() {
        val policy = samplePolicy().copy(
            longTripMode = LongTripMode.AVOID,
            longTripKm = 30.0
        )
        val req = sampleRequest(tripKm = 35.0, totalKm = 38.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.LONG_TRIP_AVOID, verdict.reasonCode)
        assertEquals("سفر مرفوض", verdict.titleAr)
    }

    @Test
    fun reasonCode_16_PRICE_LOW() {
        // minPricePerKm = 10.0. 95% threshold = 9.50.
        // totalKm = 10.0, fare = 70.0 => grossPerKm = 7.0 (< 9.50) => PRICE_LOW
        val policy = samplePolicy(minPricePerKm = 10.0)
        val req = sampleRequest(fare = 70.0, totalKm = 10.0, pickupKm = 1.0, tripKm = 9.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.RED, verdict.tone)
        assertEquals(ReasonCodes.PRICE_LOW, verdict.reasonCode)
        assertEquals("السعر ضعيف", verdict.titleAr)
        assertNotNull(verdict.suggestedPrice)
        assertEquals(100.0, verdict.suggestedPrice!!, 0.01) // 10.0 * 10km = 100
    }

    @Test
    fun reasonCode_17_STOPS_WARN() {
        val policy = samplePolicy().copy(stopsMode = StopsMode.WARN)
        val req = sampleRequest(extraStops = 2)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.STOPS_WARN, verdict.reasonCode)
        assertEquals("وقفات إضافية", verdict.titleAr)
    }

    @Test
    fun reasonCode_18_LONG_TRIP_WARN() {
        val policy = samplePolicy().copy(
            longTripMode = LongTripMode.WARN,
            longTripKm = 25.0
        )
        val req = sampleRequest(tripKm = 28.0, totalKm = 30.0, fare = 350.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.LONG_TRIP_WARN, verdict.reasonCode)
        assertEquals("مشوار طويل", verdict.titleAr)
    }

    @Test
    fun reasonCode_19_UBER_SAVER() {
        val policy = samplePolicy().copy(warnOnUberSaver = true)
        val req = sampleRequest(productName = "Uber Saver", fare = 150.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.UBER_SAVER, verdict.reasonCode)
        assertEquals("أوبر سيفر", verdict.titleAr)
    }

    @Test
    fun reasonCode_20_PRICE_BORDERLINE() {
        // minPricePerKm = 10.0. 95% = 9.50.
        // totalKm = 10.0, fare = 96.0 => rate = 9.60 (between 9.50 and 10.0) => PRICE_BORDERLINE
        val policy = samplePolicy(minPricePerKm = 10.0)
        val req = sampleRequest(fare = 96.0, totalKm = 10.0, pickupKm = 0.0, tripKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.PRICE_BORDERLINE, verdict.reasonCode)
        assertEquals("سعر قريب من الحد", verdict.titleAr)
        assertNotNull(verdict.suggestedPrice)
        assertEquals(100.0, verdict.suggestedPrice!!, 0.01)
    }

    @Test
    fun reasonCode_21_LOW_CONFIDENCE() {
        val policy = samplePolicy()
        val req = sampleRequest(confidence = 0.45f)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.LOW_CONFIDENCE, verdict.reasonCode)
        assertEquals("قراءة غير مؤكدة", verdict.titleAr)
    }

    @Test
    fun reasonCode_22_AREA_UNKNOWN() {
        // Governorate is null, but avoidedAreas is configured
        val policy = samplePolicy(governorate = null).copy(
            avoidedAreas = listOf("المطرية")
        )
        val req = sampleRequest(pickupAddress = "التجمع الخامس", destinationAddress = "المعادي")
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.ORANGE, verdict.tone)
        assertEquals(ReasonCodes.AREA_UNKNOWN, verdict.reasonCode)
        assertEquals("المحافظة غير محددة", verdict.titleAr)
    }

    @Test
    fun reasonCode_23_ACCEPTABLE() {
        val policy = samplePolicy(minPricePerKm = 8.0)
        val req = sampleRequest(fare = 120.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.GREEN, verdict.tone)
        assertEquals(ReasonCodes.ACCEPTABLE, verdict.reasonCode)
        assertEquals("طلب ممتاز", verdict.titleAr)
        assertEquals("Great Request", verdict.titleEn)
        assertNull(verdict.suggestedPrice) // Already satisfied
    }

    // =========================================================================
    // 2. Order of Precedence Tests
    // =========================================================================

    @Test
    fun precedence_APP_DISABLED_beats_allOtherFailures() {
        val policy = samplePolicy().copy(
            appEnabled = mapOf(RideApp.INDRIVE to false),
            maxPickupKm = 1.0 // will also violate
        )
        // Request with missing fare, long pickup, avoided area
        val req = sampleRequest(
            app = RideApp.INDRIVE,
            fare = null,
            pickupKm = 10.0,
            destinationAddress = "منطقة محظورة"
        )
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.APP_DISABLED, verdict.reasonCode)
    }

    @Test
    fun precedence_DATA_MISSING_beats_hardReds_whenFareNull() {
        val policy = samplePolicy().copy(maxPickupKm = 2.0)
        val req = sampleRequest(fare = null, pickupKm = 8.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.DATA_MISSING, verdict.reasonCode)
    }

    @Test
    fun precedence_PICKUP_TOO_FAR_beats_RATING_LOW_and_PRICE_LOW() {
        val policy = samplePolicy(minPricePerKm = 15.0).copy(
            maxPickupKm = 2.0,
            minRiderRating = 4.8
        )
        val req = sampleRequest(
            pickupKm = 5.0, // Fails
            riderRating = 4.2, // Fails
            fare = 30.0, // Fails price
            totalKm = 10.0
        )
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.PICKUP_TOO_FAR, verdict.reasonCode)
    }

    @Test
    fun precedence_PAYMENT_NOT_ALLOWED_beats_PRICE_LOW() {
        val policy = samplePolicy(minPricePerKm = 20.0).copy(
            allowedPayments = setOf("CASH")
        )
        val req = sampleRequest(paymentMethod = "CARD", fare = 20.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.PAYMENT_NOT_ALLOWED, verdict.reasonCode)
    }

    @Test
    fun precedence_HardRed_beats_OrangeWarnings() {
        val policy = samplePolicy(minPricePerKm = 8.0).copy(
            maxPickupKm = 3.0,
            stopsMode = StopsMode.WARN,
            warnOnUberSaver = true
        )
        val req = sampleRequest(
            pickupKm = 5.0, // Hard RED
            extraStops = 1, // ORANGE
            productName = "Uber Saver" // ORANGE
        )
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.PICKUP_TOO_FAR, verdict.reasonCode)
        assertEquals(Tone.RED, verdict.tone)
    }

    @Test
    fun precedence_STOPS_WARN_beats_PRICE_BORDERLINE_and_LOW_CONFIDENCE() {
        val policy = samplePolicy(minPricePerKm = 10.0).copy(
            stopsMode = StopsMode.WARN
        )
        val req = sampleRequest(
            extraStops = 1,
            fare = 98.0, // Price borderline (9.8 per km)
            totalKm = 10.0,
            confidence = 0.5f // Low confidence
        )
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(ReasonCodes.STOPS_WARN, verdict.reasonCode)
        assertEquals(Tone.ORANGE, verdict.tone)
    }

    // =========================================================================
    // 3. Free Pickup Math & Billable Km Tests
    // =========================================================================

    @Test
    fun freePickupMath_deductsUpToAllowance_andSetsFlag() {
        val policy = samplePolicy(minPricePerKm = 10.0).copy(
            freePickupKm = 2.0 // First 2km of pickup are free
        )
        // totalKm = 10 (pickup 3km + trip 7km)
        // deduction = min(3.0, 2.0) = 2.0 km
        // billableKm = 10 - 2 = 8.0 km
        // grossPerKm = 80 / 8.0 = 10.0 => Meets minPricePerKm!
        val req = sampleRequest(fare = 80.0, pickupKm = 3.0, tripKm = 7.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(Tone.GREEN, verdict.tone)
        assertEquals(ReasonCodes.ACCEPTABLE, verdict.reasonCode)
        assertEquals(10.0, verdict.grossPerKm!!, 0.01)
        assertTrue(verdict.flags.contains(DecisionFlags.FREE_PICKUP_USED))
    }

    @Test
    fun freePickupMath_whenPickupSmallerThanAllowance_deductsOnlyPickup() {
        val policy = samplePolicy().copy(freePickupKm = 3.0)
        // pickup is only 1.0 km => deduction = 1.0 km
        // totalKm = 6.0 => billableKm = 5.0
        val req = sampleRequest(fare = 50.0, pickupKm = 1.0, tripKm = 5.0, totalKm = 6.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(10.0, verdict.grossPerKm!!, 0.01) // 50 / 5 = 10.0
        assertTrue(verdict.flags.contains(DecisionFlags.FREE_PICKUP_USED))
    }

    @Test
    fun freePickupMath_whenFreePickupZeroOrNull_noDeductionAndNoFlag() {
        val policy = samplePolicy().copy(freePickupKm = null)
        val req = sampleRequest(fare = 100.0, pickupKm = 2.0, tripKm = 8.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(10.0, verdict.grossPerKm!!, 0.01) // 100 / 10 = 10.0
        assertFalse(verdict.flags.contains(DecisionFlags.FREE_PICKUP_USED))
    }

    // =========================================================================
    // 4. Commission & Basis Net Tests
    // =========================================================================

    @Test
    fun commission_whenDisabled_deductsZeroCommission() {
        val policy = samplePolicy().copy(
            basisNet = true,
            commissionEnabled = mapOf(RideApp.INDRIVE to false),
            commissionPct = mapOf(RideApp.INDRIVE to 15.0),
            fuelCostPerKm = 1.0,
            upkeepCostPerKm = 0.5
        )
        // totalKm = 10, carCost = 1.5 * 10 = 15.0
        // fare = 115 => commission = 0 => netProfit = 115 - 0 - 15 = 100.0 => netPerKm = 10.0
        val req = sampleRequest(app = RideApp.INDRIVE, fare = 115.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(100.0, verdict.netProfit!!, 0.01)
        assertEquals(10.0, verdict.netPerKm!!, 0.01)
        assertTrue(verdict.flags.contains(DecisionFlags.NET_BASIS))
    }

    @Test
    fun commission_whenEnabled_deductsPercentagePerApp() {
        val policy = samplePolicy().copy(
            basisNet = true,
            commissionEnabled = mapOf(RideApp.UBER to true),
            commissionPct = mapOf(RideApp.UBER to 20.0), // 20%
            fuelCostPerKm = 0.0,
            upkeepCostPerKm = 0.0
        )
        // fare = 100 => commission = 20 => netProfit = 80 => netPerKm = 8.0 (10 km)
        val req = sampleRequest(app = RideApp.UBER, fare = 100.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(80.0, verdict.netProfit!!, 0.01)
        assertEquals(8.0, verdict.netPerKm!!, 0.01)
    }

    @Test
    fun commission_withExtraStopsFee_deductsStopCost() {
        val policy = samplePolicy().copy(
            basisNet = true,
            commissionEnabled = mapOf(RideApp.DIDI to false),
            stopFee = 10.0,
            fuelCostPerKm = 0.0,
            upkeepCostPerKm = 0.0
        )
        // fare = 100, 2 extra stops with 10 EGP fee = 20 stop cost
        // netProfit = 100 - 20 = 80
        val req = sampleRequest(app = RideApp.DIDI, fare = 100.0, totalKm = 10.0, extraStops = 2)
        val verdict = DecisionEngine.decide(req, policy)
        assertEquals(80.0, verdict.netProfit!!, 0.01)
    }

    // =========================================================================
    // 5. Borderline 5% Boundary Table-Driven Tests
    // =========================================================================

    @Test
    fun borderline_5Percent_boundary_exactCases() {
        val policy = samplePolicy(minPricePerKm = 10.0) // 95% = 9.50
        val testCases = listOf(
            Triple(9.49, Tone.RED, ReasonCodes.PRICE_LOW),         // Just below 95%
            Triple(9.50, Tone.ORANGE, ReasonCodes.PRICE_BORDERLINE), // Exactly 95%
            Triple(9.51, Tone.ORANGE, ReasonCodes.PRICE_BORDERLINE), // Inside 5% band
            Triple(9.90, Tone.ORANGE, ReasonCodes.PRICE_BORDERLINE), // Near upper edge
            Triple(10.00, Tone.GREEN, ReasonCodes.ACCEPTABLE),       // Exactly 100%
            Triple(10.50, Tone.GREEN, ReasonCodes.ACCEPTABLE)        // Above threshold
        )

        for ((rate, expectedTone, expectedReason) in testCases) {
            val req = sampleRequest(fare = rate * 10.0, totalKm = 10.0, pickupKm = 0.0, tripKm = 10.0)
            val verdict = DecisionEngine.decide(req, policy)
            assertEquals("Failed for rate: $rate", expectedTone, verdict.tone)
            assertEquals("Failed reason for rate: $rate", expectedReason, verdict.reasonCode)
        }
    }

    // =========================================================================
    // 6. Suggested Price: Inversion, Rounding to Step 5, and MaxOffer Cap
    // =========================================================================

    @Test
    fun suggestedPrice_grossMode_roundsUpToStep5() {
        val policy = samplePolicy(minPricePerKm = 10.0).copy(
            negotiationStep = 5,
            basisNet = false
        )
        // billableKm = 8.0, target = 10.0 => raw required P_req = 80.0
        // Current fare = 50.0 (rate = 6.25 < 10.0)
        // 80 is multiple of 5 => suggestedPrice = 80.0
        val req1 = sampleRequest(fare = 50.0, pickupKm = 0.0, tripKm = 8.0, totalKm = 8.0)
        val verdict1 = DecisionEngine.decide(req1, policy)
        assertEquals(80.0, verdict1.suggestedPrice!!, 0.01)

        // billableKm = 8.2, target = 10.0 => raw required P_req = 82.0
        // Rounded UP to multiple of 5 => 85.0
        val req2 = sampleRequest(fare = 50.0, pickupKm = 0.0, tripKm = 8.2, totalKm = 8.2)
        val verdict2 = DecisionEngine.decide(req2, policy)
        assertEquals(85.0, verdict2.suggestedPrice!!, 0.01)
    }

    @Test
    fun suggestedPrice_netMode_accountsForCommissionAndCarCost() {
        val policy = samplePolicy(minPricePerKm = 10.0).copy(
            basisNet = true,
            commissionEnabled = mapOf(RideApp.INDRIVE to true),
            commissionPct = mapOf(RideApp.INDRIVE to 20.0), // 20% commission => (1 - 0.2) = 0.8
            fuelCostPerKm = 2.0,
            upkeepCostPerKm = 0.0,
            negotiationStep = 5
        )
        // totalKm = 10, carCost = 2 * 10 = 20.0
        // P_req = (10 * 10 + 20) / 0.8 = 120 / 0.8 = 150.0
        val req = sampleRequest(app = RideApp.INDRIVE, fare = 80.0, totalKm = 10.0)
        val verdict = DecisionEngine.decide(req, policy)
        assertNotNull(verdict.suggestedPrice)
        assertEquals(150.0, verdict.suggestedPrice!!, 0.01)
    }

    @Test
    fun suggestedPrice_cappedByMaxOffer() {
        val policy = samplePolicy(minPricePerKm = 10.0).copy(
            negotiationStep = 5,
            maxOffer = 90.0
        )
        // billableKm = 8.2 => raw = 82.0 => rounded = 85.0 <= maxOffer(90.0) => 85.0
        val req1 = sampleRequest(fare = 50.0, totalKm = 8.2, pickupKm = 0.0, tripKm = 8.2)
        assertEquals(85.0, DecisionEngine.decide(req1, policy).suggestedPrice!!, 0.01)

        // billableKm = 12.0 => raw = 120.0 > maxOffer(90.0) => impossible to reach minPrice => null
        val req2 = sampleRequest(fare = 50.0, totalKm = 12.0, pickupKm = 0.0, tripKm = 12.0)
        assertNull(DecisionEngine.decide(req2, policy).suggestedPrice)
    }

    @Test
    fun suggestedPrice_whenAlreadySatisfied_isNull() {
        val policy = samplePolicy(minPricePerKm = 8.0)
        val req = sampleRequest(fare = 150.0, totalKm = 10.0) // 15/km >= 8/km
        val verdict = DecisionEngine.decide(req, policy)
        assertNull(verdict.suggestedPrice)
    }

    // =========================================================================
    // 7. Text Normalization & Area / Banned Words Matching Tests
    // =========================================================================

    @Test
    fun textNormalizer_unifiesArabicAlefsAndTashkeel() {
        assertEquals("القاهره", TextNormalizer.normalize("القَاهِرَةُ"))
        assertEquals("اكتوبر", TextNormalizer.normalize("أكْتُوبَر"))
        assertEquals("المعادي", TextNormalizer.normalize("المَعَادِي"))
        assertEquals("حي الزمالك", TextNormalizer.normalize("حَيّ ٱلزَّمَالِك"))
        assertEquals("مستشفي", TextNormalizer.normalize("مُسْتَشْفَى")) // ى -> ي
        assertEquals("مستشفي", TextNormalizer.normalize("مستشفى"))
    }

    @Test
    fun textNormalizer_handlesTatweelAndLatin() {
        assertEquals("معادي", TextNormalizer.normalize("مــــعـــــادي"))
        assertEquals("cairo festival city", TextNormalizer.normalize("  Cairo   Festival  City  "))
    }

    @Test
    fun textNormalizer_matchesAreaInBothDirections() {
        // Minimum length 3
        assertTrue(TextNormalizer.matchesArea("شارع 9 المعادي الجديدة", "المعادي"))
        assertTrue(TextNormalizer.matchesArea("المعادي", "المعادي الجديدة"))
        assertTrue(TextNormalizer.matchesArea("مدينه نصر الحي العاشر", "مدينة نصر"))
        assertTrue(TextNormalizer.matchesArea("6 اكتوبر الحي المتميز", "اكتوبر"))
        assertFalse(TextNormalizer.matchesArea("مصر الجديدة", "الدقي"))
        // Too short (< 3 chars)
        assertFalse(TextNormalizer.matchesArea("حي", "حي"))
    }

    @Test
    fun textNormalizer_bannedWords_detection() {
        val banned = listOf("شحنه", "طرد", "كرتونه", "بضاعه", "parcel")
        assertTrue(TextNormalizer.containsBannedWord("معايا شَحْنَة ومحتاج مساعدة", banned))
        assertTrue(TextNormalizer.containsBannedWord("عندي طَرْد صغير", banned))
        assertTrue(TextNormalizer.containsBannedWord("I have a big parcel", banned))
        assertFalse(TextNormalizer.containsBannedWord("راكب عادي بدون حقائب", banned))
    }

    // =========================================================================
    // 8. Null-Safety: Every Nullable Field in Policy & Request
    // =========================================================================

    @Test
    fun nullSafety_everyNullablePolicyFieldNull_executesGracefully() {
        val emptyPolicy = Policy(
            minPricePerKm = 0.0,
            minTotalKm = null,
            maxTotalKm = null,
            maxTripKm = null,
            maxPickupKm = null,
            maxPickupMinutes = null,
            minRiderRating = null,
            minRiderTrips = null,
            maxRequestAgeMinutes = null,
            maxOffer = null,
            governorate = null,
            avoidedAreas = emptyList(),
            preferredAreas = emptyList(),
            allowedPayments = emptySet(),
            bannedNoteWords = emptyList(),
            stopFee = null,
            longTripKm = null,
            freePickupKm = null,
            fuelCostPerKm = null,
            upkeepCostPerKm = null
        )
        val req = sampleRequest()
        val verdict = DecisionEngine.decide(req, emptyPolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict.tone)
    }

    @Test
    fun nullSafety_minimalParsedRequest_executesGracefully() {
        val minimalReq = ParsedRequest(
            app = RideApp.INDRIVE,
            key = "min_key",
            source = ReadSource.OCR,
            fare = 50.0,
            currency = null,
            pickupKm = null,
            tripKm = null,
            totalKm = 5.0,
            pickupMinutes = null,
            riderRating = null,
            riderTrips = null,
            requestAgeSeconds = null,
            pickupAddress = null,
            destinationAddress = null,
            extraStops = 0,
            paymentMethod = null,
            riderNote = null,
            offerCount = null,
            roadState = RoadState.UNKNOWN,
            productName = null,
            bounds = Rect(),
            isDetailOpen = true,
            confidence = 1.0f
        )
        val verdict = DecisionEngine.decide(minimalReq, samplePolicy(minPricePerKm = 8.0))
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict.tone)
        assertEquals(10.0, verdict.grossPerKm!!, 0.01)
    }

    // =========================================================================
    // 9. Flags Evaluation Tests
    // =========================================================================

    @Test
    fun flag_PREFERRED_AREA_attachedWhenDestinationMatches() {
        val policy = samplePolicy().copy(preferredAreas = listOf("التجمع", "الرحاب"))
        val req = sampleRequest(destinationAddress = "الرحاب بوابة 6")
        val verdict = DecisionEngine.decide(req, policy)
        assertTrue(verdict.flags.contains(DecisionFlags.PREFERRED_AREA))
    }

    @Test
    fun flag_STOPS_PREFERRED_attachedWhenModePreferAndExtraStops() {
        val policy = samplePolicy().copy(stopsMode = StopsMode.PREFER)
        val req = sampleRequest(extraStops = 1)
        val verdict = DecisionEngine.decide(req, policy)
        assertTrue(verdict.flags.contains(DecisionFlags.STOPS_PREFERRED))
    }

    @Test
    fun flag_FREE_PLAN_attachedWhenContextHasFreeStatus() {
        val policy = samplePolicy()
        val context = DecisionContext(licenseStatus = LicenseStatus.FREE)
        val verdict = DecisionEngine.decide(sampleRequest(), policy, context = context)
        assertTrue(verdict.flags.contains(DecisionFlags.FREE_PLAN))
    }

    @Test
    fun flag_OPEN_FOR_ROUTE_attachedWhenListModeAndTripKmNull() {
        val policy = samplePolicy(minPricePerKm = 8.0)
        // Request in LIST mode (!isDetailOpen) with detailAvailable = true, but tripKm is null
        val req = sampleRequest(
            fare = 100.0,
            pickupKm = 1.0,
            tripKm = null,
            totalKm = null,
            isDetailOpen = false
        )
        val context = DecisionContext(detailAvailable = true)
        val verdict = DecisionEngine.decide(req, policy, context = context)
        assertEquals(Tone.GREEN, verdict.tone)
        assertTrue(verdict.flags.contains(DecisionFlags.OPEN_FOR_ROUTE))
    }

    @Test
    fun flag_HOME_DIRECTION_withCustomGeoResolver() {
        val home = HomeWayState(
            enabled = true,
            lat = 30.0500, // Home in Heliopolis
            lon = 31.3300,
            label = "المنزل - مصر الجديدة"
        )
        val fakeGeoResolver = object : GeoResolver {
            override fun resolve(address: String): LatLon? {
                return when {
                    address.contains("المعادي") -> LatLon(29.9600, 31.2600) // South
                    address.contains("مدينة نصر") -> LatLon(30.0600, 31.3400) // Heading northeast towards Heliopolis
                    else -> null
                }
            }
        }
        val req = sampleRequest(
            pickupAddress = "المعادي كورنيش النيل",
            destinationAddress = "مدينة نصر مكرم عبيد"
        )
        val verdict = DecisionEngine.decide(req, samplePolicy(), home = home, geoResolver = fakeGeoResolver)
        assertTrue(verdict.flags.contains(DecisionFlags.HOME_DIRECTION))
    }

    @Test
    fun flag_HOME_DIRECTION_notAttachedWhenMovingAwayFromHome() {
        val home = HomeWayState(
            enabled = true,
            lat = 30.0500, // Home in North-East
            lon = 31.3300
        )
        val fakeGeoResolver = object : GeoResolver {
            override fun resolve(address: String): LatLon? {
                return when {
                    address.contains("المعادي") -> LatLon(29.9600, 31.2600)
                    address.contains("حلوان") -> LatLon(29.8400, 31.3000) // Moving south, away from Heliopolis
                    else -> null
                }
            }
        }
        val req = sampleRequest(
            pickupAddress = "المعادي",
            destinationAddress = "حلوان"
        )
        val verdict = DecisionEngine.decide(req, samplePolicy(), home = home, geoResolver = fakeGeoResolver)
        assertFalse(verdict.flags.contains(DecisionFlags.HOME_DIRECTION))
    }

    @Test
    fun flag_HOME_DIRECTION_omittedWhenDefaultStubResolverUsed() {
        val home = HomeWayState(enabled = true, lat = 30.0, lon = 31.0)
        val req = sampleRequest()
        val verdict = DecisionEngine.decide(req, samplePolicy(), home = home)
        assertFalse(verdict.flags.contains(DecisionFlags.HOME_DIRECTION))
    }

    // =========================================================================
    // 10. Table-Driven Multi-Scenario Suite (40 distinct combinations)
    // =========================================================================

    private data class TableCase(
        val name: String,
        val app: RideApp = RideApp.INDRIVE,
        val fare: Double? = 100.0,
        val pickupKm: Double? = 1.0,
        val tripKm: Double? = 9.0,
        val totalKm: Double? = 10.0,
        val extraStops: Int = 0,
        val rating: Double? = 4.9,
        val note: String? = null,
        val product: String? = null,
        val isDetail: Boolean = true,
        val detailAvailable: Boolean = true,
        val policy: Policy,
        val expectedTone: Tone,
        val expectedReason: String
    )

    @Test
    fun comprehensiveTableDrivenTests() {
        val basePolicy = Policy(
            minPricePerKm = 8.0,
            currency = "EGP",
            governorate = "cairo",
            appEnabled = mapOf(RideApp.INDRIVE to true, RideApp.UBER to true, RideApp.DIDI to true)
        )

        val cases = listOf(
            TableCase(
                name = "Normal profitable trip",
                fare = 120.0,
                policy = basePolicy,
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            ),
            TableCase(
                name = "Uber app disabled",
                app = RideApp.UBER,
                policy = basePolicy.copy(appEnabled = mapOf(RideApp.UBER to false)),
                expectedTone = Tone.GRAY,
                expectedReason = ReasonCodes.APP_DISABLED
            ),
            TableCase(
                name = "DiDi app disabled",
                app = RideApp.DIDI,
                policy = basePolicy.copy(appEnabled = mapOf(RideApp.DIDI to false)),
                expectedTone = Tone.GRAY,
                expectedReason = ReasonCodes.APP_DISABLED
            ),
            TableCase(
                name = "Missing fare in list mode",
                fare = null,
                isDetail = false,
                policy = basePolicy,
                expectedTone = Tone.GRAY,
                expectedReason = ReasonCodes.DATA_MISSING
            ),
            TableCase(
                name = "Distance missing and detail not available",
                fare = 75.0,
                tripKm = null,
                totalKm = null,
                isDetail = false,
                detailAvailable = false,
                policy = basePolicy,
                expectedTone = Tone.GRAY,
                expectedReason = ReasonCodes.DATA_MISSING
            ),
            TableCase(
                name = "Pickup exceeds 5 km limit",
                pickupKm = 5.5,
                policy = basePolicy.copy(maxPickupKm = 5.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.PICKUP_TOO_FAR
            ),
            TableCase(
                name = "Pickup exactly at limit 5 km",
                pickupKm = 5.0,
                fare = 150.0,
                policy = basePolicy.copy(maxPickupKm = 5.0),
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            ),
            TableCase(
                name = "Trip exceeds max trip 20 km",
                tripKm = 22.0,
                totalKm = 24.0,
                policy = basePolicy.copy(maxTripKm = 20.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.TRIP_TOO_LONG
            ),
            TableCase(
                name = "Total distance too short (< 4 km)",
                totalKm = 3.5,
                tripKm = 3.0,
                policy = basePolicy.copy(minTotalKm = 4.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.TOTAL_TOO_SHORT
            ),
            TableCase(
                name = "Total distance too long (> 50 km)",
                totalKm = 55.0,
                tripKm = 52.0,
                policy = basePolicy.copy(maxTotalKm = 50.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.TOTAL_TOO_LONG
            ),
            TableCase(
                name = "Low rating 4.3 below 4.5",
                rating = 4.3,
                policy = basePolicy.copy(minRiderRating = 4.5),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.RATING_LOW
            ),
            TableCase(
                name = "Rating exactly at threshold 4.5",
                rating = 4.5,
                fare = 120.0,
                policy = basePolicy.copy(minRiderRating = 4.5),
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            ),
            TableCase(
                name = "Banned note word parcel",
                note = "delivery parcel box",
                policy = basePolicy.copy(bannedNoteWords = listOf("parcel")),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.NOTE_BANNED_WORD
            ),
            TableCase(
                name = "Clean note",
                note = "please turn on AC",
                fare = 120.0,
                policy = basePolicy.copy(bannedNoteWords = listOf("parcel")),
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            ),
            TableCase(
                name = "Avoid extra stops",
                extraStops = 1,
                policy = basePolicy.copy(stopsMode = StopsMode.AVOID),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.STOPS_AVOID
            ),
            TableCase(
                name = "Warn extra stops",
                extraStops = 1,
                fare = 120.0,
                policy = basePolicy.copy(stopsMode = StopsMode.WARN),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.STOPS_WARN
            ),
            TableCase(
                name = "Ignore extra stops mode",
                extraStops = 1,
                fare = 120.0,
                policy = basePolicy.copy(stopsMode = StopsMode.IGNORE),
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            ),
            TableCase(
                name = "Long trip avoid mode",
                tripKm = 35.0,
                totalKm = 36.0,
                policy = basePolicy.copy(longTripMode = LongTripMode.AVOID, longTripKm = 30.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.LONG_TRIP_AVOID
            ),
            TableCase(
                name = "Long trip warn mode",
                tripKm = 35.0,
                totalKm = 36.0,
                fare = 400.0,
                policy = basePolicy.copy(longTripMode = LongTripMode.WARN, longTripKm = 30.0),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.LONG_TRIP_WARN
            ),
            TableCase(
                name = "Uber saver warning",
                product = "Uber Saver",
                fare = 150.0,
                policy = basePolicy.copy(warnOnUberSaver = true),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.UBER_SAVER
            ),
            TableCase(
                name = "Price low 6 EGP/km vs min 8",
                fare = 60.0,
                totalKm = 10.0,
                policy = basePolicy.copy(minPricePerKm = 8.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.PRICE_LOW
            ),
            TableCase(
                name = "Price borderline 7.7 EGP/km vs min 8",
                fare = 77.0, // 7.7 >= 0.95 * 8 = 7.6
                totalKm = 10.0,
                policy = basePolicy.copy(minPricePerKm = 8.0),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.PRICE_BORDERLINE
            ),
            TableCase(
                name = "Price borderline 7.6 EGP/km exact 95%",
                fare = 76.0, // 7.6 == 0.95 * 8
                totalKm = 10.0,
                policy = basePolicy.copy(minPricePerKm = 8.0),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.PRICE_BORDERLINE
            ),
            TableCase(
                name = "Price low 7.59 EGP/km just below 95%",
                fare = 75.9, // 7.59 < 7.6
                totalKm = 10.0,
                policy = basePolicy.copy(minPricePerKm = 8.0),
                expectedTone = Tone.RED,
                expectedReason = ReasonCodes.PRICE_LOW
            ),
            TableCase(
                name = "Governorate not set with avoided areas",
                fare = 120.0,
                policy = basePolicy.copy(governorate = null, avoidedAreas = listOf("حلوان")),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.AREA_UNKNOWN
            ),
            TableCase(
                name = "Governorate not set with preferred areas",
                fare = 120.0,
                policy = basePolicy.copy(governorate = "", preferredAreas = listOf("المعادي")),
                expectedTone = Tone.ORANGE,
                expectedReason = ReasonCodes.AREA_UNKNOWN
            ),
            TableCase(
                name = "Governorate set and area clean",
                fare = 120.0,
                policy = basePolicy.copy(governorate = "cairo", avoidedAreas = listOf("حلوان")),
                expectedTone = Tone.GREEN,
                expectedReason = ReasonCodes.ACCEPTABLE
            )
        )

        for (tc in cases) {
            val req = sampleRequest(
                app = tc.app,
                fare = tc.fare,
                pickupKm = tc.pickupKm,
                tripKm = tc.tripKm,
                totalKm = tc.totalKm,
                extraStops = tc.extraStops,
                riderRating = tc.rating,
                riderNote = tc.note,
                productName = tc.product ?: "inDrive",
                isDetailOpen = tc.isDetail
            )
            val context = DecisionContext(detailAvailable = tc.detailAvailable)
            val verdict = DecisionEngine.decide(req, tc.policy, context = context)
            assertEquals("TestCase '${tc.name}' tone mismatch", tc.expectedTone, verdict.tone)
            assertEquals("TestCase '${tc.name}' reasonCode mismatch", tc.expectedReason, verdict.reasonCode)
        }
    }
}
