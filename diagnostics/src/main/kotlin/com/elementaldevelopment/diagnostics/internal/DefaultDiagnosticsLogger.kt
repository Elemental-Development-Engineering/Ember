package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.logging.DiagnosticsLogger
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.storage.DiagnosticsStore

/**
 * Default logger implementation that delegates to [EntryFactory] for sanitization
 * and [DiagnosticsStore] for storage.
 *
 * All entries are trimmed and redacted before reaching the store.
 */
internal class DefaultDiagnosticsLogger(
    private val entryFactory: EntryFactory,
    private val store: DiagnosticsStore,
) : DiagnosticsLogger {

    override fun log(tag: String, message: String) {
        log(DiagnosticLevel.INFO, tag, message)
    }

    override fun log(
        level: DiagnosticLevel,
        tag: String,
        message: String,
        attributes: Map<String, String>,
    ) {
        val entry = entryFactory.create(
            level = level,
            tag = tag,
            message = message,
            attributes = attributes,
        )
        store.append(entry)
    }

    override fun logError(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val entry = entryFactory.create(
            level = DiagnosticLevel.ERROR,
            tag = tag,
            message = message,
            throwable = throwable,
        )
        store.append(entry)
    }

    override fun recentEntries(limit: Int?): List<DiagnosticEntry> {
        return store.getRecent(limit)
    }

    override fun clear() {
        store.clear()
    }
}
