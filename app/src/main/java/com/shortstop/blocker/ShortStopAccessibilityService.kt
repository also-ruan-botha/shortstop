package com.shortstop.blocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.shortstop.blocker.detection.DetectionResult
import com.shortstop.blocker.detection.EjectionDirective
import com.shortstop.blocker.detection.EjectionStateMachine
import com.shortstop.blocker.detection.ShortsDetector
import com.shortstop.blocker.discovery.CurrentTreeResult
import com.shortstop.blocker.discovery.CurrentYouTubeTreeSource
import com.shortstop.blocker.discovery.discoveryServiceBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShortStopAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val discoveryBridge = discoveryServiceBridge()
    private val detector = ShortsDetector()
    private val youtubeHomeActionExecutor = YoutubeHomeActionExecutor(detector)
    private val stateMachine = EjectionStateMachine()

    private lateinit var treeSource: CurrentYouTubeTreeSource
    private var classificationJob: Job? = null
    private var cooldownJob: Job? = null

    @Volatile private var paused = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        treeSource = CurrentYouTubeTreeSource(applicationContext)
        AutomationRuntime.update(AutomationStatus.MONITORING)
        discoveryBridge.onServiceConnected(this)
        serviceScope.launch {
            UserPreferencesRepository(applicationContext).preferences.collect { preferences ->
                if (preferences.paused && !paused) resetAutomation()
                paused = preferences.paused
            }
        }
    }

    // This function shows that we only care when the package (app) is YouTube.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != YOUTUBE_PACKAGE || paused) return

        discoveryBridge.onEligibleYouTubeEvent(this)
        handle(stateMachine.onYouTubeEvent(SystemClock.elapsedRealtime()))
    }

    override fun onInterrupt() {
        resetAutomation()
    }

    override fun onDestroy() {
        resetAutomation()
        discoveryBridge.onServiceDestroyed(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun classifyCurrentTree() {
        if (paused) return
        when (val currentTree = treeSource.read(this)) {
            is CurrentTreeResult.Available -> {
                val decision = detector.detect(currentTree.tree)
                AutomationRuntime.update(decision.toAutomationStatus())
                handle(
                    stateMachine.onClassification(
                        result = decision.result,
                        nowMillis = SystemClock.elapsedRealtime(),
                    )
                )
            }
            CurrentTreeResult.TargetNotActive -> resetAutomation()
            CurrentTreeResult.Unavailable -> {
                AutomationRuntime.update(AutomationStatus.UNSUPPORTED_LAYOUT)
                handle(
                    stateMachine.onClassification(
                        result = DetectionResult.UNKNOWN_LAYOUT,
                        nowMillis = SystemClock.elapsedRealtime(),
                    )
                )
            }
        }
    }

    private fun handle(directives: List<EjectionDirective>) {
        directives.forEach(::handle)
    }

    private fun handle(directive: EjectionDirective) {
        when (directive) {
            is EjectionDirective.ScheduleClassification -> {
                classificationJob?.cancel()
                classificationJob = serviceScope.launch {
                    delay(directive.delayMillis)
                    classifyCurrentTree()
                }
            }
            EjectionDirective.ClickYoutubeHome -> {
                val accepted = youtubeHomeActionExecutor.perform(this)
                handle(stateMachine.onYoutubeHomeClicked(accepted))
            }
            is EjectionDirective.ScheduleCooldownEnd -> {
                cooldownJob?.cancel()
                cooldownJob = serviceScope.launch {
                    delay(directive.delayMillis)
                    handle(stateMachine.onCooldownElapsed(SystemClock.elapsedRealtime()))
                }
            }
        }
    }

    private fun resetAutomation() {
        classificationJob?.cancel()
        classificationJob = null
        cooldownJob?.cancel()
        cooldownJob = null
        stateMachine.reset()
        AutomationRuntime.update(AutomationStatus.MONITORING)
    }

    private fun com.shortstop.blocker.detection.DetectionDecision.toAutomationStatus() =
        when (result) {
            DetectionResult.CONFIRMED_SHORTS,
            DetectionResult.NOT_SHORTS -> AutomationStatus.MONITORING
            DetectionResult.POSSIBLE_SHORTS,
            DetectionResult.UNKNOWN_LAYOUT -> AutomationStatus.UNSUPPORTED_LAYOUT
        }

    private companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
