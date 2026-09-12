package com.shortstop.blocker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.shortstop.blocker.ui.theme.ShortStopTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun onboardingRequiresAcknowledgementAndAppIsForeground() {
        composeRule.setContent {
            ShortStopTheme {
                ShortStopApp(
                    state = ShortStopUiState(),
                    onAcknowledgementChanged = {},
                    onContinueToSettings = {},
                    onPausedChanged = {},
                    onOpenAccessibilitySettings = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Leave Shorts. Keep YouTube.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Continue to Accessibility settings")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule
            .onNodeWithText("does not capture screenshots", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val app = device.wait(Until.findObject(By.pkg("com.shortstop.blocker.debug")), 5_000)
        assertNotNull(app)
    }
}
