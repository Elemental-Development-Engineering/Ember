package com.elementaldevelopment.diagnostics.ui.actions

import android.content.ClipboardManager
import android.content.Context
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiagnosticsExportHandlerTest {

    private lateinit var fakeDiagnostics: TestDiagnostics
    private lateinit var handler: DiagnosticsExportHandler

    @Before
    fun setUp() {
        fakeDiagnostics = TestDiagnostics()
        handler = DiagnosticsExportHandler(
            context = RuntimeEnvironment.getApplication(),
            diagnostics = fakeDiagnostics,
        )
    }

    @Test
    fun `copyToClipboard returns true on success`() {
        val result = handler.copyToClipboard("Bug report text")
        assertThat(result).isTrue()
    }

    @Test
    fun `copyToClipboard places text on clipboard`() {
        handler.copyToClipboard("Bug report text")

        val clipboard = RuntimeEnvironment.getApplication()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertThat(clipText).isEqualTo("Bug report text")
    }

    @Test
    fun `copyToClipboard clears diagnostics log`() {
        handler.copyToClipboard("Bug report text")
        assertThat(fakeDiagnostics.logger.cleared).isTrue()
    }

    @Test
    fun `shareReport does not immediately clear log`() {
        handler.shareReport("Bug report text")
        assertThat(fakeDiagnostics.logger.cleared).isFalse()
    }

    // --- Test doubles ---

    private class TestLogger : DiagnosticsLogger {
        var cleared = false
            private set

        override fun log(tag: String, message: String) {}
        override fun log(level: DiagnosticLevel, tag: String, message: String, attributes: Map<String, String>) {}
        override fun logError(tag: String, message: String, throwable: Throwable?) {}
        override fun recentEntries(limit: Int?): List<DiagnosticEntry> = emptyList()
        override fun clear() { cleared = true }
    }

    private class TestConfig : DiagnosticsConfig {
        override val appName = "TestApp"
        override val appId = "com.test.app"
        override val supportEmail = "test@example.com"
        override val maxStoredEntries = 100
        override val includeDeviceModelByDefault = true
        override val includeOsVersionByDefault = true
        override val redactor = DiagnosticsRedactor { it }
        override fun additionalMetadata() = emptyMap<String, String>()
    }

    private class TestReportBuilder : BugReportBuilder {
        override fun build(request: BugReportRequest): BugReport {
            return BugReport(
                metadata = DiagnosticsMetadata(
                    appName = "TestApp", appId = "com.test.app",
                    versionName = "1.0", versionCode = 1,
                    androidVersion = "", apiLevel = 0,
                    deviceManufacturer = "", deviceModel = "",
                    generatedAt = System.currentTimeMillis(),
                    libraryVersion = "0.2.0", sessionId = "test",
                ),
                entries = emptyList(),
                userNote = null,
                generatedAt = System.currentTimeMillis(),
            )
        }
    }

    private class TestExporter : DiagnosticsExporter {
        override fun export(report: BugReport): String = "exported"
    }

    private class TestDiagnostics : Diagnostics {
        override val logger = TestLogger()
        override val reportBuilder = TestReportBuilder()
        override val exporter = TestExporter()
        override val config = TestConfig()
    }
}
