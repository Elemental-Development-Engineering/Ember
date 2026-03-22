package com.elementaldevelopment.diagnostics.ui.actions

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.elementaldevelopment.diagnostics.api.Diagnostics

/**
 * Handles copy and share export actions for bug reports.
 *
 * After a successful export action, the diagnostics log is cleared
 * automatically for privacy. If an export is canceled or fails,
 * the log is not cleared.
 */
class DiagnosticsExportHandler(
    context: Context,
    private val diagnostics: Diagnostics,
) {
    private val appContext: Context = context.applicationContext
    private val application = appContext as? Application
    private val deferredClearOnResume = DeferredClearOnResume {
        diagnostics.logger.clear()
    }
    private var lifecycleCallbacksRegistered = false

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) {
            if (deferredClearOnResume.clearIfPending()) {
                unregisterLifecycleCallbacks()
            }
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    /**
     * Copies the report text to the clipboard and clears the diagnostics log.
     *
     * @return true if the copy succeeded
     */
    fun copyToClipboard(reportText: String): Boolean {
        return try {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Bug Report", reportText)
            clipboard.setPrimaryClip(clip)
            diagnostics.logger.clear()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches the Android sharesheet with the report text.
     *
     * The diagnostics log is cleared when the app next resumes after the
     * chooser is launched. This keeps the data around while the user is in
     * the share flow, but still clears it as soon as they return to the app.
     */
    fun shareReport(reportText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"

            diagnostics.config.supportEmail?.let { email ->
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            }

            putExtra(Intent.EXTRA_SUBJECT, "Bug Report: ${diagnostics.config.appName}")
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share Bug Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(shareIntent)
        armDeferredClear()
    }

    private fun armDeferredClear() {
        val app = application ?: return

        deferredClearOnResume.arm()
        if (!lifecycleCallbacksRegistered) {
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            lifecycleCallbacksRegistered = true
        }
    }

    private fun unregisterLifecycleCallbacks() {
        val app = application ?: return
        if (lifecycleCallbacksRegistered) {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            lifecycleCallbacksRegistered = false
        }
    }
}
