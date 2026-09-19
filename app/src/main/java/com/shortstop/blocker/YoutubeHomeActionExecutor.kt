package com.shortstop.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.shortstop.blocker.detection.DetectionResult
import com.shortstop.blocker.detection.ShortsDetector
import com.shortstop.blocker.detection.YoutubeHomeTargetResolver
import com.shortstop.blocker.detection.YoutubeHomeTargetSignature
import com.shortstop.blocker.discovery.AccessibilityTreeReader
import com.shortstop.blocker.discovery.CaptureMetadata
import com.shortstop.blocker.discovery.recycleNodeIfRequired

internal class YoutubeHomeActionExecutor(
    private val detector: ShortsDetector,
    private val treeReader: AccessibilityTreeReader = AccessibilityTreeReader(),
    private val targetResolver: YoutubeHomeTargetResolver = YoutubeHomeTargetResolver(),
) {
    fun perform(service: AccessibilityService): Boolean {
        val root = safely { service.rootInActiveWindow } ?: return false
        return try {
            if (safely { root.packageName?.toString() } != YOUTUBE_PACKAGE) return false

            val freshTree = treeReader.read(root, actionMetadata())
            if (detector.detect(freshTree).result != DetectionResult.CONFIRMED_SHORTS) return false
            if (targetResolver.resolve(freshTree) == null) return false

            clickStructurallyVerifiedHomeTab(root)
        } finally {
            recycleNodeIfRequired(root)
        }
    }

    private fun clickStructurallyVerifiedHomeTab(root: AccessibilityNodeInfo): Boolean {
        val pivotNodes =
            safely { root.findAccessibilityNodeInfosByViewId(PIVOT_BAR_RESOURCE_ID) }.orEmpty()
        if (pivotNodes.size != 1) {
            pivotNodes.forEach(::recycleNodeIfRequired)
            return false
        }

        val pivot = pivotNodes.single()
        var tabRow: AccessibilityNodeInfo? = null
        var homeTab: AccessibilityNodeInfo? = null
        return try {
            if (!pivot.matchesPivotBar()) return false
            val resolvedTabRow = safely { pivot.getChild(0) } ?: return false
            tabRow = resolvedTabRow
            if (!resolvedTabRow.matchesTabRow()) return false
            val resolvedHomeTab = safely { resolvedTabRow.getChild(0) } ?: return false
            homeTab = resolvedHomeTab
            if (!resolvedHomeTab.matchesHomeTab()) return false
            safely { resolvedHomeTab.performAction(AccessibilityNodeInfo.ACTION_CLICK) } ?: false
        } finally {
            homeTab?.let(::recycleNodeIfRequired)
            tabRow?.let(::recycleNodeIfRequired)
            recycleNodeIfRequired(pivot)
        }
    }

    private fun AccessibilityNodeInfo.matchesPivotBar(): Boolean =
        safely { viewIdResourceName } == PIVOT_BAR_RESOURCE_ID &&
            safely { className?.toString() } == YoutubeHomeTargetSignature.PIVOT_BAR_CLASS_NAME &&
            safely { childCount } == 1 &&
            safely { isVisibleToUser } == true &&
            safely { isEnabled } == true

    private fun AccessibilityNodeInfo.matchesTabRow(): Boolean =
        safely { className?.toString() } == YoutubeHomeTargetSignature.TAB_ROW_CLASS_NAME &&
            safely { childCount } == YoutubeHomeTargetSignature.TAB_COUNT

    private fun AccessibilityNodeInfo.matchesHomeTab(): Boolean =
        safely { className?.toString() } == YoutubeHomeTargetSignature.TAB_CLASS_NAME &&
            safely { isClickable } == true &&
            safely { isVisibleToUser } == true &&
            safely { isEnabled } == true

    private inline fun <T> safely(block: () -> T): T? =
        try {
            block()
        } catch (_: RuntimeException) {
            null
        }

    private fun actionMetadata() =
        CaptureMetadata(
            capturedAtUtc = "action-check",
            targetPackage = YOUTUBE_PACKAGE,
            targetVersionName = "action-check",
            targetVersionCode = -1,
            androidRelease = "action-check",
            androidSdk = 0,
        )

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val PIVOT_BAR_RESOURCE_ID =
            "$YOUTUBE_PACKAGE:id/${YoutubeHomeTargetSignature.PIVOT_BAR_RESOURCE_ID_SUFFIX}"
    }
}
