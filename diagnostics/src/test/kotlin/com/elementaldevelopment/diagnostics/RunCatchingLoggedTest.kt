package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.DefaultDiagnosticsLogger
import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.logging.runCatchingLogged
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RunCatchingLoggedTest {

    private val noOpRedactor = DiagnosticsRedactor { it }

    private fun createLogger(): DefaultDiagnosticsLogger {
        val store = InMemoryDiagnosticsStore(100)
        val factory = EntryFactory(noOpRedactor)
        return DefaultDiagnosticsLogger(factory, store)
    }

    @Test
    fun `returns success when block succeeds`() {
        val logger = createLogger()

        val result = logger.runCatchingLogged("Test", "operation") {
            42
        }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42)
    }

    @Test
    fun `does not log when block succeeds`() {
        val logger = createLogger()

        logger.runCatchingLogged("Test", "operation") { "ok" }

        assertThat(logger.recentEntries()).isEmpty()
    }

    @Test
    fun `returns failure when block throws`() {
        val logger = createLogger()

        val result = logger.runCatchingLogged("Test", "save") {
            throw IllegalStateException("disk full")
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `logs error with action name when block throws`() {
        val logger = createLogger()

        logger.runCatchingLogged("Storage", "save") {
            throw IllegalStateException("disk full")
        }

        val entries = logger.recentEntries()
        assertThat(entries).hasSize(1)
        assertThat(entries[0].level).isEqualTo(DiagnosticLevel.ERROR)
        assertThat(entries[0].tag).isEqualTo("Storage")
        assertThat(entries[0].message).isEqualTo("save failed")
    }

    @Test
    fun `includes throwable summary in logged entry`() {
        val logger = createLogger()

        logger.runCatchingLogged("Net", "fetch") {
            throw RuntimeException("timeout")
        }

        val entries = logger.recentEntries()
        assertThat(entries[0].throwableSummary).contains("RuntimeException")
    }
}
