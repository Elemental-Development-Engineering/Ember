package com.elementaldevelopment.diagnostics.redact

/**
 * Sanitizes diagnostic content before storage.
 *
 * All messages and throwable summaries pass through redaction at write time,
 * before they reach the store. This ensures the store never holds unredacted
 * content, regardless of storage backend.
 *
 * Apps provide their own redactor to strip sensitive values such as file paths,
 * user content, document titles, or other app-specific data.
 */
fun interface DiagnosticsRedactor {
    fun redact(input: String): String
}
