package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.model.DiagnosticLevel

/**
 * Captures one final sanitized crash entry before delegating to the platform's
 * original uncaught exception handler.
 */
internal class CrashCaptureUncaughtExceptionHandler(
    private val entryFactory: EntryFactory,
    private val recoveredDiagnosticsRepository: RecoveredDiagnosticsRepository,
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val crashEntry = entryFactory.create(
                level = DiagnosticLevel.ERROR,
                tag = "Crash",
                message = "Uncaught exception terminated the app",
                throwable = throwable,
                attributes = mapOf(
                    "source" to "uncaught_exception",
                    "thread" to thread.name,
                ),
            )
            recoveredDiagnosticsRepository.appendToActiveSession(crashEntry)
            recoveredDiagnosticsRepository.markUncaughtException(System.currentTimeMillis())
        }

        delegate?.uncaughtException(thread, throwable)
    }
}
