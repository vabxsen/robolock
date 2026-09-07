package com.robolock.app.data.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.robolock.rules.SupportedApp
import com.robolock.rules.SupportedAppRegistry

/**
 * A supported app paired with whether it is actually on this device.
 *
 * [icon] is null when the app is absent, which the UI renders as a branded fallback tile rather
 * than pretending the app is there.
 */
data class InstalledAppState(
    val app: SupportedApp,
    val installed: Boolean,
    val icon: Drawable?,
)

/**
 * Resolves the supported-app registry against what is really installed.
 *
 * Robolock only ever asks the package manager about apps in its own registry; it does not
 * enumerate everything on the device.
 */
class InstalledAppsRepository(
    private val context: Context,
    private val registry: SupportedAppRegistry,
) {
    fun states(): List<InstalledAppState> = registry.apps.map { app ->
        val info = runCatching {
            context.packageManager.getApplicationInfo(app.packageName, 0)
        }.getOrNull()

        InstalledAppState(
            app = app,
            installed = info != null,
            icon = info?.let {
                runCatching { context.packageManager.getApplicationIcon(it) }.getOrNull()
            },
        )
    }

    fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    fun label(packageName: String): String? = runCatching {
        val pm: PackageManager = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()
}
