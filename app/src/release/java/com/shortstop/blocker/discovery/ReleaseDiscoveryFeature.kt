package com.shortstop.blocker.discovery

import android.accessibilityservice.AccessibilityService
import androidx.compose.runtime.Composable

internal fun discoveryServiceBridge(): DiscoveryServiceBridge = NoOpDiscoveryServiceBridge

@Composable internal fun DiscoveryPanel(serviceEnabled: Boolean, paused: Boolean) = Unit

private object NoOpDiscoveryServiceBridge : DiscoveryServiceBridge {
    override fun onServiceConnected(service: AccessibilityService) = Unit

    override fun onEligibleYouTubeEvent(service: AccessibilityService) = Unit

    override fun onServiceDestroyed(service: AccessibilityService) = Unit
}
