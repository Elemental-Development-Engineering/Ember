package com.elementaldevelopment.diagnostics.config

import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor

/**
 * Per-app configuration for the diagnostics library.
 *
 * Each app provides its own implementation to control identity metadata,
 * report behavior, and redaction policy.
 *
 * [maxStoredEntries] is the maximum number of entries the ring buffer will hold
 * in memory at any time. Oldest entries are evicted when this limit is reached.
 * This is distinct from [com.elementaldevelopment.diagnostics.model.BugReportRequest.maxEntries],
 * which controls how many entries appear in a specific report.
 */
interface DiagnosticsConfig {
    val appName: String
    val appId: String
    val supportEmail: String?
    val maxStoredEntries: Int
    val includeDeviceModelByDefault: Boolean
    val includeOsVersionByDefault: Boolean
    val redactor: DiagnosticsRedactor

    fun additionalMetadata(): Map<String, String>
}
