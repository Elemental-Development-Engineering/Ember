package com.elementaldevelopment.diagnostics.model

/**
 * Describes how the previous recorded app session ended, if known.
 */
enum class PreviousSessionOutcome {
    NONE,
    UNCUGHT_EXCEPTION,
    UNEXPECTED_TERMINATION,
}
