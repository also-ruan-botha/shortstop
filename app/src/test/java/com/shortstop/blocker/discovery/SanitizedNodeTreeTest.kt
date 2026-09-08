package com.shortstop.blocker.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanitizedNodeTreeTest {
    @Test
    fun resourceIdKeepsOnlyTheStructuralSuffix() {
        assertEquals("reel_player", resourceIdSuffix("com.google.android.youtube:id/reel_player"))
        assertEquals("unsafe_value", resourceIdSuffix("package:id/unsafe value"))
        assertNull(resourceIdSuffix(null))
    }

    @Test
    fun roleComesFromTheFrameworkClassRatherThanVisibleText() {
        assertEquals(NodeRole.BUTTON, roleForClassName("android.widget.Button"))
        assertEquals(NodeRole.LIST, roleForClassName("androidx.recyclerview.widget.RecyclerView"))
        assertEquals(NodeRole.UNKNOWN, roleForClassName("custom.component.Card"))
    }

    @Test
    fun fixtureContainsStructureAndNoContentFields() {
        val tree =
            SanitizedNodeTree(
                metadata =
                    CaptureMetadata(
                        capturedAtUtc = "2026-09-07T12:00:00Z",
                        targetPackage = "com.google.android.youtube",
                        targetVersionName = "20.00.00",
                        targetVersionCode = 20,
                        androidRelease = "16",
                        androidSdk = 36,
                    ),
                nodes =
                    listOf(
                        SanitizedNode(
                            index = 0,
                            parentIndex = null,
                            resourceIdSuffix = "root",
                            className = "android.view.ViewGroup",
                            role = NodeRole.LIST_ITEM,
                            clickable = false,
                            scrollable = false,
                            selected = false,
                            visibleToUser = true,
                            enabled = true,
                            checkable = false,
                            childCount = 1,
                            depth = 0,
                        ),
                        SanitizedNode(
                            index = 1,
                            parentIndex = 0,
                            resourceIdSuffix = "player",
                            className = "android.view.View",
                            role = NodeRole.VIEW,
                            clickable = true,
                            scrollable = false,
                            selected = true,
                            visibleToUser = true,
                            enabled = true,
                            checkable = false,
                            childCount = 0,
                            depth = 1,
                        ),
                    ),
                truncated = false,
            )

        val json = FixtureJsonEncoder.encode(tree)

        assertTrue(json.contains("\"parentIndex\": 0"))
        assertTrue(json.contains("\"resourceIdSuffix\": \"player\""))
        assertFalse(json.contains("\"text\""))
        assertFalse(json.contains("contentDescription"))
        assertFalse(json.contains("bounds"))
        assertFalse(json.contains("screenshot"))
    }
}
