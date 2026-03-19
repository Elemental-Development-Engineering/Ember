package com.elementaldevelopment.diagnostics.export

import com.elementaldevelopment.diagnostics.model.BugReport

/**
 * Converts a structured [BugReport] into a human-readable plain text payload
 * suitable for sharing or copying.
 */
interface DiagnosticsExporter {
    fun export(report: BugReport): String
}
