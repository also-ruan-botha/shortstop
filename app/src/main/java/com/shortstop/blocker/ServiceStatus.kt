package com.shortstop.blocker

internal enum class ServiceStatus(val heading: String, val description: String) {
    DISABLED(
        heading = "Accessibility access is off",
        description = "Enable ShortStop in Android Settings before it can react inside YouTube.",
    ),
    ENABLED(
        heading = "ShortStop is enabled",
        description =
            "The service is connected. Automatic detection and navigation are not active yet.",
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
    unsupportedLayout: Boolean,
): ServiceStatus =
    when {
        !serviceEnabled -> ServiceStatus.DISABLED
        paused -> ServiceStatus.PAUSED
        unsupportedLayout -> ServiceStatus.UNSUPPORTED_LAYOUT
        else -> ServiceStatus.ENABLED
    }
