package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.export.DiagnosticsExporter
import com.elementaldevelopment.diagnostics.logging.DiagnosticsLogger
import com.elementaldevelopment.diagnostics.report.BugReportBuilder

internal class DefaultDiagnostics(
    override val logger: DiagnosticsLogger,
    override val reportBuilder: BugReportBuilder,
    override val exporter: DiagnosticsExporter,
    override val config: DiagnosticsConfig,
) : Diagnostics
