package com.elementaldevelopment.diagnostics.api

import android.content.Context
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.internal.ComposedRedactor
import com.elementaldevelopment.diagnostics.internal.DefaultBugReportBuilder
import com.elementaldevelopment.diagnostics.internal.DefaultDiagnostics
import com.elementaldevelopment.diagnostics.internal.DefaultDiagnosticsLogger
import com.elementaldevelopment.diagnostics.internal.DefaultMetadataProvider
import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.InMemoryDiagnosticsStore
import com.elementaldevelopment.diagnostics.internal.PlainTextExporter
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import java.util.UUID

/**
 * Creates a fully-wired [Diagnostics] instance for the given app configuration.
 *
 * This is the primary initialization entry point. Call once during app startup.
 *
 * Automatically logs a session-start entry and generates an ephemeral session ID
 * that exists only for the current app process.
 *
 * Example usage:
 * ```kotlin
 * val diagnostics = Diagnostics.create(
 *     context = applicationContext,
 *     config = myAppConfig,
 * )
 * ```
 */
fun Diagnostics.Companion.create(
    context: Context,
    config: DiagnosticsConfig,
): Diagnostics {
    val sessionId = UUID.randomUUID().toString()
    val redactor = ComposedRedactor(config.redactor)
    val entryFactory = EntryFactory(redactor)
    val store = InMemoryDiagnosticsStore(config.maxStoredEntries)
    val logger = DefaultDiagnosticsLogger(entryFactory, store)
    val metadataProvider = DefaultMetadataProvider(
        context = context.applicationContext,
        config = config,
        sessionId = sessionId,
    )
    val reportBuilder = DefaultBugReportBuilder(metadataProvider, store, redactor)
    val exporter = PlainTextExporter()

    // Auto-log session start
    logger.log(DiagnosticLevel.INFO, "System", "Diagnostics initialized")

    return DefaultDiagnostics(
        logger = logger,
        reportBuilder = reportBuilder,
        exporter = exporter,
        config = config,
    )
}
