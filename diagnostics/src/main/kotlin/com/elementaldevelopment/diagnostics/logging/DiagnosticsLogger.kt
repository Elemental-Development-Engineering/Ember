package com.elementaldevelopment.diagnostics.logging

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel

/**
 * Primary entry point for recording diagnostic events.
 *
 * All logged content is trimmed and redacted at write time before storage.
 * The logger never stores raw unsanitized content.
 */
interface DiagnosticsLogger {

    /**
     * Log an event at [DiagnosticLevel.INFO] level.
     */
    fun log(tag: String, message: String)

    /**
     * Log an event at the specified severity [level].
     */
    fun log(
        level: DiagnosticLevel,
        tag: String,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    )

    /**
     * Log an error event at [DiagnosticLevel.ERROR] level.
     *
     * If a [throwable] is provided, a sanitized summary is stored containing
     * the exception class name and a trimmed, single-line message.
     * Full stack traces are not stored by default.
     */
    fun logError(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /**
     * Returns recent diagnostic entries, newest last.
     *
     * @param limit Maximum number of entries to return, or null for all stored entries.
     */
    fun recentEntries(limit: Int? = null): List<DiagnosticEntry>

    /**
     * Clears all stored diagnostic entries.
     */
    fun clear()
}
