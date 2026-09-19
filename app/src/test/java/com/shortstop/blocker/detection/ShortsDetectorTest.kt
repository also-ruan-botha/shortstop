package com.shortstop.blocker.detection

import com.shortstop.blocker.discovery.NodeRole
import com.shortstop.blocker.discovery.SanitizedNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShortsDetectorTest {
    private val detector = ShortsDetector()

    @Test
    fun completedShortsFixturesAreConfirmed() {
        POSITIVE_FIXTURES.forEach { fixture ->
            val decision = detector.detect(CaptureFixtureLoader.load(fixture))

            assertEquals("fixture=$fixture", DetectionResult.CONFIRMED_SHORTS, decision.result)
            assertEquals(
                "fixture=$fixture",
                DetectionReason.COMPLETE_REEL_STRUCTURE,
                decision.reason,
            )
            assertEquals("fixture=$fixture", 1, decision.ruleVersion)
        }
    }

    @Test
    fun everyOrdinaryFixtureHasTheExpectedFailOpenClassification() {
        ORDINARY_FIXTURES.forEach { (fixture, expected) ->
            val decision = detector.detect(CaptureFixtureLoader.load(fixture))

            assertEquals("fixture=$fixture", expected, decision.result)
            assertNotEquals("fixture=$fixture", DetectionResult.CONFIRMED_SHORTS, decision.result)
        }
    }

    @Test
    fun explicitOrdinaryPlaybackStructuresAreNotShorts() {
        WATCH_FIXTURES.forEach { fixture ->
            val decision = detector.detect(CaptureFixtureLoader.load(fixture))

            assertEquals("fixture=$fixture", DetectionResult.NOT_SHORTS, decision.result)
            assertEquals(
                "fixture=$fixture",
                DetectionReason.ORDINARY_PLAYBACK_STRUCTURE,
                decision.reason,
            )
        }
    }

    @Test
    fun shortsLoadingTransitionFailsOpen() {
        val decision = detector.detect(CaptureFixtureLoader.load("youtube-short-loading.json"))

        assertEquals(DetectionResult.UNKNOWN_LAYOUT, decision.result)
        assertEquals(DetectionReason.NO_RECOGNIZED_STRUCTURE, decision.reason)
    }

    @Test
    fun eachIncompletePositiveSignalStaysPossible() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val requiredIds =
            listOf(
                "reel_recycler",
                "reel_player_page_container",
                "reel_playback_loading_spinner",
            )

        requiredIds.forEach { removedId ->
            val partial =
                fixture.copy(
                    nodes =
                        fixture.nodes.map {
                            if (it.resourceIdSuffix == removedId) {
                                it.copy(resourceIdSuffix = null)
                            } else {
                                it
                            }
                        }
                )
            val decision = detector.detect(partial)

            assertEquals("removed=$removedId", DetectionResult.POSSIBLE_SHORTS, decision.result)
            assertEquals(
                "removed=$removedId",
                DetectionReason.PARTIAL_REEL_STRUCTURE,
                decision.reason,
            )
        }
    }

    @Test
    fun matchingNodesWithWrongRelationshipsStayPossible() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val player = fixture.nodes.single { it.resourceIdSuffix == "reel_player_page_container" }
        val alternativeParent =
            fixture.nodes.single { it.resourceIdSuffix == "nerd_stats_container" }
        val misplacedPlayer = player.copy(parentIndex = alternativeParent.index)
        val tree =
            fixture.copy(
                nodes =
                    fixture.nodes.map {
                        when (it.index) {
                            player.index -> misplacedPlayer
                            alternativeParent.index -> it.copy(childCount = it.childCount + 1)
                            else -> it
                        }
                    }
            )

        assertEquals(DetectionResult.POSSIBLE_SHORTS, detector.detect(tree).result)
    }

    @Test
    fun ordinaryPlaybackVetoWinsOverCompleteReelEvidence() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val changed =
            fixture.copy(
                nodes =
                    fixture.nodes.mapIndexed { position, node ->
                        if (position == fixture.nodes.lastIndex) {
                            node.copy(resourceIdSuffix = "watch_player")
                        } else {
                            node
                        }
                    }
            )

        assertEquals(DetectionResult.NOT_SHORTS, detector.detect(changed).result)
    }

    @Test
    fun duplicateCompleteReelStructuresStayPossible() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val coordinator =
            fixture.nodes.single {
                it.resourceIdSuffix == "browse_fragment_layout_coordinator_layout"
            }
        val reel = fixture.nodes.single { it.resourceIdSuffix == "reel_recycler" }
        val player = fixture.nodes.single { it.resourceIdSuffix == "reel_player_page_container" }
        val spinner =
            fixture.nodes.single { it.resourceIdSuffix == "reel_playback_loading_spinner" }
        val duplicates =
            listOf(
                reel.copy(index = 100, childCount = 1),
                player.copy(index = 101, parentIndex = 100, childCount = 0),
                spinner.copy(index = 102),
            )
        val tree =
            fixture.copy(
                nodes =
                    fixture.nodes.map {
                        if (it.index == coordinator.index) {
                            it.copy(childCount = it.childCount + 2)
                        } else {
                            it
                        }
                    } + duplicates
            )

        val decision = detector.detect(tree)
        assertEquals(DetectionResult.POSSIBLE_SHORTS, decision.result)
        assertEquals(DetectionReason.AMBIGUOUS_REEL_STRUCTURE, decision.reason)
    }

    @Test
    fun truncatedAndOversizedTreesFailOpen() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        assertUnknown(fixture.copy(truncated = true), DetectionReason.TRUNCATED_TREE)

        val root = node(index = 0, parentIndex = null, depth = 0, childCount = 1_000)
        val children = (1..1_000).map { node(index = it, parentIndex = 0, depth = 1) }
        assertUnknown(
            fixture.copy(nodes = listOf(root) + children),
            DetectionReason.MALFORMED_TREE,
        )
    }

    @Test
    fun malformedIndexesParentsAndDepthFailOpen() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val firstChild = fixture.nodes.first { it.parentIndex != null }

        assertUnknown(
            fixture.copy(nodes = fixture.nodes + fixture.nodes.last()),
            DetectionReason.MALFORMED_TREE,
        )
        assertUnknown(
            fixture.copy(
                nodes =
                    fixture.nodes.map {
                        if (it == firstChild) it.copy(parentIndex = 99_999) else it
                    }
            ),
            DetectionReason.MALFORMED_TREE,
        )
        assertUnknown(
            fixture.copy(
                nodes = fixture.nodes.map { if (it == firstChild) it.copy(depth = 40) else it }
            ),
            DetectionReason.MALFORMED_TREE,
        )
    }

    @Test
    fun unsupportedSchemaPackageAndTargetVersionFailOpen() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")

        assertUnknown(fixture.copy(schemaVersion = 2), DetectionReason.UNSUPPORTED_SCHEMA)
        assertUnknown(
            fixture.copy(metadata = fixture.metadata.copy(targetPackage = "example.invalid")),
            DetectionReason.UNSUPPORTED_PACKAGE,
        )
        assertUnknown(
            fixture.copy(metadata = fixture.metadata.copy(targetVersionName = "21.36.0")),
            DetectionReason.UNSUPPORTED_TARGET_VERSION,
        )
        assertUnknown(
            fixture.copy(metadata = fixture.metadata.copy(targetVersionCode = 1L)),
            DetectionReason.UNSUPPORTED_TARGET_VERSION,
        )
    }

    @Test
    fun ambiguousRuleVersionSelectionFailsOpen() {
        val fixture = CaptureFixtureLoader.load("youtube-shortscreen.json")
        val duplicatedRules = BundledShortsRules.all.single().copy(ruleVersion = 2)
        val decision = ShortsDetector(BundledShortsRules.all + duplicatedRules).detect(fixture)

        assertEquals(DetectionResult.UNKNOWN_LAYOUT, decision.result)
        assertEquals(DetectionReason.AMBIGUOUS_RULE_SELECTION, decision.reason)
    }

    private fun assertUnknown(
        tree: com.shortstop.blocker.discovery.SanitizedNodeTree,
        reason: DetectionReason,
    ) {
        val decision = detector.detect(tree)
        assertEquals(DetectionResult.UNKNOWN_LAYOUT, decision.result)
        assertEquals(reason, decision.reason)
    }

    private fun node(
        index: Int,
        parentIndex: Int?,
        depth: Int,
        childCount: Int = 0,
    ) =
        SanitizedNode(
            index = index,
            parentIndex = parentIndex,
            resourceIdSuffix = null,
            className = "android.view.View",
            role = NodeRole.VIEW,
            clickable = false,
            scrollable = false,
            selected = false,
            visibleToUser = true,
            enabled = true,
            checkable = false,
            childCount = childCount,
            depth = depth,
        )

    private companion object {
        val POSITIVE_FIXTURES = listOf("youtube-shortscreen.json", "youtube-other-shortscreen.json")

        val WATCH_FIXTURES =
            listOf(
                "youtube-regular-playback.json",
                "youtube-fullscreen-playback.json",
                "youtube-ordinary-video-loading.json",
                "youtube-short-ordinary-video.json",
                "youtube-short-ordinary-video-picture-in-picture.json",
            )

        val ORDINARY_FIXTURES =
            mapOf(
                "youtube-main-screen.json" to DetectionResult.UNKNOWN_LAYOUT,
                "youtube-subscription-screen.json" to DetectionResult.UNKNOWN_LAYOUT,
                "youtube-channel-screen.json" to DetectionResult.UNKNOWN_LAYOUT,
                "youtube-account-screen.json" to DetectionResult.UNKNOWN_LAYOUT,
                "youtube-regular-playback.json" to DetectionResult.NOT_SHORTS,
                "youtube-fullscreen-playback.json" to DetectionResult.NOT_SHORTS,
                "youtube-ordinary-video-loading.json" to DetectionResult.NOT_SHORTS,
                "youtube-search-autoplay.json" to DetectionResult.UNKNOWN_LAYOUT,
                "youtube-short-ordinary-video.json" to DetectionResult.NOT_SHORTS,
                "youtube-short-ordinary-video-picture-in-picture.json" to
                    DetectionResult.NOT_SHORTS,
            )
    }
}
