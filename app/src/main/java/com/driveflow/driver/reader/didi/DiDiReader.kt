package com.driveflow.driver.reader.didi

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.AppReader
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.ocr.OcrReadCoordinator
import com.driveflow.driver.ocr.model.AppOcrConfig

class DiDiReader(
    private val coordinator: OcrReadCoordinator,
    private val configProvider: () -> AppOcrConfig = { AppOcrConfig(app = "didi", enabled = true) }
) : AppReader {
    override val app: RideApp = RideApp.DIDI

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString()
        if (pkg == app.pkg) return true

        val rootId = root.viewIdResourceName
        if (rootId?.startsWith(app.pkg) == true) return true
        return false
    }

    fun isIdleOrSearching(root: AccessibilityNodeInfo): Boolean {
        val allTexts = collectTexts(root, maxDepth = 6)
        return allTexts.any { txt ->
            txt.contains("جاري البحث") || txt.contains("جاري البحث...") ||
            txt.contains("searching", ignoreCase = true) ||
            txt.contains("بحث عن طلبات")
        }
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        if (!isRideAppWindow(root)) return emptyList()
        if (isIdleOrSearching(root)) return emptyList()
        val latest = coordinator.getLatestRequests(app)
        return latest.filter { !it.isDetailOpen }
    }

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        if (!isRideAppWindow(root)) return null
        if (isIdleOrSearching(root)) return null
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

    private fun collectTexts(node: AccessibilityNodeInfo, maxDepth: Int, depth: Int = 0): List<String> {
        if (depth > maxDepth) return emptyList()
        val list = mutableListOf<String>()
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank()) list.add(text)
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank()) list.add(desc)

        val childCount = try { node.childCount } catch (_: Exception) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            list.addAll(collectTexts(child, maxDepth, depth + 1))
        }
        return list
    }
}
