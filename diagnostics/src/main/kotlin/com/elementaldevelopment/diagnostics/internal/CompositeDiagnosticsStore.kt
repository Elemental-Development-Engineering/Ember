package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.storage.DiagnosticsStore

internal class CompositeDiagnosticsStore(
    private val memoryStore: DiagnosticsStore,
    private val recoveredRepository: RecoveredDiagnosticsRepository,
) : DiagnosticsStore {

    override fun append(entry: DiagnosticEntry) {
        memoryStore.append(entry)
        recoveredRepository.appendToActiveSession(entry)
    }

    override fun getRecent(limit: Int?): List<DiagnosticEntry> {
        return memoryStore.getRecent(limit)
    }

    override fun clear() {
        memoryStore.clear()
        recoveredRepository.clearAll()
    }
}
