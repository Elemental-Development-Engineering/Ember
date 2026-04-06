package com.elementaldevelopment.diagnostics.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Approximates when the app is actively in use so unexpected foreground
 * terminations can be distinguished from sessions that backgrounded cleanly.
 */
internal class DiagnosticsSessionLifecycleCallbacks(
    private val recoveredRepository: RecoveredDiagnosticsRepository,
) : Application.ActivityLifecycleCallbacks {
    private var startedActivityCount = 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount += 1
        if (startedActivityCount == 1) {
            recoveredRepository.markSessionOpen()
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (startedActivityCount == 0) {
            recoveredRepository.markCleanExit(System.currentTimeMillis())
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
