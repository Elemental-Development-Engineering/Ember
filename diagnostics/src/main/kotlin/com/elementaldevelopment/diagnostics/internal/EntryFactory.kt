package com.elementaldevelopment.diagnostics.internal

import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import java.util.concurrent.atomic.AtomicLong

/**
 * Single internal factory responsible for creating all [DiagnosticEntry] instances.
 *
 * All entry creation MUST go through this factory to ensure trimming and redaction
 * are applied before the entry can reach the store. No direct write path should
 * bypass this helper.
 */
internal class EntryFactory(
    private val redactor: DiagnosticsRedactor,
) {
    private val idCounter = AtomicLong(0)

    fun create(
        level: DiagnosticLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        attributes: Map<String, String> = emptyMap(),
    ): DiagnosticEntry {
        val trimmedTag = trimToMaxLength(tag, Limits.MAX_TAG_LENGTH)
        val trimmedMessage = trimToMaxLength(message, Limits.MAX_MESSAGE_LENGTH)
        val sanitizedMessage = redactor.redact(trimmedMessage)
        val sanitizedTag = redactor.redact(trimmedTag)

        val throwableSummary = throwable?.let {
            ThrowableSummarizer.summarize(it, redactor)
        }

        val sanitizedAttributes = trimAttributes(attributes).mapValues { (_, value) ->
            redactor.redact(value)
        }.mapKeys { (key, _) ->
            redactor.redact(key)
        }

        return DiagnosticEntry(
            id = idCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = sanitizedTag,
            message = sanitizedMessage,
            throwableSummary = throwableSummary,
            attributes = sanitizedAttributes,
        )
    }
}
