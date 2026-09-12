package com.shortstop.blocker.detection

import com.shortstop.blocker.discovery.NodeRole
import com.shortstop.blocker.discovery.SANITIZED_TREE_SCHEMA_VERSION
import com.shortstop.blocker.discovery.SanitizedNode
import com.shortstop.blocker.discovery.SanitizedNodeTree

internal enum class DetectionResult {
    NOT_SHORTS,
    POSSIBLE_SHORTS,
    CONFIRMED_SHORTS,
    UNKNOWN_LAYOUT,
}

internal enum class DetectionReason {
    ORDINARY_PLAYBACK_STRUCTURE,
    COMPLETE_REEL_STRUCTURE,
    PARTIAL_REEL_STRUCTURE,
    NO_RECOGNIZED_STRUCTURE,
    UNSUPPORTED_PACKAGE,
    UNSUPPORTED_SCHEMA,
    AMBIGUOUS_RULE_SELECTION,
    TRUNCATED_TREE,
    MALFORMED_TREE,
    AMBIGUOUS_REEL_STRUCTURE,
}

internal data class DetectionDecision(
    val result: DetectionResult,
    val reason: DetectionReason,
    val ruleVersion: Int? = null,
)

internal data class NodeSignature(
    val resourceIdSuffix: String,
    val className: String,
    val role: NodeRole? = null,
) {
    fun matches(node: SanitizedNode): Boolean =
        node.resourceIdSuffix == resourceIdSuffix &&
            node.className == className &&
            (role == null || node.role == role)
}

internal data class ShortsRuleSet(
    val ruleVersion: Int,
    val observedTargetVersionNames: Set<String>,
    val observedTargetVersionCodes: Set<Long>,
    val reelList: NodeSignature,
    val playerPage: NodeSignature,
    val loadingSpinner: NodeSignature,
    val ordinaryPlaybackResourceIds: Set<String>,
)

internal object BundledShortsRules {
    val all: List<ShortsRuleSet> =
        listOf(
            ShortsRuleSet(
                ruleVersion = 1,
                observedTargetVersionNames = setOf("21.35.442"),
                observedTargetVersionCodes = setOf(1_561_295_275L),
                reelList =
                    NodeSignature(
                        resourceIdSuffix = "reel_recycler",
                        className = "android.support.v7.widget.RecyclerView",
                        role = NodeRole.LIST,
                    ),
                playerPage =
                    NodeSignature(
                        resourceIdSuffix = "reel_player_page_container",
                        className = "android.widget.FrameLayout",
                    ),
                loadingSpinner =
                    NodeSignature(
                        resourceIdSuffix = "reel_playback_loading_spinner",
                        className = "android.widget.ProgressBar",
                    ),
                ordinaryPlaybackResourceIds =
                    setOf(
                        "next_gen_watch_layout_no_player_fragment_container",
                        "watch_panel",
                        "watch_player",
                    ),
            )
        )
}

internal class ShortsDetector(private val ruleSets: List<ShortsRuleSet> = BundledShortsRules.all) {
    fun detect(tree: SanitizedNodeTree): DetectionDecision {
        if (tree.metadata.targetPackage != YOUTUBE_PACKAGE) {
            return unknown(DetectionReason.UNSUPPORTED_PACKAGE)
        }
        if (tree.schemaVersion != SANITIZED_TREE_SCHEMA_VERSION) {
            return unknown(DetectionReason.UNSUPPORTED_SCHEMA)
        }
        if (tree.truncated) return unknown(DetectionReason.TRUNCATED_TREE)
        if (!tree.isStructurallyValid()) return unknown(DetectionReason.MALFORMED_TREE)

        if (ruleSets.size != 1) return unknown(DetectionReason.AMBIGUOUS_RULE_SELECTION)
        val rules = ruleSets.single()

        if (tree.nodes.any { it.resourceIdSuffix in rules.ordinaryPlaybackResourceIds }) {
            return DetectionDecision(
                result = DetectionResult.NOT_SHORTS,
                reason = DetectionReason.ORDINARY_PLAYBACK_STRUCTURE,
                ruleVersion = rules.ruleVersion,
            )
        }

        val childrenByParent = tree.nodes.groupBy { it.parentIndex }
        val reelLists = tree.nodes.filter(rules.reelList::matches)
        val completeMatches = reelLists.filter { reelList ->
            val directChildren = childrenByParent[reelList.index].orEmpty()
            directChildren.any(rules.playerPage::matches)
        }

        if (completeMatches.size == 1) {
            return DetectionDecision(
                result = DetectionResult.CONFIRMED_SHORTS,
                reason = DetectionReason.COMPLETE_REEL_STRUCTURE,
                ruleVersion = rules.ruleVersion,
            )
        }
        if (completeMatches.size > 1) {
            return DetectionDecision(
                result = DetectionResult.POSSIBLE_SHORTS,
                reason = DetectionReason.AMBIGUOUS_REEL_STRUCTURE,
                ruleVersion = rules.ruleVersion,
            )
        }

        val hasReelEvidence =
            reelLists.isNotEmpty() ||
                tree.nodes.any(rules.playerPage::matches) ||
                tree.nodes.any(rules.loadingSpinner::matches)
        return if (hasReelEvidence) {
            DetectionDecision(
                result = DetectionResult.POSSIBLE_SHORTS,
                reason = DetectionReason.PARTIAL_REEL_STRUCTURE,
                ruleVersion = rules.ruleVersion,
            )
        } else {
            DetectionDecision(
                result = DetectionResult.UNKNOWN_LAYOUT,
                reason = DetectionReason.NO_RECOGNIZED_STRUCTURE,
                ruleVersion = rules.ruleVersion,
            )
        }
    }

    private fun SanitizedNodeTree.isStructurallyValid(): Boolean {
        if (nodes.isEmpty() || nodes.size > MAX_NODES) return false
        val nodesByIndex = nodes.associateBy { it.index }
        if (nodesByIndex.size != nodes.size) return false
        if (nodes.count { it.parentIndex == null } != 1) return false
        val observedChildCounts = nodes.groupingBy { it.parentIndex }.eachCount()

        return nodes.all { node ->
            if (node.index < 0 || node.depth !in 0..MAX_DEPTH || node.childCount < 0) {
                false
            } else if (node.parentIndex == null) {
                node.depth == 0 && node.childCount >= observedChildCounts[node.index].orZero()
            } else {
                val parent = nodesByIndex[node.parentIndex]
                parent != null &&
                    parent.index != node.index &&
                    node.depth == parent.depth + 1 &&
                    node.childCount >= observedChildCounts[node.index].orZero()
            }
        }
    }

    private fun Int?.orZero(): Int = this ?: 0

    private fun unknown(reason: DetectionReason) =
        DetectionDecision(result = DetectionResult.UNKNOWN_LAYOUT, reason = reason)

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val MAX_NODES = 1_000
        const val MAX_DEPTH = 40
    }
}
