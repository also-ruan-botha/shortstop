package com.shortstop.blocker

internal enum class ServiceStatus(val heading: String, val description: String) {
    DISABLED(
        heading = "Accessibility access is off",
        description = "Enable ShortStop in Android Settings before it can react inside YouTube.",
    ),
    ENABLED(
        heading = "ShortStop is enabled",
        description = "ShortStop is monitoring supported YouTube layouts on this device.",
    ),
    PAUSED(
        heading = "ShortStop is paused",
        description =
            "Accessibility access remains enabled, but ShortStop will not react to YouTube.",
    ),
    UNSUPPORTED_LAYOUT(
        heading = "This YouTube layout is not supported",
        description =
            "ShortStop will take no action because it cannot classify this layout safely.",
    ),
}

internal fun serviceStatus(
    serviceEnabled: Boolean,
    paused: Boolean,
    automationStatus: AutomationStatus,
): ServiceStatus =
    when {
        !serviceEnabled -> ServiceStatus.DISABLED
        paused -> ServiceStatus.PAUSED
        automationStatus == AutomationStatus.UNSUPPORTED_LAYOUT -> ServiceStatus.UNSUPPORTED_LAYOUT
        else -> ServiceStatus.ENABLED
    }
