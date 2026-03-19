package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.report.BugReportBuilder
import com.elementaldevelopment.diagnostics.storage.DiagnosticsStore

/**
 * Builds a structured [BugReport] from metadata, stored entries,
 * and the user's report request options.
 */
internal class DefaultBugReportBuilder(
    private val metadataProvider: DiagnosticsMetadataProvider,
    private val store: DiagnosticsStore,
) : BugReportBuilder {

    override fun build(request: BugReportRequest): BugReport {
        val metadata = metadataProvider.collect()

        val entries = if (request.includeRecentLogs) {
            store.getRecent(request.maxEntries)
        } else {
            emptyList()
        }

        val userNote = request.userNote?.let {
            trimToMaxLength(it.trim(), Limits.MAX_USER_NOTE_LENGTH)
        }

        return BugReport(
            metadata = metadata,
            entries = entries,
            userNote = userNote,
            generatedAt = System.currentTimeMillis(),
        )
    }
}
