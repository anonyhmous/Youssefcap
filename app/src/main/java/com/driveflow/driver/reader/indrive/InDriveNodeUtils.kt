package com.driveflow.driver.reader.indrive

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object InDriveNodeUtils {
    fun findNodesByViewIds(
        root: AccessibilityNodeInfo,
        viewIds: List<String>
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        for (viewId in viewIds) {
            try {
                val matches = root.findAccessibilityNodeInfosByViewId(viewId)
                if (!matches.isNullOrEmpty()) {
                    results.addAll(matches)
                }
            } catch (_: Exception) {}
        }
        return results
    }

    fun findFirstNodeByViewIds(
        root: AccessibilityNodeInfo,
        viewIds: List<String>
    ): AccessibilityNodeInfo? {
        for (viewId in viewIds) {
            try {
                val matches = root.findAccessibilityNodeInfosByViewId(viewId)
                if (!matches.isNullOrEmpty()) {
                    return matches[0]
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun findTextByViewIds(
        root: AccessibilityNodeInfo,
        viewIds: List<String>
    ): String? {
        val node = findFirstNodeByViewIds(root, viewIds) ?: return null
        return getNodeText(node)
    }

    fun getNodeText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank()) return text
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank()) return desc
        return null
    }

    fun getBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        try {
            node.getBoundsInScreen(rect)
        } catch (_: Exception) {
            rect.setEmpty()
        }
        return rect
    }

    fun collectAllTextsWithBounds(
        node: AccessibilityNodeInfo,
        maxDepth: Int = 15,
        currentDepth: Int = 0
    ): List<Pair<String, Rect>> {
        val list = mutableListOf<Pair<String, Rect>>()
        if (currentDepth > maxDepth) return list
        val text = getNodeText(node)
        val bounds = getBounds(node)
        if (!text.isNullOrBlank() && !bounds.isEmpty) {
            list.add(Pair(text, bounds))
        }
        val childCount = try { node.childCount } catch (_: Exception) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            list.addAll(collectAllTextsWithBounds(child, maxDepth, currentDepth + 1))
        }
        return list
    }

    fun findCandidateCardNodes(
        root: AccessibilityNodeInfo,
        config: InDriveReaderConfig
    ): List<AccessibilityNodeInfo> {
        val explicitCards = findNodesByViewIds(root, config.itemCardIds)
        if (explicitCards.isNotEmpty()) {
            return explicitCards.filter { node ->
                val b = getBounds(node)
                b.width() > 200 && b.height() > 100
            }
        }

        val listContainer = findFirstNodeByViewIds(root, config.listContainerIds)
        if (listContainer != null) {
            val children = mutableListOf<AccessibilityNodeInfo>()
            val childCount = try { listContainer.childCount } catch (_: Exception) { 0 }
            for (i in 0 until childCount) {
                val child = try { listContainer.getChild(i) } catch (_: Exception) { null } ?: continue
                val b = getBounds(child)
                if (b.width() > 200 && b.height() > 100) {
                    children.add(child)
                }
            }
            if (children.isNotEmpty()) {
                return children
            }
        }

        val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
        collectCardNodesByHeuristic(root, candidateRoots, 0)
        return candidateRoots
    }

    private fun collectCardNodesByHeuristic(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (depth > 8) return
        val bounds = getBounds(node)
        val isCardShape = bounds.width() > 400 && bounds.height() in 140..1000
        if (isCardShape) {
            val texts = collectAllTextsWithBounds(node, maxDepth = 4)
            val hasFare = texts.any { (txt, _) ->
                txt.contains("ج.م") || txt.contains("EGP") || txt.contains("جنيه") ||
                (txt.matches(Regex(""".*\d+.*""")) && txt.length < 15)
            }
            val hasDistance = texts.any { (txt, _) ->
                txt.contains("كم") || txt.contains("km", ignoreCase = true) || txt.contains("متر")
            }
            if (hasFare && hasDistance) {
                out.add(node)
                return
            }
        }
        val childCount = try { node.childCount } catch (_: Exception) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            collectCardNodesByHeuristic(child, out, depth + 1)
        }
    }
}
