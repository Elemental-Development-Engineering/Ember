package com.elementaldevelopment.diagnostics.ui.state

import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome

internal data class BugReportUiState(
    val userNote: String = "",
    val includeAppInfo: Boolean = true,
    val includeDeviceInfo: Boolean = true,
    val includeOsInfo: Boolean = true,
    val includeRecentLogs: Boolean = true,
    val includeRecoveredLogs: Boolean = false,
    val hasRecoveredDiagnostics: Boolean = false,
    val previousSessionOutcome: PreviousSessionOutcome = PreviousSessionOutcome.NONE,
    val isLoading: Boolean = false,
    val previewText: String = "",
    val errorMessage: String? = null,
)
