package com.elementaldevelopment.diagnostics.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.elementaldevelopment.diagnostics.ui.screens.BugReportPreviewScreen
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BugReportPreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysPreviewTitle() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Bug Report Preview").assertIsDisplayed()
    }

    @Test
    fun displaysPrivacyText() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Diagnostics remain on your device", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysToggles() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Include app info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include device info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include OS info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include recent logs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include recovered diagnostics").assertIsDisplayed()
    }

    @Test
    fun displaysCopyAndShareButtons() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
    }

    @Test
    fun copyButtonTriggersCallback() {
        var copiedText: String? = null
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = { copiedText = it },
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Copy").performClick()
        assertThat(copiedText).isNotNull()
        assertThat(copiedText).contains("Elemental Diagnostics Report")
    }

    @Test
    fun shareButtonTriggersCallback() {
        var sharedText: String? = null
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = { sharedText = it },
            )
        }

        composeTestRule.onNodeWithText("Share").performClick()
        assertThat(sharedText).isNotNull()
    }

    @Test
    fun displaysReportPreview() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Elemental Diagnostics Report", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysNoteField() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Add a note (optional)").assertIsDisplayed()
    }

    @Test
    fun displaysRecoveredDiagnosticsMessage() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Recovered Diagnostics Available").assertIsDisplayed()
        composeTestRule.onNodeWithText("ended unexpectedly", substring = true).assertIsDisplayed()
    }

    @Test
    fun recoveredDiagnosticsToggleUpdatesPreview() {
        composeTestRule.setContent {
            BugReportPreviewScreen(
                diagnostics = FakeDiagnostics(),
                onCopy = {},
                onShare = {},
            )
        }

        composeTestRule.onNodeWithText("Recovered Diagnostics From Previous Launch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include recovered diagnostics").performClick()
        composeTestRule.onNodeWithText("Recovered Diagnostics From Previous Launch").assertDoesNotExist()
    }
}
