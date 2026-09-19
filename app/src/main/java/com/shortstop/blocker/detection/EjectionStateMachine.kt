package com.shortstop.blocker.detection

internal enum class EjectionStage {
    MONITORING,
    SUSPECTED,
    EJECTING,
    VERIFYING,
    COOLDOWN,
}

internal data class EjectionState(
    val stage: EjectionStage = EjectionStage.MONITORING,
    val classificationPending: Boolean = false,
    val cooldownUntilMillis: Long? = null,
    val eventObservedDuringVerification: Boolean = false,
    val automaticRetryUsed: Boolean = false,
)

internal sealed interface EjectionDirective {
    data class ScheduleClassification(val delayMillis: Long) : EjectionDirective

    data object ClickYoutubeHome : EjectionDirective

    data class ScheduleCooldownEnd(val delayMillis: Long) : EjectionDirective
}

internal class EjectionStateMachine(
    private val classificationDelayMillis: Long = DEFAULT_CLASSIFICATION_DELAY_MILLIS,
    private val verificationDelayMillis: Long = DEFAULT_VERIFICATION_DELAY_MILLIS,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
) {
    var state: EjectionState = EjectionState()
        private set

    fun onYouTubeEvent(nowMillis: Long): List<EjectionDirective> =
        when (state.stage) {
            EjectionStage.MONITORING -> scheduleSuspectedClassification()
            EjectionStage.SUSPECTED,
            EjectionStage.EJECTING -> emptyList()
            EjectionStage.VERIFYING ->
                if (state.classificationPending) {
                    state = state.copy(eventObservedDuringVerification = true)
                    emptyList()
                } else {
                    scheduleSuspectedClassification()
                }
            EjectionStage.COOLDOWN -> {
                val cooldownUntil = state.cooldownUntilMillis ?: nowMillis
                if (nowMillis >= cooldownUntil) {
                    state = EjectionState()
                    scheduleSuspectedClassification()
                } else {
                    emptyList()
                }
            }
        }

    fun onClassification(result: DetectionResult, nowMillis: Long): List<EjectionDirective> {
        if (
            !state.classificationPending ||
                state.stage !in setOf(EjectionStage.SUSPECTED, EjectionStage.VERIFYING)
        ) {
            return emptyList()
        }

        return when (state.stage) {
            EjectionStage.SUSPECTED -> onSuspectedClassification(result)
            EjectionStage.VERIFYING -> onVerificationClassification(result, nowMillis)
            else -> emptyList()
        }
    }

    fun onYoutubeHomeClicked(accepted: Boolean): List<EjectionDirective> {
        if (state.stage != EjectionStage.EJECTING) return emptyList()
        return if (accepted) {
            state =
                EjectionState(
                    stage = EjectionStage.VERIFYING,
                    classificationPending = true,
                    automaticRetryUsed = state.automaticRetryUsed,
                )
            listOf(EjectionDirective.ScheduleClassification(verificationDelayMillis))
        } else {
            state =
                EjectionState(
                    stage = EjectionStage.VERIFYING,
                    automaticRetryUsed = state.automaticRetryUsed,
                )
            emptyList()
        }
    }

    fun onCooldownElapsed(nowMillis: Long): List<EjectionDirective> {
        if (state.stage != EjectionStage.COOLDOWN) return emptyList()
        val cooldownUntil = state.cooldownUntilMillis ?: return emptyList()
        if (nowMillis < cooldownUntil) {
            return listOf(EjectionDirective.ScheduleCooldownEnd(cooldownUntil - nowMillis))
        }
        state = EjectionState()
        return emptyList()
    }

    fun reset() {
        state = EjectionState()
    }

    private fun onSuspectedClassification(result: DetectionResult): List<EjectionDirective> =
        when (result) {
            DetectionResult.CONFIRMED_SHORTS -> {
                state = EjectionState(stage = EjectionStage.EJECTING)
                listOf(EjectionDirective.ClickYoutubeHome)
            }
            DetectionResult.NOT_SHORTS,
            DetectionResult.POSSIBLE_SHORTS,
            DetectionResult.UNKNOWN_LAYOUT -> {
                state = EjectionState()
                emptyList()
            }
        }

    private fun onVerificationClassification(
        result: DetectionResult,
        nowMillis: Long,
    ): List<EjectionDirective> =
        when (result) {
            DetectionResult.NOT_SHORTS -> {
                state =
                    EjectionState(
                        stage = EjectionStage.COOLDOWN,
                        cooldownUntilMillis = nowMillis + cooldownMillis,
                    )
                listOf(EjectionDirective.ScheduleCooldownEnd(cooldownMillis))
            }
            DetectionResult.CONFIRMED_SHORTS -> {
                if (state.eventObservedDuringVerification && !state.automaticRetryUsed) {
                    state =
                        EjectionState(
                            stage = EjectionStage.EJECTING,
                            automaticRetryUsed = true,
                        )
                    listOf(EjectionDirective.ClickYoutubeHome)
                } else {
                    state =
                        EjectionState(
                            stage = EjectionStage.VERIFYING,
                            automaticRetryUsed = state.automaticRetryUsed,
                        )
                    emptyList()
                }
            }
            DetectionResult.POSSIBLE_SHORTS,
            DetectionResult.UNKNOWN_LAYOUT -> {
                state =
                    EjectionState(
                        stage = EjectionStage.VERIFYING,
                        automaticRetryUsed = state.automaticRetryUsed,
                    )
                emptyList()
            }
        }

    private fun scheduleSuspectedClassification(): List<EjectionDirective> {
        state =
            EjectionState(
                stage = EjectionStage.SUSPECTED,
                classificationPending = true,
            )
        return listOf(EjectionDirective.ScheduleClassification(classificationDelayMillis))
    }

    internal companion object {
        const val DEFAULT_CLASSIFICATION_DELAY_MILLIS = 150L
        const val DEFAULT_VERIFICATION_DELAY_MILLIS = 400L
        const val DEFAULT_COOLDOWN_MILLIS = 1_500L
    }
}
