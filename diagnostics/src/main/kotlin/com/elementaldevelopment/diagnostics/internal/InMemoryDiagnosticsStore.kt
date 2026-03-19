package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.storage.DiagnosticsStore

/**
 * Bounded in-memory ring buffer for diagnostic entries.
 *
 * Thread-safe via [synchronized]. Eviction is eager on [append] —
 * the buffer never exceeds [maxCapacity].
 *
 * Note: high-frequency INFO logging can evict older ERROR entries.
 * Error pinning is intentionally deferred beyond v1 to keep the
 * implementation simple. See architecture addendum A2.
 *
 * TODO: Consider error pinning (reserving buffer space for ERROR/FATAL)
 *  in v1.1+ if real-world usage demonstrates a need.
 */
internal class InMemoryDiagnosticsStore(
    private val maxCapacity: Int,
) : DiagnosticsStore {
    init {
        require(maxCapacity > 0) { "maxCapacity must be > 0" }
    }

    private val lock = Any()
    private val buffer = ArrayDeque<DiagnosticEntry>(maxCapacity)

    override fun append(entry: DiagnosticEntry) = synchronized(lock) {
        if (buffer.size >= maxCapacity) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
    }

    override fun getRecent(limit: Int?): List<DiagnosticEntry> = synchronized(lock) {
        val entries = buffer.toList()
        if (limit != null) entries.takeLast(limit) else entries
    }

    override fun clear() = synchronized(lock) {
        buffer.clear()
    }
}
