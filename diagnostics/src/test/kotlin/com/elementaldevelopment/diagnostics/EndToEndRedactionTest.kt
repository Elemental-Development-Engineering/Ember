package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.ComposedRedactor
import com.elementaldevelopment.diagnostics.internal.DefaultBugReportBuilder
import com.elementaldevelopment.diagnostics.internal.DefaultDiagnosticsLogger
import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.internal.PlainTextExporter
import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * End-to-end test: Sensitive content is never present in exported report.
 *
 * This is the most important privacy-related test in the suite.
 * It spans the full pipeline: log → store → report → export.
 */
class EndToEndRedactionTest {

    @Test
    fun `sensitive content is redacted in exported report`() {
        // 1. Configure a redactor that replaces "SECRET" with "[REDACTED]"
        val appRedactor = DiagnosticsRedactor { it.replace("SECRET", "[REDACTED]") }
        val composedRedactor = ComposedRedactor(appRedactor)

        val store = InMemoryDiagnosticsStore(100)
        val entryFactory = EntryFactory(composedRedactor)
        val logger = DefaultDiagnosticsLogger(entryFactory, store)

        val metadataProvider = object : DiagnosticsMetadataProvider {
            override fun collect() = DiagnosticsMetadata(
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
            )
        }

        val reportBuilder = DefaultBugReportBuilder(metadataProvider, store, composedRedactor)
        val exporter = PlainTextExporter()

        // 2. Log an entry containing "SECRET"
        logger.log("test", "password is SECRET")
        logger.logError("auth", "login failed with SECRET token",
            RuntimeException("SECRET credential expired"))

        // 3. Build report
        val report = reportBuilder.build(BugReportRequest(
            userNote = "Help me debug this",
        ))

        // 4. Export report
        val exportedText = exporter.export(report)

        // 5. Assert exported report contains "[REDACTED]"
        assertThat(exportedText).contains("[REDACTED]")

        // 6. Assert exported report does NOT contain "SECRET"
        assertThat(exportedText).doesNotContain("SECRET")
    }

    @Test
    fun `sensitive content in throwable is redacted in exported report`() {
        val appRedactor = DiagnosticsRedactor { it.replace("my-password", "[REDACTED]") }
        val composedRedactor = ComposedRedactor(appRedactor)

        val store = InMemoryDiagnosticsStore(100)
        val entryFactory = EntryFactory(composedRedactor)
        val logger = DefaultDiagnosticsLogger(entryFactory, store)

        val metadataProvider = object : DiagnosticsMetadataProvider {
            override fun collect() = DiagnosticsMetadata(
                appName = "TestApp",
                appId = "com.test.app",
                versionName = "1.0.0",
                versionCode = 1,
                androidVersion = "15",
                apiLevel = 35,
                deviceManufacturer = "",
                deviceModel = "",
                generatedAt = System.currentTimeMillis(),
                libraryVersion = "0.1.0",
                sessionId = "test-session",
            )
        }

        val reportBuilder = DefaultBugReportBuilder(metadataProvider, store, composedRedactor)
        val exporter = PlainTextExporter()

        logger.logError("auth", "failed", RuntimeException("auth error: my-password invalid"))

        val report = reportBuilder.build(BugReportRequest())
        val exportedText = exporter.export(report)

        assertThat(exportedText).doesNotContain("my-password")
        assertThat(exportedText).contains("[REDACTED]")
    }

    @Test
    fun `path-like content can be redacted through the full pipeline`() {
        val pathRedactor = DiagnosticsRedactor { input ->
            input.replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
        }
        val composedRedactor = ComposedRedactor(pathRedactor)

        val store = InMemoryDiagnosticsStore(100)
        val entryFactory = EntryFactory(composedRedactor)
        val logger = DefaultDiagnosticsLogger(entryFactory, store)

        val metadataProvider = object : DiagnosticsMetadataProvider {
            override fun collect() = DiagnosticsMetadata(
                appName = "TestApp",
                appId = "com.test.app",
                versionName = "1.0.0",
                versionCode = 1,
                androidVersion = "15",
                apiLevel = 35,
                deviceManufacturer = "",
                deviceModel = "",
                generatedAt = System.currentTimeMillis(),
                libraryVersion = "0.1.0",
                sessionId = "test-session",
            )
        }

        val reportBuilder = DefaultBugReportBuilder(metadataProvider, store, composedRedactor)
        val exporter = PlainTextExporter()

        logger.log("FileIO", "Opening /storage/emulated/0/Documents/private.md")

        val report = reportBuilder.build(BugReportRequest())
        val exportedText = exporter.export(report)

        assertThat(exportedText).contains("[REDACTED_PATH]")
        assertThat(exportedText).doesNotContain("private.md")
        assertThat(exportedText).doesNotContain("/storage/emulated/0")
    }
}
