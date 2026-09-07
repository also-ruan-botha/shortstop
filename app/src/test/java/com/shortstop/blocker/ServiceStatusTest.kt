package com.shortstop.blocker

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusTest {
    @Test
    fun disabledTakesPrecedence() {
        assertEquals(
            ServiceStatus.DISABLED,
            serviceStatus(serviceEnabled = false, paused = true, unsupportedLayout = true),
        )
    }

    @Test
    fun pausedTakesPrecedenceOverUnsupportedLayout() {
        assertEquals(
            ServiceStatus.PAUSED,
            serviceStatus(serviceEnabled = true, paused = true, unsupportedLayout = true),
        )
    }

    @Test
    fun enabledServiceCanReportUnsupportedLayout() {
        assertEquals(
            ServiceStatus.UNSUPPORTED_LAYOUT,
            serviceStatus(serviceEnabled = true, paused = false, unsupportedLayout = true),
        )
    }

    @Test
    fun enabledIsTheActiveDefault() {
        assertEquals(
            ServiceStatus.ENABLED,
            serviceStatus(serviceEnabled = true, paused = false, unsupportedLayout = false),
        )
    }
}
