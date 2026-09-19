package com.shortstop.blocker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.shortstop.blocker.ui.theme.ShortStopTheme
import org.junit.Rule
import org.junit.Test

class CompatibilityNoticeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun noticeExplainsHowToRecoverFromYouTubeProblems() {
        composeRule.setContent { ShortStopTheme { CompatibilityNotice() } }

        composeRule.onNodeWithText("YouTube compatibility can change").assertIsDisplayed()
        composeRule.onNodeWithText("report the bug", substring = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("disable ShortStop in Accessibility settings", substring = true)
            .assertIsDisplayed()
    }
}
