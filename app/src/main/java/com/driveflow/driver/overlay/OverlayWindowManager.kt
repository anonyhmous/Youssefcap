package com.driveflow.driver.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.driveflow.driver.R
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.ScreenReading
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.BarPosition
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.data.prefs.SettingsStore
import com.driveflow.driver.license.LicenseGate
import com.driveflow.driver.overlay.alert.OpportunityAlertManager
import com.driveflow.driver.overlay.bar.FloatingControlBarView
import com.driveflow.driver.overlay.markers.CardMarkersView
import com.driveflow.driver.overlay.model.BarMode
import com.driveflow.driver.overlay.model.FloatingBarState
import com.driveflow.driver.overlay.model.OverlayWindowType
import com.driveflow.driver.overlay.placement.BarAutoPositioner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Window manager and coordinator for DriveFlow overlay elements (Card Markers + Floating Control Bar).
 *
 * Responsibilities:
 * - Prefer TYPE_ACCESSIBILITY_OVERLAY (2032) from accessibility context; fallback to TYPE_APPLICATION_OVERLAY (2038).
 * - Keep marks non-touchable and excluded from accessibility tree.
 * - Coalesce updates to at most 10/sec (100ms throttle).
 * - Immediately hide marks and bar when screen locks or ride app is backgrounded.
 * - Manage Opportunity Alerts.
 */
