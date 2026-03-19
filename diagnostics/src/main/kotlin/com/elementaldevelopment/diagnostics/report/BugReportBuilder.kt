package com.elementaldevelopment.diagnostics.report

import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.BugReportRequest

/**
 * Builds a structured [BugReport] from the current diagnostics state.
 */
interface BugReportBuilder {
    fun build(request: BugReportRequest): BugReport
}
