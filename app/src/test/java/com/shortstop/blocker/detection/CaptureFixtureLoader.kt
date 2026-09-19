package com.shortstop.blocker.detection

import com.shortstop.blocker.discovery.CaptureMetadata
import com.shortstop.blocker.discovery.NodeRole
import com.shortstop.blocker.discovery.SanitizedNode
import com.shortstop.blocker.discovery.SanitizedNodeTree

internal object CaptureFixtureLoader {
    fun load(name: String): SanitizedNodeTree {
        val json =
            checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
                    "Missing capture fixture: $name"
                }
                .bufferedReader()
                .use { it.readText() }

        return SanitizedNodeTree(
            metadata =
                CaptureMetadata(
                    capturedAtUtc = json.string("capturedAtUtc"),
                    targetPackage = json.string("targetPackage"),
                    targetVersionName = json.string("targetVersionName"),
                    targetVersionCode = json.long("targetVersionCode"),
                    androidRelease = json.string("androidRelease"),
                    androidSdk = json.int("androidSdk"),
                ),
            nodes = NODE_OBJECT.findAll(json).map { parseNode(it.value) }.toList(),
            truncated = json.boolean("truncated"),
            schemaVersion = json.int("schemaVersion"),
        )
    }

    private fun parseNode(json: String) =
        SanitizedNode(
            index = json.int("index"),
            parentIndex = json.nullableInt("parentIndex"),
            resourceIdSuffix = json.nullableString("resourceIdSuffix"),
            className = json.nullableString("className"),
            role = NodeRole.valueOf(json.string("role")),
            clickable = json.boolean("clickable"),
            scrollable = json.boolean("scrollable"),
            selected = json.boolean("selected"),
            visibleToUser = json.boolean("visibleToUser"),
            enabled = json.boolean("enabled"),
            checkable = json.boolean("checkable"),
            childCount = json.int("childCount"),
            depth = json.int("depth"),
        )

    private fun String.string(key: String): String =
        requireNotNull(
            Regex("\\\"$key\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(this)?.groupValues?.get(1)
        ) {
            "Missing string field: $key"
        }

    private fun String.nullableString(key: String): String? {
        val value =
            requireNotNull(Regex("\\\"$key\\\"\\s*:\\s*(null|\\\"([^\\\"]*)\\\")").find(this))
        return value.groupValues[2].ifEmpty { null }
    }

    private fun String.int(key: String): Int = long(key).toInt()

    private fun String.long(key: String): Long =
        requireNotNull(Regex("\\\"$key\\\"\\s*:\\s*(-?\\d+)").find(this)?.groupValues?.get(1)) {
                "Missing numeric field: $key"
            }
            .toLong()

    private fun String.nullableInt(key: String): Int? {
        val value =
            requireNotNull(
                Regex("\\\"$key\\\"\\s*:\\s*(null|-?\\d+)").find(this)?.groupValues?.get(1)
            ) {
                "Missing nullable numeric field: $key"
            }
        return value.takeUnless { it == "null" }?.toInt()
    }

    private fun String.boolean(key: String): Boolean =
        requireNotNull(Regex("\\\"$key\\\"\\s*:\\s*(true|false)").find(this)?.groupValues?.get(1)) {
                "Missing boolean field: $key"
            }
            .toBooleanStrict()

    private val NODE_OBJECT = Regex("\\{[^{}]*\\\"index\\\"[^{}]*}")
}
