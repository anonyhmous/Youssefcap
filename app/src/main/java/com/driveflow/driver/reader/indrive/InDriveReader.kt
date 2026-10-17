package com.driveflow.driver.reader.indrive

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.AppReader
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RideApp

class InDriveReader(
    private val configProvider: () -> InDriveReaderConfig = { InDriveReaderConfig.DEFAULT }
) : AppReader {
    override val app: RideApp = RideApp.INDRIVE

    private val listReader: InDriveListReader
        get() = InDriveListReader(configProvider())

    private val detailReader: InDriveDetailReader
        get() = InDriveDetailReader(configProvider())

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString()
        if (pkg == app.pkg) return true

        val viewId = root.viewIdResourceName
        if (viewId != null && viewId.startsWith(app.pkg)) {
            return true
        }

        val config = configProvider()
        val anyConfiguredNode = InDriveNodeUtils.findFirstNodeByViewIds(root, config.listContainerIds)
            ?: InDriveNodeUtils.findFirstNodeByViewIds(root, config.itemCardIds)
            ?: InDriveNodeUtils.findFirstNodeByViewIds(root, config.detailContainerIds)
        return anyConfiguredNode != null
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        if (!isRideAppWindow(root)) return emptyList()
        return listReader.readList(root)
    }

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        if (!isRideAppWindow(root)) return null
        return detailReader.readDetail(root)
    }

    override fun mapBounds(root: AccessibilityNodeInfo): Rect? {
        if (!isRideAppWindow(root)) return null
        return detailReader.findMapBounds(root)
    }
}
