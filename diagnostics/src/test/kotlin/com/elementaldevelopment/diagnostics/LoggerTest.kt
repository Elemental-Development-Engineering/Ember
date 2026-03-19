package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.DefaultDiagnosticsLogger
import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoggerTest {

    private val noOpRedactor = DiagnosticsRedactor { it }

    private fun createLogger(maxEntries: Int = 100): DefaultDiagnosticsLogger {
        val store = InMemoryDiagnosticsStore(maxEntries)
        val factory = EntryFactory(noOpRedactor)
        return DefaultDiagnosticsLogger(factory, store)
    }

    @Test
    fun `log with tag and message defaults to INFO`() {
        val logger = createLogger()
        logger.log("MyTag", "hello")

        val entries = logger.recentEntries()
        assertThat(entries).hasSize(1)
        assertThat(entries[0].level).isEqualTo(DiagnosticLevel.INFO)
        assertThat(entries[0].tag).isEqualTo("MyTag")
        assertThat(entries[0].message).isEqualTo("hello")
    }

    @Test
    fun `log with explicit level`() {
        val logger = createLogger()
        logger.log(DiagnosticLevel.WARN, "MyTag", "warning message")

        val entries = logger.recentEntries()
        assertThat(entries[0].level).isEqualTo(DiagnosticLevel.WARN)
    }

    @Test
    fun `logError records at ERROR level`() {
        val logger = createLogger()
        logger.logError("MyTag", "something failed")

        val entries = logger.recentEntries()
        assertThat(entries[0].level).isEqualTo(DiagnosticLevel.ERROR)
    }

    @Test
    fun `logError includes throwable summary`() {
        val logger = createLogger()
        logger.logError("MyTag", "failed", IllegalStateException("bad state"))

        val entries = logger.recentEntries()
        assertThat(entries[0].throwableSummary).contains("IllegalStateException")
    }

    @Test
    fun `recentEntries with limit`() {
        val logger = createLogger()
        repeat(10) { logger.log("tag", "msg $it") }

        val entries = logger.recentEntries(3)
        assertThat(entries).hasSize(3)
    }

    @Test
    fun `clear removes all entries`() {
        val logger = createLogger()
        logger.log("tag", "msg")
        logger.clear()

        assertThat(logger.recentEntries()).isEmpty()
    }

    @Test
    fun `maxEntries vs maxStoredEntries distinction`() {
        // Store holds up to 20 entries
        val store = InMemoryDiagnosticsStore(20)
        val factory = EntryFactory(noOpRedactor)
        val logger = DefaultDiagnosticsLogger(factory, store)

        repeat(20) { logger.log("tag", "msg $it") }

        // Store has 20 entries, but we request only 5 in the report
        val entries = logger.recentEntries(5)
        assertThat(entries).hasSize(5)

        // Store still has all 20
        assertThat(logger.recentEntries()).hasSize(20)
    }

    @Test
    fun `log with attributes`() {
        val logger = createLogger()
        logger.log(DiagnosticLevel.INFO, "tag", "msg", mapOf("key" to "value"))

        val entries = logger.recentEntries()
        assertThat(entries[0].attributes).containsEntry("key", "value")
    }
}
