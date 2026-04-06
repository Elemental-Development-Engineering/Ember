package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.CrashCaptureUncaughtExceptionHandler
import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.RecoveredDiagnosticsRepository
import com.elementaldevelopment.diagnostics.internal.RecoveryState
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CrashCaptureUncaughtExceptionHandlerTest {

    @Test
    fun `captures final crash entry marks uncaught outcome and delegates`() {
        val repository = RecordingRecoveredDiagnosticsRepository()
        val delegated = mutableListOf<Throwable>()
        val handler = CrashCaptureUncaughtExceptionHandler(
            entryFactory = EntryFactory(DiagnosticsRedactor { input ->
                input.replace("SECRET", "[REDACTED]")
            }),
            recoveredDiagnosticsRepository = repository,
            delegate = Thread.UncaughtExceptionHandler { _, throwable ->
                delegated += throwable
            },
        )

        val throwable = IllegalStateException("SECRET failure")
        handler.uncaughtException(Thread.currentThread(), throwable)

        assertThat(repository.appendedEntries).hasSize(1)
        assertThat(repository.appendedEntries.single().tag).isEqualTo("Crash")
        assertThat(repository.appendedEntries.single().message)
            .isEqualTo("Uncaught exception terminated the app")
        assertThat(repository.appendedEntries.single().throwableSummary)
            .contains("[REDACTED] failure")
        assertThat(repository.appendedEntries.single().attributes).containsEntry(
            "source",
            "uncaught_exception",
        )
        assertThat(repository.markedUncaughtAt).isNotNull()
        assertThat(delegated).containsExactly(throwable)
    }
}

private class RecordingRecoveredDiagnosticsRepository : RecoveredDiagnosticsRepository {
    val appendedEntries = mutableListOf<DiagnosticEntry>()
    var markedUncaughtAt: Long? = null

    override fun recoverAndStartNewSession(sessionId: String, startedAt: Long): RecoveryState {
        return RecoveryState()
    }

    override fun appendToActiveSession(entry: DiagnosticEntry) {
        appendedEntries += entry
    }

    override fun markSessionOpen() = Unit

    override fun markCleanExit(endedAt: Long) = Unit

    override fun markUncaughtException(endedAt: Long) {
        markedUncaughtAt = endedAt
    }

    override fun getRecoveredEntries(limit: Int?): List<DiagnosticEntry> = emptyList()

    override fun clearAll() = Unit
}
