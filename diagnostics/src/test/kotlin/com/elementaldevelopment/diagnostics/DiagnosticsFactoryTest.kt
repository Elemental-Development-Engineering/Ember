package com.elementaldevelopment.diagnostics

import android.content.Context
import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.api.create
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiagnosticsFactoryTest {

    private val testConfig = object : DiagnosticsConfig {
        override val appName = "TestApp"
        override val appId = "com.test.app"
        override val supportEmail = "test@test.com"
        override val maxStoredEntries = 50
        override val includeDeviceModelByDefault = true
        override val includeOsVersionByDefault = true
        override val redactor = DiagnosticsRedactor { it }
        override fun additionalMetadata() = emptyMap<String, String>()
    }

    private fun createDiagnostics(
        context: Context = RuntimeEnvironment.getApplication(),
        config: DiagnosticsConfig = testConfig,
    ): Diagnostics = Diagnostics.create(context, config)

    @Test
    fun `returns a non-null Diagnostics instance`() {
        val diagnostics = createDiagnostics()
        assertThat(diagnostics).isNotNull()
    }

    @Test
    fun `exposes logger, reportBuilder, exporter, and config`() {
        val diagnostics = createDiagnostics()
        assertThat(diagnostics.logger).isNotNull()
        assertThat(diagnostics.reportBuilder).isNotNull()
        assertThat(diagnostics.exporter).isNotNull()
        assertThat(diagnostics.config).isEqualTo(testConfig)
    }

    @Test
    fun `auto-logs session start entry`() {
        val diagnostics = createDiagnostics()
        val entries = diagnostics.logger.recentEntries()

        assertThat(entries).hasSize(1)
        assertThat(entries[0].level).isEqualTo(DiagnosticLevel.INFO)
        assertThat(entries[0].tag).isEqualTo("System")
        assertThat(entries[0].message).isEqualTo("Diagnostics initialized")
    }

    @Test
    fun `respects maxStoredEntries from config`() {
        val smallConfig = object : DiagnosticsConfig by testConfig {
            override val maxStoredEntries = 3
        }
        val diagnostics = createDiagnostics(config = smallConfig)

        // 1 auto-logged entry + 5 manual entries = 6, but store caps at 3
        repeat(5) { diagnostics.logger.log("tag", "msg $it") }

        assertThat(diagnostics.logger.recentEntries()).hasSize(3)
    }

    @Test
    fun `applies redactor to logged entries`() {
        val redactingConfig = object : DiagnosticsConfig by testConfig {
            override val redactor = DiagnosticsRedactor { input ->
                input.replace("SECRET", "[REDACTED]")
            }
        }
        val diagnostics = createDiagnostics(config = redactingConfig)

        diagnostics.logger.log("tag", "the SECRET value")

        val entries = diagnostics.logger.recentEntries()
        val message = entries.last().message
        assertThat(message).contains("[REDACTED]")
        assertThat(message).doesNotContain("SECRET")
    }

    @Test
    fun `logger and reportBuilder share the same store`() {
        val diagnostics = createDiagnostics()
        diagnostics.logger.log("Test", "hello")

        val report = diagnostics.reportBuilder.build(
            com.elementaldevelopment.diagnostics.model.BugReportRequest()
        )

        // Should contain auto-log + our entry
        assertThat(report.entries).hasSize(2)
    }

    @Test
    fun `each create call generates a unique session`() {
        val d1 = createDiagnostics()
        val d2 = createDiagnostics()

        val report1 = d1.reportBuilder.build(
            com.elementaldevelopment.diagnostics.model.BugReportRequest()
        )
        val report2 = d2.reportBuilder.build(
            com.elementaldevelopment.diagnostics.model.BugReportRequest()
        )

        assertThat(report1.metadata.sessionId).isNotEqualTo(report2.metadata.sessionId)
    }
}
