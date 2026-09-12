package com.shortstop.blocker.detection

import com.shortstop.blocker.discovery.NodeRole
import com.shortstop.blocker.discovery.SanitizedNode
import com.shortstop.blocker.discovery.SanitizedNodeTree

internal object YoutubeHomeTargetSignature {
    const val PIVOT_BAR_RESOURCE_ID_SUFFIX = "pivot_bar"
    const val PIVOT_BAR_CLASS_NAME = "android.widget.HorizontalScrollView"
    const val TAB_ROW_CLASS_NAME = "android.widget.LinearLayout"
    const val TAB_CLASS_NAME = "android.widget.Button"
    const val TAB_COUNT = 5
}

internal class YoutubeHomeTargetResolver {
    fun resolve(tree: SanitizedNodeTree): SanitizedNode? {
        if (tree.truncated) return null
        val childrenByParent = tree.nodes.groupBy { it.parentIndex }
        val candidates =
            tree.nodes.mapNotNull { pivot ->
                if (!pivot.matchesPivotBar()) return@mapNotNull null
                val pivotChildren = childrenByParent[pivot.index].orEmpty()
                if (pivot.childCount != 1 || pivotChildren.size != 1) return@mapNotNull null

                val tabRow = pivotChildren.single()
                val tabs = childrenByParent[tabRow.index].orEmpty().sortedBy { it.index }
                if (
                    tabRow.className != YoutubeHomeTargetSignature.TAB_ROW_CLASS_NAME ||
                        tabRow.childCount != YoutubeHomeTargetSignature.TAB_COUNT ||
                        tabs.size != YoutubeHomeTargetSignature.TAB_COUNT
                ) {
                    return@mapNotNull null
                }

                tabs.first().takeIf { it.matchesHomeTab() }
            }
        return candidates.singleOrNull()
    }

    private fun SanitizedNode.matchesPivotBar(): Boolean =
        resourceIdSuffix == YoutubeHomeTargetSignature.PIVOT_BAR_RESOURCE_ID_SUFFIX &&
            className == YoutubeHomeTargetSignature.PIVOT_BAR_CLASS_NAME &&
            role == NodeRole.SCROLL_CONTAINER &&
            visibleToUser &&
            enabled

    private fun SanitizedNode.matchesHomeTab(): Boolean =
        className == YoutubeHomeTargetSignature.TAB_CLASS_NAME &&
            role == NodeRole.BUTTON &&
            clickable &&
            visibleToUser &&
            enabled
}
