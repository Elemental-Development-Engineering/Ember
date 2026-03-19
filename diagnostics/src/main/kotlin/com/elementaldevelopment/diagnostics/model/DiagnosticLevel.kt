package com.elementaldevelopment.diagnostics.model

/**
 * Severity level for diagnostic entries.
 *
 * In v1, [DEBUG] entries may be omitted from exported reports by default.
 */
enum class DiagnosticLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
}
