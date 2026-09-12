package com.shortstop.blocker.discovery

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant

internal sealed interface CurrentTreeResult {
    data class Available(val tree: SanitizedNodeTree) : CurrentTreeResult

    data object TargetNotActive : CurrentTreeResult

    data object Unavailable : CurrentTreeResult
}

internal class CurrentYouTubeTreeSource(
    context: Context,
    private val treeReader: AccessibilityTreeReader = AccessibilityTreeReader(),
) {
    private val packageManager = context.packageManager

    fun read(service: AccessibilityService): CurrentTreeResult {
        val root =
            try {
                service.rootInActiveWindow
            } catch (_: RuntimeException) {
                null
            } ?: return CurrentTreeResult.Unavailable

        return try {
            val rootPackage =
                try {
                    root.packageName?.toString()
                } catch (_: RuntimeException) {
                    null
                }
            if (rootPackage != YOUTUBE_PACKAGE) {
                CurrentTreeResult.TargetNotActive
            } else {
                CurrentTreeResult.Available(treeReader.read(root, captureMetadata()))
            }
        } catch (_: RuntimeException) {
            CurrentTreeResult.Unavailable
        } finally {
            recycleNodeIfRequired(root)
        }
    }

    private fun captureMetadata(): CaptureMetadata {
        val packageInfo = packageManager.youtubePackageInfo()
        return CaptureMetadata(
            capturedAtUtc = Instant.now().toString(),
            targetPackage = YOUTUBE_PACKAGE,
            targetVersionName = packageInfo?.versionName ?: "unknown",
            targetVersionCode = packageInfo?.safeLongVersionCode() ?: -1,
            androidRelease = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
        )
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.youtubePackageInfo(): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                getPackageInfo(YOUTUBE_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                getPackageInfo(YOUTUBE_PACKAGE, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    @Suppress("DEPRECATION")
    private fun PackageInfo.safeLongVersionCode(): Long =
        if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
