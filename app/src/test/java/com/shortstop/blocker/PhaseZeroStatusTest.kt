package com.shortstop.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseZeroStatusTest {
    @Test
    fun statusMakesInactivePrototypeExplicit() {
        assertEquals("Phase 0 ready", PhaseZeroStatus.HEADING)
        assertTrue(PhaseZeroStatus.DESCRIPTION.contains("not active"))
    }
}
