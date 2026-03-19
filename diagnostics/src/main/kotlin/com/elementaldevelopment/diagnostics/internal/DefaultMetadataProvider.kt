package com.elementaldevelopment.diagnostics.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.elementaldevelopment.diagnostics.config.DiagnosticsConfig
import com.elementaldevelopment.diagnostics.metadata.DiagnosticsMetadataProvider
import com.elementaldevelopment.diagnostics.model.DiagnosticsMetadata

/**
 * Default metadata provider that collects conservative device and app information.
 *
 * Only technical diagnostics needed for debugging are included.
 * This provider never collects:
 * - Android ID, Advertising ID, App Set ID
 * - IMEI, MEID, serial numbers
 * - Unique hardware identifiers
 * - Contacts, clipboard, precise location
 * - Installed apps list or user content
 */
internal class DefaultMetadataProvider(
    private val context: Context,
    private val config: DiagnosticsConfig,
    private val sessionId: String,
) : DiagnosticsMetadataProvider {

    override fun collect(): DiagnosticsMetadata {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 0L
        }

        return DiagnosticsMetadata(
            appName = config.appName,
            appId = config.appId,
            versionName = versionName,
            versionCode = versionCode,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            deviceManufacturer = if (config.includeDeviceModelByDefault) Build.MANUFACTURER else "",
            deviceModel = if (config.includeDeviceModelByDefault) Build.MODEL else "",
            generatedAt = System.currentTimeMillis(),
            libraryVersion = EmberVersion.VERSION,
            sessionId = sessionId,
            additionalMetadata = config.additionalMetadata(),
        )
    }
}
