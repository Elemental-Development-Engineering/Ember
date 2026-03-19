package com.elementaldevelopment.diagnostics.storage

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry

/**
 * Abstraction for diagnostic entry storage.
 *
 * v1 provides only [com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore].
 * A persistent implementation (e.g., Room-backed) may be added in a future version
 * without changing the public API.
 */
interface DiagnosticsStore {
    fun append(entry: DiagnosticEntry)
    fun getRecent(limit: Int? = null): List<DiagnosticEntry>
    fun clear()
}
