package com.elementaldevelopment.diagnostics.sample

import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor

/**
 * Example configuration for a general utility app (e.g., PlainMark, PlainTimers).
 *
 * Lower sensitivity — redacts file paths and user content but keeps
 * most diagnostic information available for debugging.
 */
class GeneralUtilityAppConfig : DiagnosticsConfig {
    override val appName = "PlainMark"
    override val appId = "com.elementaldevelopment.plainmark"
    override val supportEmail = "support@elementaldevelopment.com"
    override val maxStoredEntries = 300
    override val includeDeviceModelByDefault = true
    override val includeOsVersionByDefault = true

    override val redactor = DiagnosticsRedactor { input ->
        input
            .replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
            .replace(Regex("/data/user/0/[^\\s]+"), "[REDACTED_PATH]")
    }

    override fun additionalMetadata(): Map<String, String> = emptyMap()
}

/**
 * Example configuration for a highly sensitive app (e.g., Private Vault).
 *
 * Most restrictive setup — redacts all item names, folder names,
 * user notes, file references, and any content that could identify
 * what the user stores in the app.
 */
class HighSensitivityAppConfig : DiagnosticsConfig {
    override val appName = "Private Vault"
    override val appId = "com.elementaldevelopment.privatevault"
    override val supportEmail = "support@elementaldevelopment.com"
    override val maxStoredEntries = 200
    override val includeDeviceModelByDefault = true
    override val includeOsVersionByDefault = true

    override val redactor = DiagnosticsRedactor { input ->
        input
            // Redact file paths
            .replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
            .replace(Regex("/data/user/0/[^\\s]+"), "[REDACTED_PATH]")
            // Redact quoted names (item names, folder names)
            .replace(Regex("\"[^\"]+\""), "\"[REDACTED_NAME]\"")
            // Redact content after common labels
            .replace(Regex("(?i)(note|title|name|label|content):\\s*\\S+"), "$1: [REDACTED]")
    }

    override fun additionalMetadata(): Map<String, String> = emptyMap()
}
