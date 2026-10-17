package com.driveflow.driver.reader.uber

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.AppReader
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.ocr.OcrReadCoordinator
import com.driveflow.driver.ocr.model.AppOcrConfig

class UberReader(
    private val coordinator: OcrReadCoordinator,
    private val configProvider: () -> AppOcrConfig = { AppOcrConfig(app = "uber", enabled = false) }
) : AppReader {
    override val app: RideApp = RideApp.UBER

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString()
        if (pkg == app.pkg) return true

        val rootId = root.viewIdResourceName
        if (rootId?.startsWith(app.pkg) == true) return true

        val knownIds = listOf(
            "com.ubercab.driver:id/rxmap",
            "com.ubercab.driver:id/go_online_button",
            "com.ubercab.driver:id/active_container",
            "rxmap",
            "go_online_button",
            "active_container"
        )
        for (id in knownIds) {
            try {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        if (!isRideAppWindow(root)) return emptyList()
        val latest = coordinator.getLatestRequests(app)
        return latest.filter { !it.isDetailOpen }
    }

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        if (!isRideAppWindow(root)) return null
        val latest = coordinator.getLatestRequests(app)
        return latest.firstOrNull { it.isDetailOpen }
    }

    override fun mapBounds(root: AccessibilityNodeInfo): Rect? {
        if (!isRideAppWindow(root)) return null
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val w = if (rootBounds.width() > 0) rootBounds.width() else 1080
        val h = if (rootBounds.height() > 0) rootBounds.height() else 2340
        val config = configProvider()
        return config.mapFraction.toScreenRect(w, h)
    }
}
