package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.internal.Limits
import com.elementaldevelopment.diagnostics.internal.trimAttributes
import com.elementaldevelopment.diagnostics.internal.trimToMaxLength
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimmingTest {

    @Test
    fun `short string is not trimmed`() {
        val input = "hello"
        assertThat(trimToMaxLength(input, 240)).isEqualTo("hello")
    }

    @Test
    fun `string at max length is not trimmed`() {
        val input = "a".repeat(240)
        assertThat(trimToMaxLength(input, 240)).isEqualTo(input)
    }

    @Test
    fun `string over max length is trimmed with suffix`() {
        val input = "a".repeat(300)
        val result = trimToMaxLength(input, 240)
        assertThat(result.length).isEqualTo(240)
        assertThat(result).endsWith(Limits.TRIMMED_SUFFIX)
    }

    @Test
    fun `empty string is not trimmed`() {
        assertThat(trimToMaxLength("", 240)).isEmpty()
    }

    @Test
    fun `attributes are limited to max count`() {
        val attrs = (1..20).associate { "key$it" to "value$it" }
        val result = trimAttributes(attrs)
        assertThat(result).hasSize(Limits.MAX_ATTRIBUTES_PER_ENTRY)
    }

    @Test
    fun `attribute keys are trimmed`() {
        val longKey = "k".repeat(100)
        val result = trimAttributes(mapOf(longKey to "value"))
        val key = result.keys.first()
        assertThat(key.length).isEqualTo(Limits.MAX_ATTRIBUTE_KEY_LENGTH)
        assertThat(key).endsWith(Limits.TRIMMED_SUFFIX)
    }

    @Test
    fun `attribute values are trimmed`() {
        val longValue = "v".repeat(200)
        val result = trimAttributes(mapOf("key" to longValue))
        val value = result.values.first()
        assertThat(value.length).isEqualTo(Limits.MAX_ATTRIBUTE_VALUE_LENGTH)
        assertThat(value).endsWith(Limits.TRIMMED_SUFFIX)
    }
}
