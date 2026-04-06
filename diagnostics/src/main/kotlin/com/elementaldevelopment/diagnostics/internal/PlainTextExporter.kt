package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.export.DiagnosticsExporter
import com.elementaldevelopment.diagnostics.model.BugReport
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Renders a [BugReport] into human-readable plain text.
 *
 * Uses a stable section order and omits empty sections.
 * Output is compact, line-oriented, and easy to inspect before sharing.
 */
internal class PlainTextExporter : DiagnosticsExporter {

    override fun export(report: BugReport): String {
        val sb = StringBuilder()

        sb.appendLine("Elemental Diagnostics Report")
        sb.appendLine("Format Version: ${report.formatVersion}")
        sb.appendLine("Library Version: ${report.metadata.libraryVersion}")

        appendAppSection(sb, report.metadata)
        appendEnvironmentSection(sb, report.metadata)
        appendUserNoteSection(sb, report.userNote)
        appendEntriesSection(sb, report.entries)
        appendRecoveredEntriesSection(sb, report.recoveredEntries)

        return sb.toString().trimEnd()
    }

    private fun appendAppSection(sb: StringBuilder, metadata: DiagnosticsMetadata) {
        val hasAppName = metadata.appName.isNotBlank()
        val hasAppId = metadata.appId.isNotBlank()
        val hasVersion = metadata.versionName.isNotBlank() || metadata.versionCode != 0L

        if (!hasAppName && !hasAppId && !hasVersion) return

        sb.appendLine()
        sb.appendLine("App")
        if (hasAppName) {
            sb.appendLine("- Name: ${metadata.appName}")
        }
        if (hasAppId) {
            sb.appendLine("- App ID: ${metadata.appId}")
        }
        if (hasVersion) {
            sb.appendLine("- Version: ${metadata.versionName} (${metadata.versionCode})")
        }
    }

    private fun appendEnvironmentSection(sb: StringBuilder, metadata: DiagnosticsMetadata) {
        val hasDevice = metadata.deviceManufacturer.isNotEmpty() || metadata.deviceModel.isNotEmpty()
        val hasOs = metadata.androidVersion.isNotEmpty() && metadata.apiLevel > 0
        val hasEnvironmentDetails = hasDevice ||
            hasOs ||
            metadata.additionalMetadata.isNotEmpty() ||
            metadata.sessionId.isNotBlank() ||
            metadata.generatedAt > 0L

        if (!hasEnvironmentDetails) return

        sb.appendLine()
        sb.appendLine("Environment")
        if (hasOs) {
            sb.appendLine("- Android: ${metadata.androidVersion} / API ${metadata.apiLevel}")
        }
        if (hasDevice) {
            val device = listOfNotNull(
                metadata.deviceManufacturer.takeIf { it.isNotEmpty() },
                metadata.deviceModel.takeIf { it.isNotEmpty() },
            ).joinToString(" ")
            sb.appendLine("- Device: $device")
        }
        sb.appendLine("- Time: ${formatTimestamp(metadata.generatedAt)}")
        sb.appendLine("- Session: ${metadata.sessionId}")
        if (metadata.previousSessionOutcome != PreviousSessionOutcome.NONE) {
            sb.appendLine("- Previous Session: ${metadata.previousSessionOutcome.toDisplayName()}")
            metadata.previousSessionId?.let { previousSessionId ->
                sb.appendLine("- Previous Session ID: $previousSessionId")
            }
            metadata.previousSessionTimestamp?.let { previousTimestamp ->
                sb.appendLine("- Previous Session Time: ${formatTimestamp(previousTimestamp)}")
            }
        }

        if (metadata.additionalMetadata.isNotEmpty()) {
            metadata.additionalMetadata.forEach { (key, value) ->
                sb.appendLine("- $key: $value")
            }
        }
    }

    private fun appendUserNoteSection(sb: StringBuilder, userNote: String?) {
        if (userNote.isNullOrBlank()) return

        sb.appendLine()
        sb.appendLine("User Note")
        sb.appendLine(userNote)
    }

    private fun appendEntriesSection(sb: StringBuilder, entries: List<DiagnosticEntry>) {
        if (entries.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Recent Diagnostics")
        appendEntryLines(sb, entries)
    }

    private fun appendRecoveredEntriesSection(sb: StringBuilder, entries: List<DiagnosticEntry>) {
        if (entries.isEmpty()) return

        sb.appendLine()
        sb.appendLine("Recovered Diagnostics From Previous Launch")
        appendEntryLines(sb, entries)
    }

    private fun appendEntryLines(sb: StringBuilder, entries: List<DiagnosticEntry>) {
        entries.forEach { entry ->
            val timestamp = formatTimestamp(entry.timestamp)
            val line = "[$timestamp] ${entry.level} ${entry.tag}: ${entry.message}"
            sb.appendLine(line)

            entry.throwableSummary?.let { summary ->
                sb.appendLine("  Exception: $summary")
            }

            if (entry.attributes.isNotEmpty()) {
                entry.attributes.forEach { (key, value) ->
                    sb.appendLine("  $key=$value")
                }
            }
        }
    }

    companion object {
        private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

        private fun formatTimestamp(epochMillis: Long): String {
            return timestampFormat.format(Date(epochMillis))
        }
    }
}

private fun PreviousSessionOutcome.toDisplayName(): String {
    return when (this) {
        PreviousSessionOutcome.NONE -> "none"
        PreviousSessionOutcome.UNCUGHT_EXCEPTION -> "uncaught exception"
        PreviousSessionOutcome.UNEXPECTED_TERMINATION -> "unexpected termination"
    }
}
