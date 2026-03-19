package com.elementaldevelopment.diagnostics.ui.state

internal data class BugReportUiState(
    val userNote: String = "",
    val includeAppInfo: Boolean = true,
    val includeDeviceInfo: Boolean = true,
    val includeOsInfo: Boolean = true,
    val includeRecentLogs: Boolean = true,
    val isLoading: Boolean = false,
    val previewText: String = "",
    val errorMessage: String? = null,
)
