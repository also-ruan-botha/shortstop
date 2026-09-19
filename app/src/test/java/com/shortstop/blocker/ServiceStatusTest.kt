package com.shortstop.blocker

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusTest {
    @Test
    fun disabledTakesPrecedence() {
        assertEquals(
            ServiceStatus.DISABLED,
            serviceStatus(
                serviceEnabled = false,
                paused = true,
                automationStatus = AutomationStatus.UNSUPPORTED_LAYOUT,
            ),
        )
    }

    @Test
    fun pausedTakesPrecedenceOverUnsupportedLayout() {
        assertEquals(
            ServiceStatus.PAUSED,
            serviceStatus(
                serviceEnabled = true,
                paused = true,
                automationStatus = AutomationStatus.UNSUPPORTED_LAYOUT,
            ),
        )
    }

    @Test
    fun enabledServiceCanReportUnsupportedLayout() {
        assertEquals(
            ServiceStatus.UNSUPPORTED_LAYOUT,
            serviceStatus(
                serviceEnabled = true,
                paused = false,
                automationStatus = AutomationStatus.UNSUPPORTED_LAYOUT,
            ),
        )
    }

    @Test
    fun enabledIsTheActiveDefault() {
        assertEquals(
            ServiceStatus.ENABLED,
            serviceStatus(
                serviceEnabled = true,
                paused = false,
                automationStatus = AutomationStatus.MONITORING,
            ),
        )
    }
}
