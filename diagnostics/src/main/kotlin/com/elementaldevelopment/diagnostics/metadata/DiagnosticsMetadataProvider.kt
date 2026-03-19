package com.elementaldevelopment.diagnostics.metadata

import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata

/**
 * Collects technical environment metadata for bug reports.
 *
 * Metadata collection is conservative by default. Only technical diagnostics
 * needed for debugging are included. Never collects user content, account
 * identifiers, or unique hardware identifiers.
 */
interface DiagnosticsMetadataProvider {
    fun collect(): DiagnosticsMetadata
}
