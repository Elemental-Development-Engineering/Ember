package com.elementaldevelopment.diagnostics.model

/**
 * Controls how a bug report should be built.
 *
 * [maxEntries] limits the number of stored entries included in this specific report.
 * This may be less than [com.elementaldevelopment.diagnostics.config.DiagnosticsConfig.maxStoredEntries]
 * to keep reports concise. For example, the buffer might hold 500 entries while a
 * typical report only includes the most recent 50.
 */
data class BugReportRequest(
    val userNote: String? = null,
    val includeDeviceInfo: Boolean = true,
    val includeOsInfo: Boolean = true,
    val includeAppInfo: Boolean = true,
    val includeRecentLogs: Boolean = true,
    val maxEntries: Int = 50,
)
