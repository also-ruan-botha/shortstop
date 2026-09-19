package com.shortstop.blocker.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeHomeTargetResolverTest {
    private val resolver = YoutubeHomeTargetResolver()

    @Test
    fun bothConfirmedShortsFixturesResolveTheFirstPivotButton() {
        listOf("youtube-shortscreen.json", "youtube-other-shortscreen.json").forEach { fixture ->
            val tree = CaptureFixtureLoader.load(fixture)
            val target = requireNotNull(resolver.resolve(tree))
            val pivot = tree.nodes.single { it.resourceIdSuffix == "pivot_bar" }
            val row = tree.nodes.single { it.parentIndex == pivot.index }
            val firstTab = tree.nodes.filter { it.parentIndex == row.index }.minBy { it.index }

            assertEquals(firstTab.index, target.index)
            assertTrue(target.clickable)
            assertTrue(target.visibleToUser)
            assertTrue(target.enabled)
        }
    }

    @Test
    fun missingPivotBarFailsOpen() {
        val tree = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val withoutPivot =
            tree.copy(
                nodes =
                    tree.nodes.map { node ->
                        if (node.resourceIdSuffix == "pivot_bar") {
                            node.copy(resourceIdSuffix = null)
                        } else {
                            node
                        }
                    }
            )

        assertNull(resolver.resolve(withoutPivot))
    }

    @Test
    fun disabledOrNonClickableFirstTabFailsOpen() {
        val tree = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val pivot = tree.nodes.single { it.resourceIdSuffix == "pivot_bar" }
        val row = tree.nodes.single { it.parentIndex == pivot.index }
        val firstTab = tree.nodes.filter { it.parentIndex == row.index }.minBy { it.index }

        assertNull(
            resolver.resolve(
                tree.copy(
                    nodes =
                        tree.nodes.map { node ->
                            if (node.index == firstTab.index) {
                                node.copy(clickable = false, enabled = false)
                            } else {
                                node
                            }
                        }
                )
            )
        )
    }
}
