package com.shortstop.blocker.discovery

internal object FixtureJsonEncoder {
    fun encode(tree: SanitizedNodeTree): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": $SANITIZED_TREE_SCHEMA_VERSION,")
        appendLine("  \"capturedAtUtc\": ${tree.metadata.capturedAtUtc.jsonString()},")
        appendLine("  \"targetPackage\": ${tree.metadata.targetPackage.jsonString()},")
        appendLine("  \"targetVersionName\": ${tree.metadata.targetVersionName.jsonString()},")
        appendLine("  \"targetVersionCode\": ${tree.metadata.targetVersionCode},")
        appendLine("  \"androidRelease\": ${tree.metadata.androidRelease.jsonString()},")
        appendLine("  \"androidSdk\": ${tree.metadata.androidSdk},")
        appendLine("  \"truncated\": ${tree.truncated},")
        appendLine("  \"nodes\": [")
        tree.nodes.forEachIndexed { position, node ->
            appendLine("    {")
            appendLine("      \"index\": ${node.index},")
            appendLine("      \"parentIndex\": ${node.parentIndex ?: "null"},")
            appendLine("      \"resourceIdSuffix\": ${node.resourceIdSuffix.jsonString()},")
            appendLine("      \"className\": ${node.className.jsonString()},")
            appendLine("      \"role\": ${node.role.name.jsonString()},")
            appendLine("      \"clickable\": ${node.clickable},")
            appendLine("      \"scrollable\": ${node.scrollable},")
            appendLine("      \"selected\": ${node.selected},")
            appendLine("      \"visibleToUser\": ${node.visibleToUser},")
            appendLine("      \"enabled\": ${node.enabled},")
            appendLine("      \"checkable\": ${node.checkable},")
            appendLine("      \"childCount\": ${node.childCount},")
            appendLine("      \"depth\": ${node.depth}")
            append("    }")
            if (position != tree.nodes.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    }

    private fun String?.jsonString(): String {
        if (this == null) return "null"
        return buildString {
            append('"')
            this@jsonString.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (character.code < 0x20) {
                            append("\\u")
                            append(character.code.toString(16).padStart(4, '0'))
                        } else {
                            append(character)
                        }
                    }
                }
            }
            append('"')
        }
    }
}
