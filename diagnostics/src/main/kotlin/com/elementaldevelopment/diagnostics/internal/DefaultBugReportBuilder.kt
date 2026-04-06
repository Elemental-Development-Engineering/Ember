package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.elementaldevelopment.diagnostics.report.BugReportBuilder
import com.elementaldevelopment.diagnostics.storage.DiagnosticsStore

/**
 * Builds a structured [BugReport] from metadata, stored entries,
 * and the user's report request options.
 */
internal class DefaultBugReportBuilder(
    private val metadataProvider: DiagnosticsMetadataProvider,
    private val store: DiagnosticsStore,
    private val redactor: DiagnosticsRedactor,
    private val recoveredDiagnosticsRepository: RecoveredDiagnosticsRepository? = null,
) : BugReportBuilder {

    override fun build(request: BugReportRequest): BugReport {
        val metadata = metadataProvider.collect()
            .redacted()
            .filtered(request)

        val entries = if (request.includeRecentLogs) {
            store.getRecent(request.maxEntries)
        } else {
            emptyList()
        }

        val recoveredEntries = if (request.includeRecoveredLogs) {
            recoveredDiagnosticsRepository?.getRecoveredEntries(request.maxRecoveredEntries).orEmpty()
        } else {
            emptyList()
        }

        val userNote = request.userNote?.let {
            trimToMaxLength(redactor.redact(it.trim()), Limits.MAX_USER_NOTE_LENGTH)
        }

        return BugReport(
            metadata = metadata,
            entries = entries,
            recoveredEntries = recoveredEntries,
            userNote = userNote,
            generatedAt = System.currentTimeMillis(),
        )
    }

    private fun com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata.filtered(
        request: BugReportRequest,
    ): com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata {
        return copy(
            appName = appName.takeIf { request.includeAppInfo }.orEmpty(),
            appId = appId.takeIf { request.includeAppInfo }.orEmpty(),
            versionName = versionName.takeIf { request.includeAppInfo }.orEmpty(),
            versionCode = versionCode.takeIf { request.includeAppInfo } ?: 0L,
            androidVersion = androidVersion.takeIf { request.includeOsInfo }.orEmpty(),
            apiLevel = apiLevel.takeIf { request.includeOsInfo } ?: 0,
            deviceManufacturer = deviceManufacturer.takeIf { request.includeDeviceInfo }.orEmpty(),
            deviceModel = deviceModel.takeIf { request.includeDeviceInfo }.orEmpty(),
        )
    }

    private fun com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata.redacted(
    ): com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata {
        return copy(
            additionalMetadata = additionalMetadata.entries.associate { (key, value) ->
                redactor.redact(key) to redactor.redact(value)
            },
        )
    }
}
