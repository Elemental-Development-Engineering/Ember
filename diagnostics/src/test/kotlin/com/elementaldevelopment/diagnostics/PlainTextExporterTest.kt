package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.PlainTextExporter
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlainTextExporterTest {

    private val exporter = PlainTextExporter()

    private fun metadata(
        deviceManufacturer: String = "Google",
        deviceModel: String = "Pixel 8",
    ) = DiagnosticsMetadata(
        appName = "TestApp",
        appId = "com.test.app",
        versionName = "1.0.0",
        versionCode = 1,
        androidVersion = "15",
        apiLevel = 35,
        deviceManufacturer = deviceManufacturer,
        deviceModel = deviceModel,
        generatedAt = 1710864600000L,
        libraryVersion = "0.2.0",
        sessionId = "test-session-123",
    )

    @Test
    fun `exports report with all sections`() {
        val report = BugReport(
            metadata = metadata(),
            entries = listOf(
                DiagnosticEntry(
                    id = 1,
                    timestamp = 1710864500000L,
                    level = DiagnosticLevel.INFO,
                    tag = "Parser",
                    message = "parse started",
                ),
            ),
            userNote = "App freezes on large files",
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)

        assertThat(text).contains("Elemental Diagnostics Report")
        assertThat(text).contains("Format Version: 2")
        assertThat(text).contains("Library Version: 0.2.0")
        assertThat(text).contains("Name: TestApp")
        assertThat(text).contains("App ID: com.test.app")
        assertThat(text).contains("Version: 1.0.0 (1)")
        assertThat(text).contains("Android: 15 / API 35")
        assertThat(text).contains("Google Pixel 8")
        assertThat(text).contains("Session: test-session-123")
        assertThat(text).contains("User Note")
        assertThat(text).contains("App freezes on large files")
        assertThat(text).contains("Recent Diagnostics")
        assertThat(text).contains("INFO Parser: parse started")
    }

    @Test
    fun `exports recovered diagnostics section when present`() {
        val report = BugReport(
            metadata = metadata().copy(
                previousSessionOutcome = PreviousSessionOutcome.UNEXPECTED_TERMINATION,
                previousSessionId = "previous-session",
                previousSessionTimestamp = 1710864550000L,
            ),
            entries = emptyList(),
            recoveredEntries = listOf(
                DiagnosticEntry(
                    id = 2,
                    timestamp = 1710864500000L,
                    level = DiagnosticLevel.ERROR,
                    tag = "Crash",
                    message = "Recovered from previous launch",
                ),
            ),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)

        assertThat(text).contains("Previous Session: unexpected termination")
        assertThat(text).contains("Previous Session ID: previous-session")
        assertThat(text).contains("Recovered Diagnostics From Previous Launch")
        assertThat(text).contains("ERROR Crash: Recovered from previous launch")
    }

    @Test
    fun `omits user note section when empty`() {
        val report = BugReport(
            metadata = metadata(),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).doesNotContain("User Note")
    }

    @Test
    fun `omits entries section when empty`() {
        val report = BugReport(
            metadata = metadata(),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).doesNotContain("Recent Diagnostics")
    }

    @Test
    fun `includes throwable summary in entry`() {
        val report = BugReport(
            metadata = metadata(),
            entries = listOf(
                DiagnosticEntry(
                    id = 1,
                    timestamp = 1710864500000L,
                    level = DiagnosticLevel.ERROR,
                    tag = "Parser",
                    message = "parse failed",
                    throwableSummary = "OutOfMemoryError: Java heap space",
                ),
            ),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).contains("Exception: OutOfMemoryError: Java heap space")
    }

    @Test
    fun `includes attributes in entry`() {
        val report = BugReport(
            metadata = metadata(),
            entries = listOf(
                DiagnosticEntry(
                    id = 1,
                    timestamp = 1710864500000L,
                    level = DiagnosticLevel.INFO,
                    tag = "Import",
                    message = "import started",
                    attributes = mapOf("format" to "markdown"),
                ),
            ),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).contains("format=markdown")
    }

    @Test
    fun `omits device info when manufacturer and model are empty`() {
        val report = BugReport(
            metadata = metadata(deviceManufacturer = "", deviceModel = ""),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).doesNotContain("Device:")
    }

    @Test
    fun `omits app section when app info is empty`() {
        val report = BugReport(
            metadata = metadata().copy(
                appName = "",
                appId = "",
                versionName = "",
                versionCode = 0,
            ),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).doesNotContain("\nApp\n")
        assertThat(text).doesNotContain("App ID:")
        assertThat(text).doesNotContain("- Version:")
    }

    @Test
    fun `omits android line when os info is empty`() {
        val report = BugReport(
            metadata = metadata().copy(
                androidVersion = "",
                apiLevel = 0,
            ),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).doesNotContain("Android:")
        assertThat(text).contains("Session: test-session-123")
    }

    @Test
    fun `keeps environment section for session context when device and os info are empty`() {
        val report = BugReport(
            metadata = metadata(
                deviceManufacturer = "",
                deviceModel = "",
            ).copy(
                androidVersion = "",
                apiLevel = 0,
            ),
            entries = emptyList(),
            userNote = null,
            generatedAt = 1710864600000L,
        )

        val text = exporter.export(report)
        assertThat(text).contains("\nEnvironment\n")
        assertThat(text).contains("Time:")
        assertThat(text).contains("Session: test-session-123")
    }
}
