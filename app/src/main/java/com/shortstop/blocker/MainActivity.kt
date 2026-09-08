package com.shortstop.blocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.shortstop.blocker.discovery.DiscoveryPanel
import com.shortstop.blocker.ui.theme.ShortStopTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val preferences by lazy { UserPreferencesRepository(applicationContext) }
    private var uiState by mutableStateOf(ShortStopUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            preferences.preferences.collect { current ->
                uiState =
                    uiState.copy(
                        onboardingAcknowledged = current.onboardingAcknowledged,
                        paused = current.paused,
                    )
            }
        }

        setContent {
            ShortStopTheme {
                ShortStopApp(
                    state = uiState,
                    onAcknowledgementChanged = {
                        uiState = uiState.copy(disclosureChecked = it)
                    },
                    onContinueToSettings = {
                        lifecycleScope.launch {
                            preferences.acknowledgeOnboarding()
                            openAccessibilitySettings()
                        }
                    },
                    onPausedChanged = { paused ->
                        lifecycleScope.launch { preferences.setPaused(paused) }
                    },
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiState = uiState.copy(serviceEnabled = isShortStopServiceEnabled())
    }

    private fun isShortStopServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java)
        val expected = ComponentName(this, ShortStopAccessibilityService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expected
            }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

internal data class ShortStopUiState(
    val onboardingAcknowledged: Boolean = false,
    val disclosureChecked: Boolean = false,
    val serviceEnabled: Boolean = false,
    val paused: Boolean = false,
    val unsupportedLayout: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortStopApp(
    state: ShortStopUiState,
    onAcknowledgementChanged: (Boolean) -> Unit,
    onContinueToSettings: () -> Unit,
    onPausedChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("ShortStop") }) },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.onboardingAcknowledged) {
                StatusScreen(
                    state = state,
                    onPausedChanged = onPausedChanged,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )
            } else {
                OnboardingScreen(
                    checked = state.disclosureChecked,
                    onCheckedChange = onAcknowledgementChanged,
                    onContinue = onContinueToSettings,
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier.widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Leave Shorts. Keep YouTube.",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text =
                "ShortStop needs Android Accessibility access to recognize the Shorts interface in the official YouTube app.",
            style = MaterialTheme.typography.bodyLarge,
        )
        DisclosureCard()
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = onCheckedChange,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text(
                text = "I understand how ShortStop uses Accessibility access",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(
            onClick = onContinue,
            enabled = checked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Continue to Accessibility settings")
        }
        Text(
            text =
                "Android always requires you to enable this access yourself. ShortStop cannot enable it for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DisclosureCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "What access is used for",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "When blocking is active, ShortStop examines the structure of YouTube’s on-screen interface only while YouTube is active. After it confidently detects Shorts, it presses Back."
            )
            HorizontalDivider()
            Text(
                "ShortStop does not capture screenshots, record what you watch or type, collect titles or searches, or send accessibility data off your device."
            )
            Text(
                "You can pause ShortStop here at any time, or disable its Accessibility access in Android Settings."
            )
        }
    }
}

@Composable
private fun StatusScreen(
    state: ShortStopUiState,
    onPausedChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val status =
        serviceStatus(
            serviceEnabled = state.serviceEnabled,
            paused = state.paused,
            unsupportedLayout = state.unsupportedLayout,
        )
    Column(
        modifier =
            Modifier.widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StatusCard(status)
        if (state.serviceEnabled) {
            PauseControl(paused = state.paused, onPausedChanged = onPausedChanged)
        } else {
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("Enable in Accessibility settings")
            }
        }
        DiscoveryPanel(serviceEnabled = state.serviceEnabled, paused = state.paused)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Private by design",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "ShortStop reacts only inside the official YouTube app. Processing stays on this device, and the app has no Internet permission."
                )
            }
        }
        Text(
            text =
                "To disable access completely, open Accessibility settings and switch ShortStop off.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Open Accessibility settings")
        }
    }
}

@Composable
private fun StatusCard(status: ServiceStatus) {
    val containerColor =
        when (status) {
            ServiceStatus.ENABLED -> MaterialTheme.colorScheme.primaryContainer
            ServiceStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
            ServiceStatus.UNSUPPORTED_LAYOUT -> MaterialTheme.colorScheme.tertiaryContainer
            ServiceStatus.DISABLED -> MaterialTheme.colorScheme.errorContainer
        }
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = status.heading,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(text = status.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PauseControl(paused: Boolean, onPausedChanged: (Boolean) -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(
                    value = paused,
                    role = Role.Switch,
                    onValueChange = onPausedChanged,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Pause ShortStop", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep access enabled without reacting to YouTube",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = paused, onCheckedChange = null)
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    ShortStopTheme {
        ShortStopApp(
            state = ShortStopUiState(disclosureChecked = true),
            onAcknowledgementChanged = {},
            onContinueToSettings = {},
            onPausedChanged = {},
            onOpenAccessibilitySettings = {},
        )
    }
}
