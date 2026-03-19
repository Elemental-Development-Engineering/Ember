package com.elementaldevelopment.diagnostics.ui.actions

internal class DeferredClearOnResume(
    private val onClear: () -> Unit,
) {
    private var pendingClear = false

    fun arm() {
        pendingClear = true
    }

    fun clearIfPending(): Boolean {
        if (!pendingClear) return false

        pendingClear = false
        onClear()
        return true
    }
}
