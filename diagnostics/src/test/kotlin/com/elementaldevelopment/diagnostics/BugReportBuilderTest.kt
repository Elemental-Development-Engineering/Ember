package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.DefaultBugReportBuilder
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.internal.RecoveredDiagnosticsRepository
import com.elementaldevelopment.diagnostics.internal.RecoveryState
import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BugReportBuilderTest {

    private val noOpRedactor = DiagnosticsRedactor { it }

    private val metadataProvider = object : DiagnosticsMetadataProvider {
        override fun collect() = DiagnosticsMetadata(
            appName = "TestApp",
            appId = "com.test.app",
            versionName = "1.0.0",
            versionCode = 1,
            androidVersion = "15",
            apiLevel = 35,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8",
            generatedAt = 1710864600000L,
            libraryVersion = "0.1.0",
            sessionId = "test-session-123",
        )
    }

    @Test
    fun `removes app info when excluded`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = metadataProvider,
            store = InMemoryDiagnosticsStore(10),
            redactor = noOpRedactor,
        )

        val report = builder.build(BugReportRequest(includeAppInfo = false))

        assertThat(report.metadata.appName).isEmpty()
        assertThat(report.metadata.appId).isEmpty()
        assertThat(report.metadata.versionName).isEmpty()
        assertThat(report.metadata.versionCode).isEqualTo(0)
    }

    @Test
    fun `removes device and os info when excluded`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = metadataProvider,
            store = InMemoryDiagnosticsStore(10),
            redactor = noOpRedactor,
        )

        val report = builder.build(
            BugReportRequest(
                includeDeviceInfo = false,
                includeOsInfo = false,
            ),
        )

        assertThat(report.metadata.deviceManufacturer).isEmpty()
        assertThat(report.metadata.deviceModel).isEmpty()
        assertThat(report.metadata.androidVersion).isEmpty()
        assertThat(report.metadata.apiLevel).isEqualTo(0)
    }

    @Test
    fun `redacts additional metadata before export`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = object : DiagnosticsMetadataProvider {
                override fun collect() = metadataProvider.collect().copy(
                    additionalMetadata = mapOf(
                        "auth token" to "SECRET value",
                    ),
                )
            },
            store = InMemoryDiagnosticsStore(10),
            redactor = DiagnosticsRedactor { input ->
                input.replace("SECRET", "[REDACTED]")
                    .replace("token", "field")
            },
        )

        val report = builder.build(BugReportRequest())

        assertThat(report.metadata.additionalMetadata).containsExactly(
            "auth field",
            "[REDACTED] value",
        )
    }

    @Test
    fun `redacts user note before export`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = metadataProvider,
            store = InMemoryDiagnosticsStore(10),
            redactor = DiagnosticsRedactor { input ->
                input.replace("SECRET", "[REDACTED]")
            },
        )

        val report = builder.build(BugReportRequest(userNote = "Contains SECRET details"))

        assertThat(report.userNote).isEqualTo("Contains [REDACTED] details")
    }

    @Test
    fun `includes recovered entries when requested`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = metadataProvider,
            store = InMemoryDiagnosticsStore(10),
            redactor = noOpRedactor,
            recoveredDiagnosticsRepository = FakeRecoveredDiagnosticsRepository(
                entries = listOf(
                    DiagnosticEntry(
                        id = 42,
                        timestamp = 1710864500000L,
                        level = DiagnosticLevel.ERROR,
                        tag = "Crash",
                        message = "Recovered from disk",
                    ),
                ),
            ),
        )

        val report = builder.build(BugReportRequest(includeRecoveredLogs = true))

        assertThat(report.recoveredEntries).hasSize(1)
        assertThat(report.recoveredEntries.single().message).isEqualTo("Recovered from disk")
    }

    @Test
    fun `omits recovered entries when excluded`() {
        val builder = DefaultBugReportBuilder(
            metadataProvider = metadataProvider,
            store = InMemoryDiagnosticsStore(10),
            redactor = noOpRedactor,
            recoveredDiagnosticsRepository = FakeRecoveredDiagnosticsRepository(
                entries = listOf(
                    DiagnosticEntry(
                        id = 42,
                        timestamp = 1710864500000L,
                        level = DiagnosticLevel.ERROR,
                        tag = "Crash",
                        message = "Recovered from disk",
                    ),
                ),
            ),
        )

        val report = builder.build(BugReportRequest(includeRecoveredLogs = false))

        assertThat(report.recoveredEntries).isEmpty()
    }
}

private class FakeRecoveredDiagnosticsRepository(
    private val entries: List<DiagnosticEntry>,
) : RecoveredDiagnosticsRepository {
    override fun recoverAndStartNewSession(sessionId: String, startedAt: Long): RecoveryState {
        return RecoveryState()
    }

    override fun appendToActiveSession(entry: DiagnosticEntry) = Unit

    override fun markSessionOpen() = Unit

    override fun markCleanExit(endedAt: Long) = Unit

    override fun markUncaughtException(endedAt: Long) = Unit

    override fun getRecoveredEntries(limit: Int?): List<DiagnosticEntry> {
        return if (limit != null) entries.takeLast(limit) else entries
    }

    override fun clearAll() = Unit
}
