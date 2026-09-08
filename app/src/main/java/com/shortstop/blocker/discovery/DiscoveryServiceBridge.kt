package com.shortstop.blocker.discovery

import android.accessibilityservice.AccessibilityService

internal interface DiscoveryServiceBridge {
    fun onServiceConnected(service: AccessibilityService)

    fun onEligibleYouTubeEvent(service: AccessibilityService)

    fun onServiceDestroyed(service: AccessibilityService)
}
