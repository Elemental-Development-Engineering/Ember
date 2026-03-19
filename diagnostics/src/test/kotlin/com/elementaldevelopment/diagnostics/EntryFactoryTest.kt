package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.EntryFactory
import com.elementaldevelopment.diagnostics.internal.Limits
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntryFactoryTest {

    private val noOpRedactor = DiagnosticsRedactor { it }

    @Test
    fun `creates entry with sanitized fields`() {
        val redactor = DiagnosticsRedactor { it.replace("SECRET", "[REDACTED]") }
        val factory = EntryFactory(redactor)

        val entry = factory.create(
            level = DiagnosticLevel.INFO,
            tag = "test",
            message = "value is SECRET",
        )

        assertThat(entry.message).isEqualTo("value is [REDACTED]")
        assertThat(entry.message).doesNotContain("SECRET")
    }

    @Test
    fun `trims long tags`() {
        val factory = EntryFactory(noOpRedactor)
        val longTag = "t".repeat(100)
        val entry = factory.create(DiagnosticLevel.INFO, longTag, "msg")

        assertThat(entry.tag.length).isEqualTo(Limits.MAX_TAG_LENGTH)
    }

    @Test
    fun `trims long messages`() {
        val factory = EntryFactory(noOpRedactor)
        val longMsg = "m".repeat(500)
        val entry = factory.create(DiagnosticLevel.INFO, "tag", longMsg)

        assertThat(entry.message.length).isEqualTo(Limits.MAX_MESSAGE_LENGTH)
    }

    @Test
    fun `assigns incrementing IDs`() {
        val factory = EntryFactory(noOpRedactor)
        val e1 = factory.create(DiagnosticLevel.INFO, "tag", "msg1")
        val e2 = factory.create(DiagnosticLevel.INFO, "tag", "msg2")

        assertThat(e2.id).isGreaterThan(e1.id)
    }

    @Test
    fun `includes throwable summary when throwable provided`() {
        val factory = EntryFactory(noOpRedactor)
        val entry = factory.create(
            level = DiagnosticLevel.ERROR,
            tag = "test",
            message = "failed",
            throwable = IllegalStateException("bad state"),
        )

        assertThat(entry.throwableSummary).contains("IllegalStateException")
        assertThat(entry.throwableSummary).contains("bad state")
    }

    @Test
    fun `no throwable summary when throwable not provided`() {
        val factory = EntryFactory(noOpRedactor)
        val entry = factory.create(DiagnosticLevel.INFO, "tag", "msg")

        assertThat(entry.throwableSummary).isNull()
    }

    @Test
    fun `sanitizes attributes`() {
        val redactor = DiagnosticsRedactor { it.replace("SECRET", "[REDACTED]") }
        val factory = EntryFactory(redactor)

        val entry = factory.create(
            level = DiagnosticLevel.INFO,
            tag = "tag",
            message = "msg",
            attributes = mapOf("key" to "value SECRET"),
        )

        assertThat(entry.attributes["key"]).isEqualTo("value [REDACTED]")
    }

    @Test
    fun `limits attribute count`() {
        val factory = EntryFactory(noOpRedactor)
        val attrs = (1..20).associate { "key$it" to "value$it" }

        val entry = factory.create(
            level = DiagnosticLevel.INFO,
            tag = "tag",
            message = "msg",
            attributes = attrs,
        )

        assertThat(entry.attributes).hasSize(Limits.MAX_ATTRIBUTES_PER_ENTRY)
    }
}
