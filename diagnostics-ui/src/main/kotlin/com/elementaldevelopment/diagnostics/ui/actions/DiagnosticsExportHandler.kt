package com.elementaldevelopment.diagnostics.ui.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.elementaldevelopment.diagnostics.api.Diagnostics

/**
 * Handles copy and share export actions for bug reports.
 *
 * After a successful export action, the diagnostics log is cleared
 * automatically for privacy. If an export is canceled or fails,
 * the log is not cleared.
 */
class DiagnosticsExportHandler(
    private val context: Context,
    private val diagnostics: Diagnostics,
) {
    /**
     * Copies the report text to the clipboard and clears the diagnostics log.
     *
     * @return true if the copy succeeded
     */
    fun copyToClipboard(reportText: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
     * The diagnostics log is cleared after the share intent is launched.
     * Note: Android's sharesheet does not provide a completion callback,
     * so the log is cleared optimistically when the intent launches successfully.
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
        context.startActivity(shareIntent)

        diagnostics.logger.clear()
    }
}
