package com.shortstop.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Phase 1 service shell. Detection and navigation are intentionally absent until their later
 * phases.
 */
class ShortStopAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var paused = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            UserPreferencesRepository(applicationContext).preferences.collect { preferences ->
                paused = preferences.paused
            }
        }
    }

    // This function shows that we only care when the package (app) is YouTube.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (paused || event?.packageName?.toString() != YOUTUBE_PACKAGE) return

        // Phase 1 deliberately performs no node inspection and no global action.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
