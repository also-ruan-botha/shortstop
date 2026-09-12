package com.shortstop.blocker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AutomationStatus {
    MONITORING,
    UNSUPPORTED_LAYOUT,
}

internal object AutomationRuntime {
    private val mutableStatus = MutableStateFlow(AutomationStatus.MONITORING)
    val status = mutableStatus.asStateFlow()

    fun update(status: AutomationStatus) {
        mutableStatus.value = status
    }
}
