package com.elementaldevelopment.diagnostics

import android.content.Context
import com.elementaldevelopment.diagnostics.internal.FileBackedCrashStore
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.elementaldevelopment.diagnostics.model.DiagnosticLevel
import com.elementaldevelopment.diagnostics.model.PreviousSessionOutcome
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileBackedCrashStoreTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val appId = "com.test.filebacked"

    @Before
    fun setUp() {
        snapshotFile().delete()
    }

    @Test
    fun `clearAll removes recovered entries and clears active session entries`() {
        val baseNow = System.currentTimeMillis()
        val first = createStore(retentionAfterRecoveryMillis = 60_000L)
        first.recoverAndStartNewSession(sessionId = "session-1", startedAt = baseNow)
        first.appendToActiveSession(entry(message = "before restart"))

        val second = createStore(retentionAfterRecoveryMillis = 60_000L)
        val recoveryState = second.recoverAndStartNewSession(
            sessionId = "session-2",
            startedAt = baseNow + 1_000L,
        )

        assertThat(recoveryState.previousSessionOutcome)
            .isEqualTo(PreviousSessionOutcome.UNEXPECTED_TERMINATION)
        assertThat(second.getRecoveredEntries()).hasSize(1)

        second.clearAll()

        assertThat(second.getRecoveredEntries()).isEmpty()

        val third = createStore(retentionAfterRecoveryMillis = 60_000L)
        val thirdRecovery = third.recoverAndStartNewSession(
            sessionId = "session-3",
            startedAt = baseNow + 2_000L,
        )
        assertThat(thirdRecovery.recoveredEntries).isEmpty()
    }

    @Test
    fun `expired recovered entries are discarded on next startup`() {
        val baseNow = System.currentTimeMillis()
        val first = createStore(retentionAfterRecoveryMillis = 10L)
        first.recoverAndStartNewSession(sessionId = "session-1", startedAt = baseNow)
        first.appendToActiveSession(entry(message = "stale entry"))

        val second = createStore(retentionAfterRecoveryMillis = 10L)
        val recoveryState = second.recoverAndStartNewSession(
            sessionId = "session-2",
            startedAt = baseNow + 5L,
        )

        assertThat(recoveryState.previousSessionOutcome)
            .isEqualTo(PreviousSessionOutcome.UNEXPECTED_TERMINATION)
        assertThat(recoveryState.recoveredEntries).isNotEmpty()
        second.markCleanExit(endedAt = baseNow + 20L)

        val third = createStore(retentionAfterRecoveryMillis = 10L)
        val expiredRecovery = third.recoverAndStartNewSession(
            sessionId = "session-3",
            startedAt = baseNow + 50L,
        )

        assertThat(expiredRecovery.recoveredEntries).isEmpty()
        assertThat(expiredRecovery.previousSessionOutcome).isEqualTo(PreviousSessionOutcome.NONE)
    }

    @Test
    fun `clean exit does not recover previous session entries`() {
        val baseNow = System.currentTimeMillis()
        val first = createStore(retentionAfterRecoveryMillis = 60_000L)
        first.recoverAndStartNewSession(sessionId = "session-1", startedAt = baseNow)
        first.appendToActiveSession(entry(message = "backgrounded cleanly"))
        first.markCleanExit(endedAt = baseNow + 500L)

        val second = createStore(retentionAfterRecoveryMillis = 60_000L)
        val recoveryState = second.recoverAndStartNewSession(
            sessionId = "session-2",
            startedAt = baseNow + 1_000L,
        )

        assertThat(recoveryState.recoveredEntries).isEmpty()
        assertThat(recoveryState.previousSessionOutcome).isEqualTo(PreviousSessionOutcome.NONE)
    }

    private fun createStore(
        retentionAfterRecoveryMillis: Long,
    ): FileBackedCrashStore {
        return FileBackedCrashStore(
            context = context,
            appId = appId,
            maxPersistedEntries = 5,
            retentionAfterRecoveryMillis = retentionAfterRecoveryMillis,
        )
    }

    private fun entry(message: String): DiagnosticEntry {
        return DiagnosticEntry(
            id = 1L,
            timestamp = 1234L,
            level = DiagnosticLevel.INFO,
            tag = "Test",
            message = message,
        )
    }

    private fun snapshotFile(): File {
        val sanitizedAppId = appId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(File(context.filesDir, "ember"), "${sanitizedAppId}_diagnostics_snapshot.json")
    }
}
