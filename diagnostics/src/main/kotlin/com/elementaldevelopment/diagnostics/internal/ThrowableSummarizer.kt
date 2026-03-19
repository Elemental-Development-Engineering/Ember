package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor

/**
 * Produces a sanitized single-line summary of a throwable.
 *
 * v1 constraints:
 * - Includes exception class name and sanitized message
 * - Max 300 characters total
 * - Single-line (newlines collapsed)
 * - No full stack traces
 * - No recursive cause chains
 * - No suppressed exceptions
 */
internal object ThrowableSummarizer {

    fun summarize(throwable: Throwable, redactor: DiagnosticsRedactor): String {
        val className = throwable::class.simpleName ?: throwable::class.java.name
        val message = throwable.message

        val summary = if (message != null) {
            val sanitizedMessage = redactor.redact(message)
            "$className: $sanitizedMessage"
        } else {
            className
        }

        return trimToMaxLength(summary, Limits.MAX_THROWABLE_MESSAGE_LENGTH)
    }
}
