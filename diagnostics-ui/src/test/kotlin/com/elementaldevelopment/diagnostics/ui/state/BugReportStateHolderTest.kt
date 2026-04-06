package com.elementaldevelopment.diagnostics.ui.state

import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.config.CrashPersistenceConfig
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.export.DiagnosticsExporter
import com.elementaldevelopment.diagnostics.logging.DiagnosticsLogger
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.elementaldevelopment.diagnostics.report.BugReportBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BugReportStateHolderTest {

    @Test
    fun `preview and export both use redacted note`() {
        val stateHolder = BugReportStateHolder(RedactingNoteDiagnostics())

        stateHolder.updateUserNote("User typed SECRET value")

        assertThat(stateHolder.state.previewText).contains("User typed [REDACTED] value")
        assertThat(stateHolder.state.previewText).doesNotContain("User typed SECRET value")
        assertThat(stateHolder.getExportText()).contains("User typed [REDACTED] value")
        assertThat(stateHolder.getExportText()).doesNotContain("User typed SECRET value")
    }

    @Test
    fun `recovered diagnostics default on can be toggled off for preview and export`() {
        val stateHolder = BugReportStateHolder(RedactingNoteDiagnostics())

        assertThat(stateHolder.state.hasRecoveredDiagnostics).isTrue()
        assertThat(stateHolder.state.includeRecoveredLogs).isTrue()
        assertThat(stateHolder.state.previewText).contains("Recovered Diagnostics From Previous Launch")

        stateHolder.toggleRecoveredLogs(false)

        assertThat(stateHolder.state.includeRecoveredLogs).isFalse()
        assertThat(stateHolder.state.previewText)
            .doesNotContain("Recovered Diagnostics From Previous Launch")
        assertThat(stateHolder.getExportText())
            .doesNotContain("Recovered Diagnostics From Previous Launch")
    }
}

private class RedactingNoteDiagnostics : Diagnostics {
    override val logger = object : DiagnosticsLogger {
        override fun log(tag: String, message: String) = Unit

        override fun log(
            level: DiagnosticLevel,
            tag: String,
            message: String,
            attributes: Map<String, String>,
        ) = Unit

        override fun logError(tag: String, message: String, throwable: Throwable?) = Unit

        override fun recentEntries(limit: Int?): List<DiagnosticEntry> = emptyList()

        override fun clear() = Unit
    }

    override val reportBuilder = object : BugReportBuilder {
        override fun build(request: BugReportRequest): BugReport {
            return BugReport(
                metadata = DiagnosticsMetadata(
                    appName = "TestApp",
                    appId = "com.test.app",
                    versionName = "1.0.0",
                    versionCode = 1,
                    androidVersion = "15",
                    apiLevel = 35,
                    deviceManufacturer = "Google",
                    deviceModel = "Pixel 8",
                    generatedAt = 0L,
                    libraryVersion = "0.2.0",
                    sessionId = "test-session",
                    previousSessionOutcome = PreviousSessionOutcome.UNEXPECTED_TERMINATION,
                ),
                entries = emptyList(),
                recoveredEntries = if (request.includeRecoveredLogs) {
                    listOf(
                        DiagnosticEntry(
                            id = 2,
                            timestamp = 0L,
                            level = DiagnosticLevel.ERROR,
                            tag = "Crash",
                            message = "Recovered from previous launch",
                        )
                    )
                } else {
                    emptyList()
                },
                userNote = request.userNote?.replace("SECRET", "[REDACTED]"),
                generatedAt = 0L,
            )
        }
    }

    override val exporter = object : DiagnosticsExporter {
        override fun export(report: BugReport): String {
            return buildString {
                appendLine("Elemental Diagnostics Report")
                report.userNote?.let {
                    appendLine()
                    appendLine("User Note")
                    appendLine(it)
                }
                if (report.recoveredEntries.isNotEmpty()) {
                    appendLine()
                    appendLine("Recovered Diagnostics From Previous Launch")
                    report.recoveredEntries.forEach { entry ->
                        appendLine("[test] ${entry.level} ${entry.tag}: ${entry.message}")
                    }
                }
            }.trimEnd()
        }
    }

    override val config = object : DiagnosticsConfig {
        override val appName = "TestApp"
        override val appId = "com.test.app"
        override val supportEmail = "support@example.com"
        override val maxStoredEntries = 100
        override val crashPersistence = CrashPersistenceConfig.Enabled(
            maxPersistedEntries = 100,
            includeRecoveredEntriesByDefault = true,
        )
        override val includeDeviceModelByDefault = true
        override val includeOsVersionByDefault = true
        override val redactor = DiagnosticsRedactor { it }

        override fun additionalMetadata(): Map<String, String> = emptyMap()
    }
}
