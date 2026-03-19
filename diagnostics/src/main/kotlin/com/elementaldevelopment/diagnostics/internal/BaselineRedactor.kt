package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor

/**
 * Library-provided baseline redactor that normalizes content before storage.
 *
 * Applied before the app-provided redactor in the composition chain.
 */
internal object BaselineRedactor : DiagnosticsRedactor {

    override fun redact(input: String): String {
        return input
            .collapseWhitespace()
            .collapseNewlines()
    }
}

private fun String.collapseWhitespace(): String {
    return replace(Regex("\\s+"), " ")
}

private fun String.collapseNewlines(): String {
    return replace(Regex("[\\r\\n]+"), " ")
}
