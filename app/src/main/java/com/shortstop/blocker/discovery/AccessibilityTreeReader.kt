package com.shortstop.blocker.discovery

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

internal class AccessibilityTreeReader(
    private val maxNodes: Int = DEFAULT_MAX_NODES,
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxChildrenPerNode: Int = DEFAULT_MAX_CHILDREN_PER_NODE,
) {
    fun read(root: AccessibilityNodeInfo, metadata: CaptureMetadata): SanitizedNodeTree {
        val nodes = mutableListOf<SanitizedNode>()
        var truncated = false

        fun visit(node: AccessibilityNodeInfo, parentIndex: Int?, depth: Int) {
            if (nodes.size >= maxNodes) {
                truncated = true
                return
            }

            val className = safely { node.className?.toString() }
            val childCount = safely { node.childCount.coerceAtLeast(0) } ?: 0
            val index = nodes.size
            nodes +=
                SanitizedNode(
                    index = index,
                    parentIndex = parentIndex,
                    resourceIdSuffix = resourceIdSuffix(safely { node.viewIdResourceName }),
                    className = sanitizeClassName(className),
                    role = roleForClassName(className),
                    clickable = safely { node.isClickable } ?: false,
                    scrollable = safely { node.isScrollable } ?: false,
                    selected = safely { node.isSelected } ?: false,
                    visibleToUser = safely { node.isVisibleToUser } ?: false,
                    enabled = safely { node.isEnabled } ?: false,
                    checkable = safely { node.isCheckable } ?: false,
                    childCount = childCount,
                    depth = depth,
                )

            if (depth >= maxDepth) {
                if (childCount > 0) truncated = true
                return
            }

            val inspectedChildren = childCount.coerceAtMost(maxChildrenPerNode)
            if (childCount > inspectedChildren) truncated = true
            for (childPosition in 0 until inspectedChildren) {
                if (nodes.size >= maxNodes) {
                    truncated = true
                    return
                }
                val child = safely { node.getChild(childPosition) } ?: continue
                try {
                    visit(child, parentIndex = index, depth = depth + 1)
                } finally {
                    recycleNodeIfRequired(child)
                }
            }
        }

        visit(root, parentIndex = null, depth = 0)
        return SanitizedNodeTree(metadata = metadata, nodes = nodes, truncated = truncated)
    }

    private inline fun <T> safely(block: () -> T): T? =
        try {
            block()
        } catch (_: RuntimeException) {
            null
        }

    private companion object {
        const val DEFAULT_MAX_NODES = 1_000
        const val DEFAULT_MAX_DEPTH = 40
        const val DEFAULT_MAX_CHILDREN_PER_NODE = 100
    }
}

@Suppress("DEPRECATION")
internal fun recycleNodeIfRequired(node: AccessibilityNodeInfo) {
    if (Build.VERSION.SDK_INT < 33) node.recycle()
}
