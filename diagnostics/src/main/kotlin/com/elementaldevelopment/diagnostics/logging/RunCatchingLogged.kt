package com.elementaldevelopment.diagnostics.logging

/**
 * Executes [block] and logs any exception via [DiagnosticsLogger.logError].
 *
 * Returns [Result.success] with the block's return value, or [Result.failure]
 * if an exception was thrown (after logging it).
 */
inline fun <T> DiagnosticsLogger.runCatchingLogged(
    tag: String,
    actionName: String,
    block: () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        logError(tag, "$actionName failed", e)
        Result.failure(e)
    }
}
