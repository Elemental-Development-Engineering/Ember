package com.elementaldevelopment.diagnostics.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elementaldevelopment.diagnostics.api.Diagnostics
import com.elementaldevelopment.diagnostics.config.CrashPersistenceConfig
import com.elementaldevelopment.diagnostics.model.BugReportRequest

internal class BugReportStateHolder(
    private val diagnostics: Diagnostics,
) {
    private val defaultIncludeRecoveredLogs = when (val crashPersistence = diagnostics.config.crashPersistence) {
        CrashPersistenceConfig.Disabled -> false
        is CrashPersistenceConfig.Enabled -> crashPersistence.includeRecoveredEntriesByDefault
    }

    var state by mutableStateOf(
        BugReportUiState(
            includeRecoveredLogs = defaultIncludeRecoveredLogs,
        )
    )
        private set

    init {
        refreshPreview()
    }

    fun updateUserNote(note: String) {
        state = state.copy(userNote = note)
        refreshPreview()
    }

    fun toggleAppInfo(include: Boolean) {
        state = state.copy(includeAppInfo = include)
        refreshPreview()
    }

    fun toggleDeviceInfo(include: Boolean) {
        state = state.copy(includeDeviceInfo = include)
        refreshPreview()
    }

    fun toggleOsInfo(include: Boolean) {
        state = state.copy(includeOsInfo = include)
        refreshPreview()
    }

    fun toggleRecentLogs(include: Boolean) {
        state = state.copy(includeRecentLogs = include)
        refreshPreview()
    }

    fun toggleRecoveredLogs(include: Boolean) {
        state = state.copy(includeRecoveredLogs = include)
        refreshPreview()
    }

    fun getExportText(): String {
        val request = buildRequest()
        val report = diagnostics.reportBuilder.build(request)
        return diagnostics.exporter.export(report)
    }

    private fun refreshPreview() {
        try {
            state = state.copy(isLoading = true, errorMessage = null)
            val request = buildRequest()
            val report = diagnostics.reportBuilder.build(request)
            val text = diagnostics.exporter.export(report)
            state = state.copy(
                previewText = text,
                hasRecoveredDiagnostics = report.metadata.previousSessionOutcome !=
                    com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome.NONE,
                previousSessionOutcome = report.metadata.previousSessionOutcome,
                isLoading = false,
            )
        } catch (e: Exception) {
            state = state.copy(
                errorMessage = "Failed to generate report preview",
                isLoading = false,
            )
        }
    }

    private fun buildRequest() = BugReportRequest(
        userNote = state.userNote.takeIf { it.isNotBlank() },
        includeAppInfo = state.includeAppInfo,
        includeDeviceInfo = state.includeDeviceInfo,
        includeOsInfo = state.includeOsInfo,
        includeRecentLogs = state.includeRecentLogs,
        includeRecoveredLogs = state.includeRecoveredLogs,
    )
}
