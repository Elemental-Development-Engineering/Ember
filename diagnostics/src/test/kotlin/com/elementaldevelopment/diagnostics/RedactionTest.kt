package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.BaselineRedactor
import com.elementaldevelopment.diagnostics.internal.ComposedRedactor
import com.elementaldevelopment.diagnostics.redact.DiagnosticsRedactor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RedactionTest {

    @Test
    fun `baseline redactor collapses whitespace`() {
        val result = BaselineRedactor.redact("hello   world")
        assertThat(result).isEqualTo("hello world")
    }

    @Test
    fun `baseline redactor collapses newlines`() {
        val result = BaselineRedactor.redact("line1\nline2\n\nline3")
        assertThat(result).isEqualTo("line1 line2 line3")
    }

    @Test
    fun `baseline redactor handles tabs and mixed whitespace`() {
        val result = BaselineRedactor.redact("hello\t\t  world")
        assertThat(result).isEqualTo("hello world")
    }

    @Test
    fun `composed redactor applies baseline then app redactor`() {
        val appRedactor = DiagnosticsRedactor { it.replace("SECRET", "[REDACTED]") }
        val composed = ComposedRedactor(appRedactor)

        val result = composed.redact("value is\nSECRET here")
        assertThat(result).isEqualTo("value is [REDACTED] here")
    }

    @Test
    fun `composed redactor with no-op app redactor still normalizes whitespace`() {
        val noOp = DiagnosticsRedactor { it }
        val composed = ComposedRedactor(noOp)

        val result = composed.redact("hello\n\nworld")
        assertThat(result).isEqualTo("hello world")
    }
}
