package com.driveflow.driver.overlay.alert

import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.license.LicenseGate

/**
 * Pure Kotlin eligibility and cooldown rules for DriveFlow Opportunity Alerts.
 *
 * Rules:
 * - Request must be Tone.GREEN.
 * - Fare must meet or exceed required policy price: fare >= minPricePerKm * totalKm.
 * - No ORANGE or warning flags (must be clean opportunity).
 * - License gate must allow reading.
 * - SettingsStore.alertEnabled must be true.
 * - Device must not be in quiet mode (active call or Do Not Disturb).
 * - Per-request-key cooldown of 8 seconds (8,000 ms).
 * - Never alert for RED, ORANGE, or GRAY.
 */
object OpportunityAlertEligibility {

    const val COOLDOWN_MS = 8000L

    /**
     * Determines whether an opportunity alert should fire for the given request and verdict.
     */
    fun shouldAlert(
        request: ParsedRequest,
        verdict: Verdict,
        policy: Policy,
        licenseGate: LicenseGate,
        isAlertEnabled: Boolean,
        isQuietMode: Boolean,
        lastAlertTimeForRequest: Long?,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        // 1. Settings & License checks
        if (!isAlertEnabled) return false
        if (!licenseGate.canRead()) return false
        if (isQuietMode) return false

        // 2. Cooldown check per request key
        if (lastAlertTimeForRequest != null) {
            val elapsed = currentTime - lastAlertTimeForRequest
            if (elapsed < COOLDOWN_MS) return false
        }

        // 3. Strict Tone check: Only GREEN
        if (verdict.tone != Tone.GREEN) return false

        // 4. No warning / orange flags
        val hasWarningFlags = verdict.flags.any { flag ->
            flag.contains("WARN", ignoreCase = true) ||
            flag == "EXTRA_STOPS" ||
            flag == "LONG_TRIP" ||
            flag == "UBER_SAVER"
        }
        if (hasWarningFlags) return false

        // 5. Fare >= policy price check
        val fare = request.fare ?: return false
        val totalKm = verdict.totalKm ?: request.totalKm
        if (totalKm != null && totalKm > 0 && policy.minPricePerKm > 0) {
            val requiredPrice = policy.minPricePerKm * totalKm
            if (fare < requiredPrice) return false
        }

        return true
    }
}
