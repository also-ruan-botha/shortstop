package com.shortstop.blocker.discovery

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

internal fun discoveryServiceBridge(): DiscoveryServiceBridge = DebugDiscoveryRuntime

@Composable
internal fun DiscoveryPanel(serviceEnabled: Boolean, paused: Boolean) {
    val state by DebugDiscoveryRuntime.state.collectAsState()
    val context = LocalContext.current
    val canArm = serviceEnabled && !paused && state.mode != DebugDiscoveryMode.ARMED
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Debug discovery",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Captures structural properties only. Text, descriptions, bounds, screenshots, and media are excluded."
            )
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = DebugDiscoveryRuntime::arm,
                enabled = canArm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    if (state.mode == DebugDiscoveryMode.ARMED) "Waiting for YouTube…"
                    else "Arm one capture"
                )
            }
            if (state.tree != null) {
                OutlinedButton(
                    onClick = { DebugDiscoveryRuntime.exportLatest(context) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Export sanitized fixture")
                }
            }
        }
    }
}

private enum class DebugDiscoveryMode {
    IDLE,
    ARMED,
    CAPTURED,
    EXPORTED,
    ERROR,
}

private data class DebugDiscoveryState(
    val mode: DebugDiscoveryMode = DebugDiscoveryMode.IDLE,
    val message: String = "Enable the service, arm a capture, then switch to YouTube.",
    val tree: SanitizedNodeTree? = null,
)

private object DebugDiscoveryRuntime : DiscoveryServiceBridge {
    private val mutableState = MutableStateFlow(DebugDiscoveryState())
    val state = mutableState.asStateFlow()

    fun arm() {
        mutableState.value =
            DebugDiscoveryState(
                mode = DebugDiscoveryMode.ARMED,
                message = "Armed. Switch to the YouTube screen you want to inspect.",
            )
    }

    fun exportLatest(context: Context) {
        val tree = mutableState.value.tree ?: return
        try {
            val directory = File(context.filesDir, "debug-fixtures").apply { mkdirs() }
            val file = File(directory, "youtube-${System.currentTimeMillis()}.json")
            file.writeText(FixtureJsonEncoder.encode(tree), Charsets.UTF_8)
            mutableState.value =
                mutableState.value.copy(
                    mode = DebugDiscoveryMode.EXPORTED,
                    message = "Exported locally to files/debug-fixtures/${file.name}",
                )
        } catch (_: IOException) {
            mutableState.value =
                mutableState.value.copy(
                    mode = DebugDiscoveryMode.ERROR,
                    message = "The local fixture could not be written.",
                )
        }
    }

    override fun onServiceConnected(service: AccessibilityService) = Unit

    override fun onEligibleYouTubeEvent(service: AccessibilityService) {
        if (mutableState.value.mode != DebugDiscoveryMode.ARMED) return
        val root =
            try {
                service.rootInActiveWindow
            } catch (_: RuntimeException) {
                null
            } ?: return

        try {
            if (root.packageName?.toString() != YOUTUBE_PACKAGE) return
            val tree = AccessibilityTreeReader().read(root, captureMetadata(service))
            mutableState.value =
                DebugDiscoveryState(
                    mode = DebugDiscoveryMode.CAPTURED,
                    message =
                        "Captured ${tree.nodes.size} structural nodes" +
                            if (tree.truncated) " (safely truncated)." else ".",
                    tree = tree,
                )
        } catch (_: RuntimeException) {
            mutableState.value =
                DebugDiscoveryState(
                    mode = DebugDiscoveryMode.ERROR,
                    message = "Capture failed safely. No fixture was retained.",
                )
        } finally {
            recycleRootIfRequired(root)
        }
    }

    override fun onServiceDestroyed(service: AccessibilityService) {
        if (mutableState.value.mode == DebugDiscoveryMode.ARMED) {
            mutableState.value =
                DebugDiscoveryState(message = "The service stopped before a tree was captured.")
        }
    }

    private fun captureMetadata(context: Context): CaptureMetadata {
        val packageInfo =
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getPackageInfo(
                        YOUTUBE_PACKAGE,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(YOUTUBE_PACKAGE, 0)
                }
            } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                null
            }
        return CaptureMetadata(
            capturedAtUtc = Instant.now().toString(),
            targetPackage = YOUTUBE_PACKAGE,
            targetVersionName = packageInfo?.versionName ?: "unknown",
            targetVersionCode =
                packageInfo?.let {
                    if (Build.VERSION.SDK_INT >= 28) it.longVersionCode
                    else {
                        @Suppress("DEPRECATION") it.versionCode.toLong()
                    }
                } ?: -1,
            androidRelease = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
        )
    }

    private fun recycleRootIfRequired(root: android.view.accessibility.AccessibilityNodeInfo) =
        recycleNodeIfRequired(root)
}
