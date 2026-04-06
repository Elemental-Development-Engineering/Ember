package com.elementaldevelopment.diagnostics

import android.app.Application
import com.elementaldevelopment.diagnostics.internal.DiagnosticsRuntimeInstaller
import com.elementaldevelopment.diagnostics.internal.RecoveredDiagnosticsRepository
import com.elementaldevelopment.diagnostics.internal.RecoveryState
import com.elementaldevelopment.diagnostics.model.DiagnosticEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiagnosticsRuntimeInstallerTest {

    @Test
    fun `registerOrReplaceLifecycleCallbacks unregisters previous callbacks for same app id`() {
        val application = CountingApplication()
        val firstRepository = NoOpRecoveredDiagnosticsRepository()
        val secondRepository = NoOpRecoveredDiagnosticsRepository()

        DiagnosticsRuntimeInstaller.registerOrReplaceLifecycleCallbacks(
            application = application,
            appId = "com.test.app",
            recoveredRepository = firstRepository,
        )
        DiagnosticsRuntimeInstaller.registerOrReplaceLifecycleCallbacks(
            application = application,
            appId = "com.test.app",
            recoveredRepository = secondRepository,
        )

        assertThat(application.registeredCount).isEqualTo(2)
        assertThat(application.unregisteredCount).isEqualTo(1)
    }
}

private class CountingApplication : Application() {
    var registeredCount = 0
        private set
    var unregisteredCount = 0
        private set

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        registeredCount += 1
    }

    override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        unregisteredCount += 1
    }
}

private class NoOpRecoveredDiagnosticsRepository : RecoveredDiagnosticsRepository {
    override fun recoverAndStartNewSession(sessionId: String, startedAt: Long): RecoveryState {
        return RecoveryState()
    }

    override fun appendToActiveSession(entry: DiagnosticEntry) = Unit

    override fun markSessionOpen() = Unit

    override fun markCleanExit(endedAt: Long) = Unit

    override fun markUncaughtException(endedAt: Long) = Unit

    override fun getRecoveredEntries(limit: Int?): List<DiagnosticEntry> = emptyList()

    override fun clearAll() = Unit
}
