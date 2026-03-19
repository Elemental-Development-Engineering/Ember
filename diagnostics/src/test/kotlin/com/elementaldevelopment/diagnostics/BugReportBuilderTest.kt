package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.DefaultBugReportBuilder
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.BugReportRequest
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
}
