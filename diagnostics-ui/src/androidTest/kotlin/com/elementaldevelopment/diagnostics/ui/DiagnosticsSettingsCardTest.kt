package com.elementaldevelopment.diagnostics.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.elementaldevelopment.diagnostics.ui.components.DiagnosticsSettingsCard
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class DiagnosticsSettingsCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            DiagnosticsSettingsCard(onSendBugReport = {})
        }

        composeTestRule.onNodeWithText("Send Bug Report").assertIsDisplayed()
    }

    @Test
    fun displaysExplanationText() {
        composeTestRule.setContent {
            DiagnosticsSettingsCard(onSendBugReport = {})
        }

        composeTestRule.onNodeWithText("Generate a diagnostic report", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysButton() {
        composeTestRule.setContent {
            DiagnosticsSettingsCard(onSendBugReport = {})
        }

        composeTestRule.onNodeWithText("Review & Send Report").assertIsDisplayed()
    }

    @Test
    fun buttonTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            DiagnosticsSettingsCard(onSendBugReport = { clicked = true })
        }

        composeTestRule.onNodeWithText("Review & Send Report").performClick()
        assertThat(clicked).isTrue()
    }
}
