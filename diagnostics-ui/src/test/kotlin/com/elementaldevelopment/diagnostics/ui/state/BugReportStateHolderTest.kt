package com.elementaldevelopment.diagnostics.ui.state

import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.export.DiagnosticsExporter
import com.elementaldevelopment.diagnostics.logging.DiagnosticsLogger
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
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
                    libraryVersion = "0.1.0",
                    sessionId = "test-session",
                ),
                entries = emptyList(),
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
            }.trimEnd()
        }
    }

    override val config = object : DiagnosticsConfig {
        override val appName = "TestApp"
        override val appId = "com.test.app"
        override val supportEmail = "support@example.com"
        override val maxStoredEntries = 100
        override val includeDeviceModelByDefault = true
        override val includeOsVersionByDefault = true
        override val redactor = DiagnosticsRedactor { it }

        override fun additionalMetadata(): Map<String, String> = emptyMap()
    }
}
