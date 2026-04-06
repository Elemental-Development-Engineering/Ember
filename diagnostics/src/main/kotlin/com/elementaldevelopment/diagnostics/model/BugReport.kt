package com.elementaldevelopment.diagnostics.model

/**
 * Structured bug report output before rendering to text.
 *
 * This is the intermediate representation built by [com.elementaldevelopment.diagnostics.report.BugReportBuilder]
 * and consumed by [com.elementaldevelopment.diagnostics.export.DiagnosticsExporter] to produce
 * the final human-readable text.
 */
data class BugReport(
    val metadata: DiagnosticsMetadata,
    val entries: List<DiagnosticEntry>,
    val recoveredEntries: List<DiagnosticEntry> = emptyList(),
    val userNote: String?,
    val generatedAt: Long,
    val formatVersion: String = "2",
)
