package com.shortstop.blocker.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EjectionStateMachineTest {
    @Test
    fun rapidEventsCoalesceIntoOneClassification() {
        val machine = machine()

        assertEquals(
            listOf(EjectionDirective.ScheduleClassification(CLASSIFICATION_DELAY)),
            machine.onYouTubeEvent(nowMillis = 0),
        )
        assertTrue(machine.onYouTubeEvent(nowMillis = 1).isEmpty())
        assertTrue(machine.onYouTubeEvent(nowMillis = 2).isEmpty())
        assertEquals(EjectionStage.SUSPECTED, machine.state.stage)
    }

    @Test
    fun confirmationClicksYoutubeHomeThenSchedulesOneVerification() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)

        assertEquals(
            listOf(EjectionDirective.ClickYoutubeHome),
            machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1),
        )
        assertEquals(EjectionStage.EJECTING, machine.state.stage)
        assertEquals(
            listOf(EjectionDirective.ScheduleClassification(VERIFICATION_DELAY)),
            machine.onYoutubeHomeClicked(accepted = true),
        )
        assertEquals(EjectionStage.VERIFYING, machine.state.stage)
    }

    @Test
    fun rejectedYoutubeHomeClickWaitsForAnotherEventBeforeRetrying() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1)

        assertTrue(machine.onYoutubeHomeClicked(accepted = false).isEmpty())
        assertEquals(EjectionStage.VERIFYING, machine.state.stage)
        assertEquals(false, machine.state.classificationPending)

        assertEquals(
            listOf(EjectionDirective.ScheduleClassification(CLASSIFICATION_DELAY)),
            machine.onYouTubeEvent(nowMillis = 2),
        )
        assertEquals(
            listOf(EjectionDirective.ClickYoutubeHome),
            machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 3),
        )
    }

    @Test
    fun eventDuringVerificationAllowsOneBoundedImmediateRetry() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1)
        machine.onYoutubeHomeClicked(accepted = true)

        assertTrue(machine.onYouTubeEvent(nowMillis = 2).isEmpty())
        assertEquals(
            listOf(EjectionDirective.ClickYoutubeHome),
            machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 3),
        )

        machine.onYoutubeHomeClicked(accepted = true)
        assertTrue(machine.onYouTubeEvent(nowMillis = 4).isEmpty())
        assertTrue(
            machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 5).isEmpty()
        )
        assertEquals(EjectionStage.VERIFYING, machine.state.stage)
        assertEquals(false, machine.state.classificationPending)
        assertEquals(true, machine.state.automaticRetryUsed)
    }

    @Test
    fun persistentShortsCanBePreventedIndefinitelyWithoutAPollingLoop() {
        val machine = machine()

        repeat(20) { encounter ->
            assertEquals(
                listOf(EjectionDirective.ScheduleClassification(CLASSIFICATION_DELAY)),
                machine.onYouTubeEvent(nowMillis = encounter * 10L),
            )
            assertEquals(
                listOf(EjectionDirective.ClickYoutubeHome),
                machine.onClassification(
                    DetectionResult.CONFIRMED_SHORTS,
                    nowMillis = encounter * 10L + 1,
                ),
            )
            machine.onYoutubeHomeClicked(accepted = true)

            assertTrue(
                machine
                    .onClassification(
                        DetectionResult.CONFIRMED_SHORTS,
                        nowMillis = encounter * 10L + 2,
                    )
                    .isEmpty()
            )
            assertEquals(EjectionStage.VERIFYING, machine.state.stage)
            assertEquals(false, machine.state.classificationPending)
        }
    }

    @Test
    fun successfulExitToRecognizedPlaybackEntersCooldown() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1)
        machine.onYoutubeHomeClicked(accepted = true)

        assertEquals(
            listOf(EjectionDirective.ScheduleCooldownEnd(COOLDOWN)),
            machine.onClassification(DetectionResult.NOT_SHORTS, nowMillis = 10),
        )
        assertEquals(EjectionStage.COOLDOWN, machine.state.stage)
        assertTrue(machine.onYouTubeEvent(nowMillis = 11).isEmpty())
        assertTrue(machine.onCooldownElapsed(nowMillis = 10 + COOLDOWN).isEmpty())
        assertEquals(EjectionState(), machine.state)
    }

    @Test
    fun unknownVerificationWaitsForAnEventWithoutSchedulingAnotherRead() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1)
        machine.onYoutubeHomeClicked(accepted = true)

        assertTrue(
            machine.onClassification(DetectionResult.UNKNOWN_LAYOUT, nowMillis = 2).isEmpty()
        )
        assertEquals(EjectionStage.VERIFYING, machine.state.stage)
        assertEquals(false, machine.state.classificationPending)
    }

    @Test
    fun initialNonConfirmingResultsReturnToMonitoring() {
        listOf(
                DetectionResult.NOT_SHORTS,
                DetectionResult.POSSIBLE_SHORTS,
                DetectionResult.UNKNOWN_LAYOUT,
            )
            .forEach { result ->
                val machine = machine()
                machine.onYouTubeEvent(nowMillis = 0)

                assertTrue(machine.onClassification(result, nowMillis = 1).isEmpty())
                assertEquals(EjectionState(), machine.state)
            }
    }

    @Test
    fun staleCallbacksCannotCauseActionsOrEndCooldownEarly() {
        val machine = machine()
        assertTrue(
            machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 0).isEmpty()
        )
        assertTrue(machine.onYoutubeHomeClicked(accepted = true).isEmpty())

        machine.onYouTubeEvent(nowMillis = 1)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 2)
        machine.onYoutubeHomeClicked(accepted = true)
        machine.onClassification(DetectionResult.NOT_SHORTS, nowMillis = 10)

        assertEquals(
            listOf(EjectionDirective.ScheduleCooldownEnd(COOLDOWN - 1)),
            machine.onCooldownElapsed(nowMillis = 11),
        )
        assertEquals(EjectionStage.COOLDOWN, machine.state.stage)
    }

    @Test
    fun resetAlwaysReturnsToMonitoring() {
        val machine = machine()
        machine.onYouTubeEvent(nowMillis = 0)
        machine.onClassification(DetectionResult.CONFIRMED_SHORTS, nowMillis = 1)

        machine.reset()

        assertEquals(EjectionState(), machine.state)
    }

    private fun machine() =
        EjectionStateMachine(
            classificationDelayMillis = CLASSIFICATION_DELAY,
            verificationDelayMillis = VERIFICATION_DELAY,
            cooldownMillis = COOLDOWN,
        )

    private companion object {
        const val CLASSIFICATION_DELAY = 100L
        const val VERIFICATION_DELAY = 250L
        const val COOLDOWN = 1_000L
    }
}
