package com.elementaldevelopment.diagnostics.model

/**
 * A single diagnostic event stored in the ring buffer.
 *
 * All fields are sanitized and trimmed at write time before storage.
 * The [message] and [throwableSummary] have already passed through
 * redaction and length trimming by the time they reach this class.
 *
 * [attributes] must contain only sanitized key/value pairs.
 */
data class DiagnosticEntry(
    val id: Long,
    val timestamp: Long,
    val level: DiagnosticLevel,
    val tag: String,
    val message: String,
    val throwableSummary: String? = null,
    val attributes: Map<String, String> = emptyMap(),
)
