package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor

/**
 * Composes the library baseline redactor with the app-provided redactor.
 *
 * Order: baseline first, then app redactor.
 */
internal class ComposedRedactor(
    private val appRedactor: DiagnosticsRedactor,
) : DiagnosticsRedactor {

    override fun redact(input: String): String {
        return appRedactor.redact(BaselineRedactor.redact(input))
    }
}
