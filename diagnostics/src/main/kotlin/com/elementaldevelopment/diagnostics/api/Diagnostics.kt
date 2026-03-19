package com.elementaldevelopment.diagnostics.api

import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.export.DiagnosticsExporter
import com.elementaldevelopment.diagnostics.logging.DiagnosticsLogger
import com.elementaldevelopment.diagnostics.report.BugReportBuilder

/**
 * Top-level container for the diagnostics library.
 *
 * Apps initialize diagnostics once during startup via [Diagnostics.create]
 * and use this container to access all library functionality.
 *
 * The UI module depends on this container rather than threading
 * individual interfaces, keeping integration simple.
 */
interface Diagnostics {
    val logger: DiagnosticsLogger
    val reportBuilder: BugReportBuilder
    val exporter: DiagnosticsExporter
    val config: DiagnosticsConfig

    companion object
}
