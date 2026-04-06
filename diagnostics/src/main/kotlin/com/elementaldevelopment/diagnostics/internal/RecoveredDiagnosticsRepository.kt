package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome

internal data class RecoveryState(
    val recoveredEntries: List<DiagnosticEntry> = emptyList(),
    val previousSessionOutcome: PreviousSessionOutcome = PreviousSessionOutcome.NONE,
    val previousSessionId: String? = null,
    val previousSessionTimestamp: Long? = null,
)

internal interface RecoveredDiagnosticsRepository {
    fun recoverAndStartNewSession(sessionId: String, startedAt: Long): RecoveryState
    fun appendToActiveSession(entry: DiagnosticEntry)
    fun markSessionOpen()
    fun markCleanExit(endedAt: Long)
    fun getRecoveredEntries(limit: Int? = null): List<DiagnosticEntry>
    fun clearAll()
}
