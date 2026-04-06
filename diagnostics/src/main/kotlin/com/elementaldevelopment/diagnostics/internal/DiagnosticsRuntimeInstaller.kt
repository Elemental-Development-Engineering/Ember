package com.elementaldevelopment.diagnostics.internal

import android.app.Application
import java.util.WeakHashMap

internal object DiagnosticsRuntimeInstaller {
    private val lock = Any()
    private val lifecycleCallbacksByApplication =
        WeakHashMap<Application, MutableMap<String, DiagnosticsSessionLifecycleCallbacks>>()

    fun registerOrReplaceLifecycleCallbacks(
        application: Application,
        appId: String,
        recoveredRepository: RecoveredDiagnosticsRepository,
    ) = synchronized(lock) {
        val callbacksByAppId = lifecycleCallbacksByApplication.getOrPut(application) { mutableMapOf() }
        callbacksByAppId.remove(appId)?.let(application::unregisterActivityLifecycleCallbacks)

        val callbacks = DiagnosticsSessionLifecycleCallbacks(recoveredRepository)
        application.registerActivityLifecycleCallbacks(callbacks)
        callbacksByAppId[appId] = callbacks
    }

    fun installOrReplaceUncaughtExceptionHandler(
        appId: String,
        entryFactory: EntryFactory,
        recoveredRepository: RecoveredDiagnosticsRepository,
    ) = synchronized(lock) {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        val delegate = if (current is CrashCaptureUncaughtExceptionHandler && current.appId == appId) {
            current.delegate
        } else {
            current
        }

        Thread.setDefaultUncaughtExceptionHandler(
            CrashCaptureUncaughtExceptionHandler(
                appId = appId,
                entryFactory = entryFactory,
                recoveredDiagnosticsRepository = recoveredRepository,
                delegate = delegate,
            )
        )
    }
}
