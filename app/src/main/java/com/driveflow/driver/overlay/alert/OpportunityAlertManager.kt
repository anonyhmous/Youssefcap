package com.driveflow.driver.overlay.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.driveflow.driver.R
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.license.LicenseGate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Android system integration for DriveFlow Opportunity Alerts.
 *
 * Plays audio tones, triggers haptic feedback, and displays heads-up notifications
 * when an optimal trip opportunity appears on screen.
 */
class OpportunityAlertManager(
    private val context: Context,
    private val licenseGate: LicenseGate = LicenseGate.PERMISSIVE
) {

    companion object {
        const val CHANNEL_ID = "driveflow_alerts"
        private const val NOTIFICATION_ID_BASE = 50000
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val lastAlertTimes = ConcurrentHashMap<String, Long>()

    private var toneGenerator: ToneGenerator? = null

    init {
        createNotificationChannel()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {
            // Some devices or emulators fail ToneGenerator allocation; gracefully handled
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.alert_channel_name)
            val desc = context.getString(R.string.alert_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = desc
                enableVibration(false) // Handled explicitly to prevent double vibration
                setSound(null, null)   // Custom sound handled via ToneGenerator
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if the device is currently in quiet mode (active call or DND).
     */
    fun isQuietMode(): Boolean {
        // 1. Phone call active check
        val mode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
            return true
        }

        // 2. Do Not Disturb check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
            val filter = notificationManager.currentInterruptionFilter
            if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if device is in silent mode (no sound and no vibration allowed).
     */
    fun isSilentMode(): Boolean {
        return audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    /**
     * Evaluates and triggers an alert if eligible.
     */
    fun triggerIfEligible(
        request: ParsedRequest,
        verdict: Verdict,
        policy: Policy,
        alertEnabled: Boolean,
        alertMode: AlertMode
    ): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastAlertTimes[request.key]

        val eligible = OpportunityAlertEligibility.shouldAlert(
            request = request,
            verdict = verdict,
            policy = policy,
            licenseGate = licenseGate,
            isAlertEnabled = alertEnabled,
            isQuietMode = isQuietMode(),
            lastAlertTimeForRequest = lastTime,
            currentTime = now
        )

        if (!eligible) return false

        lastAlertTimes[request.key] = now
        executeAlert(request, verdict, alertMode)
        return true
    }

    /**
     * Directly executes sound, vibration, and heads-up notification.
     */
    fun executeAlert(
        request: ParsedRequest,
        verdict: Verdict,
        alertMode: AlertMode
    ) {
        val silent = isSilentMode()

        // 1. Audio tone (Short two-tone beep)
        if (!silent) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
            } catch (_: Exception) {
            }
        }

        // 2. Haptic vibration (if TONE_VIBRATE and not silent mode)
        if (alertMode == AlertMode.TONE_VIBRATE && !silent) {
            triggerVibration()
        }

        // 3. Heads-up Notification
        showHeadsUpNotification(request, verdict)
    }

    private fun triggerVibration() {
        try {
            val pattern = longArrayOf(0, 160, 80, 160)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showHeadsUpNotification(request: ParsedRequest, verdict: Verdict) {
        try {
            val fareStr = request.fare?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
            val kmStr = (verdict.totalKm ?: request.totalKm)?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val appName = request.app.name

            val title = context.getString(R.string.alert_opportunity_title, appName)
            val body = context.getString(R.string.alert_opportunity_body, verdict.titleAr, fareStr, kmStr)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_driveflow_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setTimeoutAfter(6000L) // Dismiss after 6 seconds

            notificationManager?.notify(NOTIFICATION_ID_BASE + (request.key.hashCode() % 1000), builder.build())
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
        }
        lastAlertTimes.clear()
    }
}
