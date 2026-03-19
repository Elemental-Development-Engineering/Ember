package com.elementaldevelopment.diagnostics.ui

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

internal class FakeDiagnostics : Diagnostics {
    override val logger = FakeLogger()
    override val reportBuilder = FakeReportBuilder()
    override val exporter = FakeExporter()
    override val config = FakeConfig()
}

internal class FakeLogger : DiagnosticsLogger {
    var cleared = false
        private set

    override fun log(tag: String, message: String) {}
    override fun log(level: DiagnosticLevel, tag: String, message: String, attributes: Map<String, String>) {}
    override fun logError(tag: String, message: String, throwable: Throwable?) {}
    override fun recentEntries(limit: Int?): List<DiagnosticEntry> = emptyList()
    override fun clear() { cleared = true }
}

internal class FakeReportBuilder : BugReportBuilder {
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
                generatedAt = System.currentTimeMillis(),
                libraryVersion = "0.1.0",
                sessionId = "test-session",
            ),
            entries = listOf(
                DiagnosticEntry(
                    id = 1,
                    timestamp = System.currentTimeMillis(),
                    level = DiagnosticLevel.INFO,
                    tag = "System",
                    message = "Diagnostics initialized",
                ),
            ),
            userNote = request.userNote,
            generatedAt = System.currentTimeMillis(),
        )
    }
}

internal class FakeExporter : DiagnosticsExporter {
    override fun export(report: BugReport): String {
        return buildString {
            appendLine("Elemental Diagnostics Report")
            appendLine("Format Version: 1")
            appendLine("Library Version: 0.1.0")
            appendLine()
            appendLine("App")
            appendLine("- Name: ${report.metadata.appName}")
            report.userNote?.let {
                appendLine()
                appendLine("User Note")
                appendLine(it)
            }
            appendLine()
            appendLine("Recent Diagnostics")
            report.entries.forEach { entry ->
                appendLine("[test] ${entry.level} ${entry.tag}: ${entry.message}")
            }
        }.trimEnd()
    }
}

internal class FakeConfig : DiagnosticsConfig {
    override val appName = "TestApp"
    override val appId = "com.test.app"
    override val supportEmail = "test@example.com"
    override val maxStoredEntries = 100
    override val includeDeviceModelByDefault = true
    override val includeOsVersionByDefault = true
    override val redactor = DiagnosticsRedactor { it }
    override fun additionalMetadata() = emptyMap<String, String>()
}
