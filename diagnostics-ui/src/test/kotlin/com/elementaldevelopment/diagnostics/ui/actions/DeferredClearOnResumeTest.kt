package com.elementaldevelopment.diagnostics.ui.actions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeferredClearOnResumeTest {

    @Test
    fun `does nothing when not armed`() {
        var clearCount = 0
        val deferredClear = DeferredClearOnResume {
            clearCount += 1
        }

        val cleared = deferredClear.clearIfPending()

        assertThat(cleared).isFalse()
        assertThat(clearCount).isEqualTo(0)
    }

    @Test
    fun `clears once after being armed`() {
        var clearCount = 0
        val deferredClear = DeferredClearOnResume {
            clearCount += 1
        }

        deferredClear.arm()

        assertThat(deferredClear.clearIfPending()).isTrue()
        assertThat(deferredClear.clearIfPending()).isFalse()
        assertThat(clearCount).isEqualTo(1)
    }

    @Test
    fun `can be re-armed after clearing`() {
        var clearCount = 0
        val deferredClear = DeferredClearOnResume {
            clearCount += 1
        }

        deferredClear.arm()
        deferredClear.clearIfPending()
        deferredClear.arm()
        deferredClear.clearIfPending()

        assertThat(clearCount).isEqualTo(2)
    }
}
