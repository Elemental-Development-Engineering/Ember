package com.elementaldevelopment.diagnostics.model

/**
 * Technical environment metadata collected for a bug report.
 *
 * Contains only technical diagnostics needed for debugging.
 * Never includes user content, account identifiers, or
 * hardware identifiers intended to uniquely identify a device.
 */
data class DiagnosticsMetadata(
    val appName: String,
    val appId: String,
    val versionName: String,
    val versionCode: Long,
    val androidVersion: String,
    val apiLevel: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val generatedAt: Long,
    val libraryVersion: String,
    val sessionId: String,
    val previousSessionOutcome: PreviousSessionOutcome = PreviousSessionOutcome.NONE,
    val previousSessionId: String? = null,
    val previousSessionTimestamp: Long? = null,
    val additionalMetadata: Map<String, String> = emptyMap(),
)
