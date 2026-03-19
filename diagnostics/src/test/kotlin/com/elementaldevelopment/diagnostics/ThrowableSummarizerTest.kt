package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.Limits
import com.elementaldevelopment.diagnostics.internal.ThrowableSummarizer
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThrowableSummarizerTest {

    private val noOpRedactor = DiagnosticsRedactor { it }

    @Test
    fun `summarizes exception with class name and message`() {
        val ex = IllegalStateException("something went wrong")
        val result = ThrowableSummarizer.summarize(ex, noOpRedactor)
        assertThat(result).isEqualTo("IllegalStateException: something went wrong")
    }

    @Test
    fun `summarizes exception without message`() {
        val ex = NullPointerException()
        val result = ThrowableSummarizer.summarize(ex, noOpRedactor)
        assertThat(result).isEqualTo("NullPointerException")
    }

    @Test
    fun `trims long throwable messages`() {
        val longMessage = "x".repeat(500)
        val ex = RuntimeException(longMessage)
        val result = ThrowableSummarizer.summarize(ex, noOpRedactor)
        assertThat(result.length).isEqualTo(Limits.MAX_THROWABLE_MESSAGE_LENGTH)
        assertThat(result).endsWith(Limits.TRIMMED_SUFFIX)
    }

    @Test
    fun `applies redaction to throwable message`() {
        val redactor = DiagnosticsRedactor { it.replace("SECRET", "[REDACTED]") }
        val ex = RuntimeException("password is SECRET")
        val result = ThrowableSummarizer.summarize(ex, redactor)
        assertThat(result).isEqualTo("RuntimeException: password is [REDACTED]")
        assertThat(result).doesNotContain("SECRET")
    }

    @Test
    fun `collapses multiline throwable messages`() {
        val ex = RuntimeException("line1\nline2\nline3")
        val redactor = DiagnosticsRedactor { it.replace(Regex("\\s+"), " ") }
        val result = ThrowableSummarizer.summarize(ex, redactor)
        assertThat(result).doesNotContain("\n")
    }

    @Test
    fun `does not include cause chain`() {
        val cause = IllegalArgumentException("root cause")
        val ex = RuntimeException("wrapper", cause)
        val result = ThrowableSummarizer.summarize(ex, noOpRedactor)
        assertThat(result).doesNotContain("root cause")
        assertThat(result).doesNotContain("IllegalArgumentException")
    }
}