class OverlayWindowManager(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val licenseGate: LicenseGate = LicenseGate.PERMISSIVE,
    private val isAccessibilityContext: Boolean = false,
    private val onOpenReport: ((facts: Map<String, Any?>) -> Unit)? = null
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _activeWindowType = MutableStateFlow(determineInitialWindowType())
    val activeWindowType: StateFlow<OverlayWindowType> = _activeWindowType.asStateFlow()

    private var markersView: CardMarkersView? = null
    private var controlBarView: FloatingControlBarView? = null

    private var markersParams: WindowManager.LayoutParams? = null
    private var controlBarParams: WindowManager.LayoutParams? = null

    private val alertManager = OpportunityAlertManager(context, licenseGate)

    // Last processed state for deduplication & throttling
    private var lastUpdateTimestamp = 0L
    private var lastReadingHash = 0
    private var lastVerdictsHash = 0

    // Hidden requests (closed by user for current session)
    private val closedRequestKeys = mutableSetOf<String>()

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                hideAll()
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(screenStateReceiver, filter)
    }

    private fun determineInitialWindowType(): OverlayWindowType {
        return if (isAccessibilityContext) {
            OverlayWindowType.ACCESSIBILITY_OVERLAY
        } else if (Settings.canDrawOverlays(context)) {
            OverlayWindowType.APPLICATION_OVERLAY
        } else {
            OverlayWindowType.NONE
        }
    }

    /**
     * Diagnostic text describing active overlay type for Settings screen.
     */
    fun getDiagnosticString(): String {
        return when (_activeWindowType.value) {
            OverlayWindowType.ACCESSIBILITY_OVERLAY ->
                context.getString(R.string.settings_overlay_diag_accessibility)
            OverlayWindowType.APPLICATION_OVERLAY ->
                context.getString(R.string.settings_overlay_diag_application)
            OverlayWindowType.NONE ->
                context.getString(R.string.settings_overlay_diag_none)
        }
    }

    /**
     * Initializes and attaches the overlay views to WindowManager.
     */
    fun attach() {
        val type = determineInitialWindowType()
        _activeWindowType.value = type
        if (type == OverlayWindowType.NONE) return

        val windowType = type.code

        // 1. Full-screen non-touchable CardMarkersView
        if (markersView == null) {
            val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            markersView = CardMarkersView(context, isRtl)

            markersParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            try {
                windowManager.addView(markersView, markersParams)
            } catch (_: Exception) {
            }
        }

        // 2. Floating Control Bar View
        if (controlBarView == null) {
            val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            controlBarView = FloatingControlBarView(
                context = context,
                onPositionChanged = { newX, newY ->
                    scope.launch {
                        settingsStore.setBarPosition(
                            BarPosition(isAuto = false, x = newX, y = newY)
                        )
                    }
                },
                onReportClicked = { req, verdict ->
                    val facts = mutableMapOf<String, Any?>()
                    req?.let {
                        facts["app"] = it.app.code
                        facts["fare"] = it.fare
                        facts["tripKm"] = it.tripKm
                        facts["pickupKm"] = it.pickupKm
                        facts["totalKm"] = it.totalKm
                    }
                    verdict?.let {
                        facts["reasonCode"] = it.reasonCode
                        facts["grossPerKm"] = it.grossPerKm
                        facts["netPerKm"] = it.netPerKm
                    }
                    onOpenReport?.invoke(facts)
                },
                onCloseClicked = {
                    controlBarView?.state?.request?.let {
                        closedRequestKeys.add(it.key)
                    }
                    controlBarView?.updateState(
                        controlBarView?.state?.copy(mode = BarMode.HIDDEN)
                            ?: FloatingBarState(mode = BarMode.HIDDEN)
                    )
                },
                isRtl = isRtl
            )

            controlBarParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 150
            }

            try {
                windowManager.addView(controlBarView, controlBarParams)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Ingests latest ScreenReading and calculated Verdicts, coalesced to max 10 updates/sec.
     */
    fun update(
        reading: ScreenReading,
        verdicts: Map<String, Verdict>,
        policy: Policy,
        markerSize: MarkerSize,
        barOpacityPercent: Int,
        barPosition: BarPosition,
        alertEnabled: Boolean,
        alertMode: com.driveflow.driver.data.prefs.AlertMode
    ) {
        // Fast-path hide if ride app is not visible
        if (!reading.isRideAppVisible) {
            hideAll()
            return
        }

        val now = System.currentTimeMillis()
        // Rate-limit coalescing: at most 10 per second (100ms)
        if (now - lastUpdateTimestamp < 100L) {
            return
        }

        // Deduplication: check if data changed
        val readingHash = reading.requests.hashCode() xor reading.isDetailOpen.hashCode()
        val verdictsHash = verdicts.hashCode()
        if (readingHash == lastReadingHash && verdictsHash == lastVerdictsHash) {
            return
        }

        lastUpdateTimestamp = now
        lastReadingHash = readingHash
        lastVerdictsHash = verdictsHash

        if (markersView == null || controlBarView == null) {
            attach()
        }

        // 1. Update Card Markers
        val visibleRequests = reading.requests.filter { !closedRequestKeys.contains(it.key) }
        markersView?.visibility = View.VISIBLE
        markersView?.updateMarkers(
            requests = visibleRequests,
            verdicts = verdicts,
            size = markerSize,
            opacityPercent = barOpacityPercent
        )

        // 2. Determine and update Floating Control Bar state
        val targetRequest = if (reading.isDetailOpen) {
            reading.detailRequest ?: visibleRequests.firstOrNull()
        } else {
            visibleRequests.firstOrNull()
        }

        val targetVerdict = targetRequest?.let { verdicts[it.key] }

        val barMode = when {
            targetRequest == null -> BarMode.READING
            closedRequestKeys.contains(targetRequest.key) -> BarMode.HIDDEN
            targetVerdict == null -> BarMode.WAITING_DETAILS
            !licenseGate.canRead() -> BarMode.LICENSE_PROBLEM
            else -> BarMode.VERDICT
        }

        // Auto or custom position calculation
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels
        val barW = controlBarView?.width?.takeIf { it > 0 } ?: (240 * metrics.density).toInt()
        val barH = controlBarView?.height?.takeIf { it > 0 } ?: (64 * metrics.density).toInt()

        val (posX, posY) = if (barPosition.isAuto) {
            BarAutoPositioner.calculateAutoPosition(
                mapBounds = reading.mapBounds,
                screenWidth = screenW,
                screenHeight = screenH,
                barWidth = barW,
                barHeight = barH
            )
        } else {
            BarAutoPositioner.clampManualPosition(
                x = barPosition.x,
                y = barPosition.y,
                screenWidth = screenW,
                screenHeight = screenH,
                barWidth = barW,
                barHeight = barH
            )
        }

        controlBarView?.updatePosition(posX, posY, notify = false)
        controlBarView?.updateState(
            FloatingBarState(
                mode = barMode,
                request = targetRequest,
                verdict = targetVerdict,
                isCollapsed = controlBarView?.state?.isCollapsed ?: false,
                opacityPercent = barOpacityPercent,
                isAutoPosition = barPosition.isAuto,
                positionX = posX,
                positionY = posY
            )
        )

        // 3. Opportunity Alert trigger
        if (targetRequest != null && targetVerdict != null) {
            alertManager.triggerIfEligible(
                request = targetRequest,
                verdict = targetVerdict,
                policy = policy,
                alertEnabled = alertEnabled,
                alertMode = alertMode
            )
        }
    }

    /**
     * Hides all overlay components immediately.
     */
    fun hideAll() {
        markersView?.clearMarkers()
        markersView?.visibility = View.GONE
        controlBarView?.visibility = View.GONE
    }

    /**
     * Completely detaches and releases overlay resources.
     */
    fun release() {
        try {
            context.unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
        }

        hideAll()
        alertManager.release()

        markersView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        markersView = null

        controlBarView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        controlBarView = null
    }
}
