package com.driveflow.driver.overlay

import android.graphics.Rect
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.license.LicenseGate
import com.driveflow.driver.overlay.alert.OpportunityAlertEligibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class OpportunityAlertTest {

    private fun sampleRequest(
        key: String = "req_1",
        fare: Double? = 120.0,
        totalKm: Double? = 10.0
    ): ParsedRequest {
        return ParsedRequest(
            app = RideApp.INDRIVE,
            key = key,
            source = ReadSource.ACCESSIBILITY,
            fare = fare,
            currency = "EGP",
            pickupKm = 1.0,
            tripKm = 9.0,
            totalKm = totalKm,
            pickupMinutes = 4,
            riderRating = 4.9,
            riderTrips = 50,
            requestAgeSeconds = 15,
            pickupAddress = "المعادي",
            destinationAddress = "مدينة نصر",
            extraStops = 0,
            paymentMethod = "CASH",
            riderNote = null,
            offerCount = null,
            roadState = RoadState.EASY,
            productName = "inDrive",
            bounds = Rect(0, 0, 100, 100)
        )
    }

    private fun sampleVerdict(
        tone: Tone = Tone.GREEN,
        flags: List<String> = emptyList(),
        totalKm: Double? = 10.0
    ): Verdict {
        return Verdict(
            tone = tone,
            reasonCode = "PRICE_OK",
            titleAr = "طلب ممتاز",
            titleEn = "Great Trip",
            detailAr = "السعر مناسب",
            detailEn = "Price is good",
            flags = flags,
            totalKm = totalKm,
            grossPerKm = 12.0,
            netPerKm = 10.5,
            netProfit = 85.0,
            suggestedPrice = 120.0
        )
    }

    @Test
    fun shouldAlert_tableDriven_eligibilityScenarios() {
        data class TestCase(
            val name: String,
            val request: ParsedRequest,
            val verdict: Verdict,
            val policy: Policy,
            val licenseCanRead: Boolean,
            val isAlertEnabled: Boolean,
            val isQuietMode: Boolean,
            val lastAlertTime: Long?,
            val currentTime: Long,
            val expected: Boolean
        )

        val policy = Policy(minPricePerKm = 10.0)
        val t0 = 100000L

        val testCases = listOf(
            TestCase(
                name = "Ideal green request meets policy price",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN, flags = emptyList()),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = true
            ),
            TestCase(
                name = "Red tone rejected",
                request = sampleRequest(fare = 60.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.RED),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Orange tone rejected",
                request = sampleRequest(fare = 110.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.ORANGE),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Gray tone rejected",
                request = sampleRequest(fare = null, totalKm = null),
                verdict = sampleVerdict(tone = Tone.GRAY),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Green with EXTRA_STOPS_WARN rejected",
                request = sampleRequest(fare = 150.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN, flags = listOf("EXTRA_STOPS_WARN")),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Green with LONG_TRIP_WARN rejected",
                request = sampleRequest(fare = 350.0, totalKm = 30.0),
                verdict = sampleVerdict(tone = Tone.GREEN, flags = listOf("LONG_TRIP_WARN")),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Green with UBER_SAVER_WARN rejected",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN, flags = listOf("UBER_SAVER_WARN")),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Green with OPEN_FOR_ROUTE flag is allowed",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN, flags = listOf("OPEN_FOR_ROUTE", "HOME_DIRECTION")),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = true
            ),
            TestCase(
                name = "Fare less than required policy price rejected",
                request = sampleRequest(fare = 80.0, totalKm = 10.0), // needs 10.0 * 10 = 100
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "License cannot read rejected",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = false,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Alerts disabled in settings rejected",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = false,
                isQuietMode = false,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Quiet mode (phone call / DND) rejected",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = true,
                lastAlertTime = null,
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Cooldown active (<8000ms elapsed) rejected",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = t0 - 4000L, // 4 seconds ago
                currentTime = t0,
                expected = false
            ),
            TestCase(
                name = "Cooldown expired (>=8000ms elapsed) accepted",
                request = sampleRequest(fare = 120.0, totalKm = 10.0),
                verdict = sampleVerdict(tone = Tone.GREEN),
                policy = policy,
                licenseCanRead = true,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTime = t0 - 8500L, // 8.5 seconds ago
                currentTime = t0,
                expected = true
            )
        )

        for (tc in testCases) {
            val gate = object : LicenseGate {
                override fun canRead(): Boolean = tc.licenseCanRead
            }
            val actual = OpportunityAlertEligibility.shouldAlert(
                request = tc.request,
                verdict = tc.verdict,
                policy = tc.policy,
                licenseGate = gate,
                isAlertEnabled = tc.isAlertEnabled,
                isQuietMode = tc.isQuietMode,
                lastAlertTimeForRequest = tc.lastAlertTime,
                currentTime = tc.currentTime
            )
            assertEquals("Failed test case: ${tc.name}", tc.expected, actual)
        }
    }

    @Test
    fun shouldAlert_independentCooldownPerRequestKey() {
        val policy = Policy(minPricePerKm = 10.0)
        val gate = LicenseGate.PERMISSIVE
        val t0 = 200000L

        val req1 = sampleRequest(key = "req_1", fare = 150.0, totalKm = 10.0)
        val req2 = sampleRequest(key = "req_2", fare = 180.0, totalKm = 12.0)
        val verdict = sampleVerdict(tone = Tone.GREEN)

        // req1 alerted 2 seconds ago
        val lastAlertTimes = mapOf("req_1" to t0 - 2000L)

        // req1 is on cooldown
        assertFalse(
            OpportunityAlertEligibility.shouldAlert(
                request = req1,
                verdict = verdict,
                policy = policy,
                licenseGate = gate,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTimeForRequest = lastAlertTimes[req1.key],
                currentTime = t0
            )
        )

        // req2 has never alerted (or null last time) -> eligible
        assertTrue(
            OpportunityAlertEligibility.shouldAlert(
                request = req2,
                verdict = verdict,
                policy = policy,
                licenseGate = gate,
                isAlertEnabled = true,
                isQuietMode = false,
                lastAlertTimeForRequest = lastAlertTimes[req2.key],
                currentTime = t0
            )
        )
    }
}
