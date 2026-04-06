package com.elementaldevelopment.diagnostics

import android.content.Context
import com.elementaldevelopment.diagnostics.config.CrashPersistenceConfig
import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.api.create
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.internal.CrashCaptureUncaughtExceptionHandler
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiagnosticsFactoryTest {
    private val originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

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

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler)
    }

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

    @Test
    fun `recovers previous active session entries when crash persistence is enabled`() {
        val recoveryConfig = object : DiagnosticsConfig by testConfig {
            override val appId = "com.test.app.persistence"
            override val crashPersistence = CrashPersistenceConfig.Enabled(
                maxPersistedEntries = 10,
                retentionAfterRecoveryMillis = 60_000L,
            )
        }
        clearPersistenceFile(recoveryConfig.appId)

        val first = createDiagnostics(config = recoveryConfig)
        first.logger.log("Parser", "before restart")

        val second = createDiagnostics(config = recoveryConfig)
        val report = second.reportBuilder.build(
            com.elementaldevelopment.diagnostics.model.BugReportRequest()
        )

        assertThat(report.metadata.previousSessionOutcome)
            .isEqualTo(PreviousSessionOutcome.UNEXPECTED_TERMINATION)
        assertThat(report.recoveredEntries).isNotEmpty()
        assertThat(report.recoveredEntries.map { it.message }).contains("before restart")
        assertThat(report.entries.map { it.message }).contains("Diagnostics initialized")
    }

    @Test
    fun `installs uncaught exception handler only when crash persistence is enabled`() {
        val sentinelHandler = Thread.UncaughtExceptionHandler { _, _ -> Unit }
        Thread.setDefaultUncaughtExceptionHandler(sentinelHandler)

        createDiagnostics()
        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isSameInstanceAs(sentinelHandler)

        val recoveryConfig = object : DiagnosticsConfig by testConfig {
            override val appId = "com.test.app.handler"
            override val crashPersistence = CrashPersistenceConfig.Enabled(
                maxPersistedEntries = 10,
                retentionAfterRecoveryMillis = 60_000L,
            )
        }
        clearPersistenceFile(recoveryConfig.appId)

        createDiagnostics(config = recoveryConfig)

        assertThat(Thread.getDefaultUncaughtExceptionHandler())
            .isInstanceOf(CrashCaptureUncaughtExceptionHandler::class.java)
    }

    @Test
    fun `repeated initialization replaces ember handler instead of nesting delegates`() {
        val recoveryConfig = object : DiagnosticsConfig by testConfig {
            override val appId = "com.test.app.repeat"
            override val crashPersistence = CrashPersistenceConfig.Enabled(
                maxPersistedEntries = 10,
                retentionAfterRecoveryMillis = 60_000L,
            )
        }
        clearPersistenceFile(recoveryConfig.appId)

        val sentinelHandler = Thread.UncaughtExceptionHandler { _, _ -> Unit }
        Thread.setDefaultUncaughtExceptionHandler(sentinelHandler)

        createDiagnostics(config = recoveryConfig)
        val firstHandler = Thread.getDefaultUncaughtExceptionHandler()
        assertThat(firstHandler).isInstanceOf(CrashCaptureUncaughtExceptionHandler::class.java)
        assertThat((firstHandler as CrashCaptureUncaughtExceptionHandler).delegate)
            .isSameInstanceAs(sentinelHandler)

        createDiagnostics(config = recoveryConfig)
        val secondHandler = Thread.getDefaultUncaughtExceptionHandler()
        assertThat(secondHandler).isInstanceOf(CrashCaptureUncaughtExceptionHandler::class.java)
        assertThat(secondHandler).isNotSameInstanceAs(firstHandler)
        assertThat((secondHandler as CrashCaptureUncaughtExceptionHandler).delegate)
            .isSameInstanceAs(sentinelHandler)
    }

    @Test
    fun `uncaught exception is recovered as previous crash on next startup`() {
        val recoveryConfig = object : DiagnosticsConfig by testConfig {
            override val appId = "com.test.app.uncaught"
            override val crashPersistence = CrashPersistenceConfig.Enabled(
                maxPersistedEntries = 10,
                retentionAfterRecoveryMillis = 60_000L,
            )
        }
        clearPersistenceFile(recoveryConfig.appId)

        val sentinelHandler = Thread.UncaughtExceptionHandler { _, _ -> Unit }
        Thread.setDefaultUncaughtExceptionHandler(sentinelHandler)

        createDiagnostics(config = recoveryConfig)
        val installedHandler = Thread.getDefaultUncaughtExceptionHandler()
        assertThat(installedHandler).isInstanceOf(CrashCaptureUncaughtExceptionHandler::class.java)

        checkNotNull(installedHandler).uncaughtException(
            Thread.currentThread(),
            IllegalStateException("boom"),
        )

        val recovered = createDiagnostics(config = recoveryConfig)
        val report = recovered.reportBuilder.build(
            com.elementaldevelopment.diagnostics.model.BugReportRequest()
        )

        assertThat(report.metadata.previousSessionOutcome)
            .isEqualTo(PreviousSessionOutcome.UNCUGHT_EXCEPTION)
        assertThat(report.recoveredEntries.map { it.tag }).contains("Crash")
        assertThat(report.recoveredEntries.mapNotNull { it.throwableSummary })
            .contains("IllegalStateException: boom")
    }

    private fun clearPersistenceFile(appId: String) {
        val context: Context = RuntimeEnvironment.getApplication()
        val sanitizedAppId = appId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(File(context.filesDir, "ember"), "${sanitizedAppId}_diagnostics_snapshot.json")
        if (file.exists()) {
            file.delete()
        }
    }
}
