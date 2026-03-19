package com.elementaldevelopment.diagnostics

import com.elementaldevelopment.diagnostics.model.BugReportRequest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BugReportRequestTest {

    @Test
    fun `allows zero max entries`() {
        val request = BugReportRequest(maxEntries = 0)

        assertThat(request.maxEntries).isEqualTo(0)
    }

    @Test
    fun `rejects negative max entries`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            BugReportRequest(maxEntries = -1)
        }

        assertThat(error).hasMessageThat().contains("maxEntries must be >= 0")
    }
}
