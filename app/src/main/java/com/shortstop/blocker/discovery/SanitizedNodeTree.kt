package com.shortstop.blocker.discovery

import java.util.Locale

internal const val SANITIZED_TREE_SCHEMA_VERSION = 1

internal data class CaptureMetadata(
    val capturedAtUtc: String,
    val targetPackage: String,
    val targetVersionName: String,
    val targetVersionCode: Long,
    val androidRelease: String,
    val androidSdk: Int,
)

internal data class SanitizedNode(
    val index: Int,
    val parentIndex: Int?,
    val resourceIdSuffix: String?,
    val className: String?,
    val role: NodeRole,
    val clickable: Boolean,
    val scrollable: Boolean,
    val selected: Boolean,
    val visibleToUser: Boolean,
    val enabled: Boolean,
    val checkable: Boolean,
    val childCount: Int,
    val depth: Int,
)

internal data class SanitizedNodeTree(
    val metadata: CaptureMetadata,
    val nodes: List<SanitizedNode>,
    val truncated: Boolean,
    val schemaVersion: Int = SANITIZED_TREE_SCHEMA_VERSION,
)

internal enum class NodeRole {
    BUTTON,
    CHECK_BOX,
    EDIT_TEXT,
    IMAGE,
    LIST,
    LIST_ITEM,
    SCROLL_CONTAINER,
    SWITCH,
    TEXT,
    VIEW,
    UNKNOWN,
}

internal fun resourceIdSuffix(resourceId: String?): String? =
    sanitizeStructuralIdentifier(resourceId?.substringAfterLast('/'))

internal fun sanitizeClassName(className: String?): String? =
    sanitizeStructuralIdentifier(className)

internal fun roleForClassName(className: String?): NodeRole {
    val normalized = className?.lowercase(Locale.ROOT) ?: return NodeRole.UNKNOWN
    return when {
        normalized.endsWith("button") -> NodeRole.BUTTON
        normalized.endsWith("checkbox") -> NodeRole.CHECK_BOX
        normalized.endsWith("edittext") -> NodeRole.EDIT_TEXT
        normalized.endsWith("imageview") -> NodeRole.IMAGE
        normalized.endsWith("recyclerview") || normalized.endsWith("listview") -> NodeRole.LIST
        normalized.endsWith("switch") -> NodeRole.SWITCH
        normalized.endsWith("scrollview") -> NodeRole.SCROLL_CONTAINER
        normalized.endsWith("textview") -> NodeRole.TEXT
        normalized.endsWith("viewgroup") -> NodeRole.LIST_ITEM
        normalized.endsWith("view") -> NodeRole.VIEW
        else -> NodeRole.UNKNOWN
    }
}

private fun sanitizeStructuralIdentifier(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return buildString {
            trimmed.take(MAX_IDENTIFIER_LENGTH).forEach { character ->
                append(
                    if (
                        character in 'a'..'z' ||
                            character in 'A'..'Z' ||
                            character in '0'..'9' ||
                            character == '_' ||
                            character == '.' ||
                            character == ':' ||
                            character == '-'
                    ) {
                        character
                    } else {
                        '_'
                    }
                )
            }
        }
        .takeIf { it.isNotEmpty() }
}

private const val MAX_IDENTIFIER_LENGTH = 120
