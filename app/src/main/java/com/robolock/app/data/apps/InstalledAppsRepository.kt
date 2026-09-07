package com.robolock.app.data.apps

import android.content.Context
import android.content.Intent
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
 * Resolves the launchable apps on this device against the built-in registry.
 *
 * The manifest declares this launcher query explicitly. That gives the user a real choice of
 * apps to protect without broad package visibility, and we never monitor an app until its rule
 * has been explicitly enabled.
 */
class InstalledAppsRepository(
    private val context: Context,
    private val registry: SupportedAppRegistry,
) {
    fun states(): List<InstalledAppState> {
        val pm = context.packageManager
        val builtIns = registry.apps.associateBy { it.packageName }
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return pm.queryIntentActivities(launchIntent, 0)
            .asSequence()
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@map null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName || packageName in NEVER_WATCH) return@map null

                val builtIn = builtIns[packageName]
                val label = resolveInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
                val app = builtIn ?: SupportedApp(
                    packageName = packageName,
                    displayName = label,
                    brandColor = DEFAULT_BRAND_COLOR,
                    controlTagline = "Protect this app when you choose.",
                )
                InstalledAppState(
                    app = app,
                    installed = true,
                    icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull(),
                )
            }
            .filterNotNull()
            .distinctBy { it.app.packageName }
            .sortedBy { it.app.displayName.lowercase() }
            .toList()
    }

    fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    fun label(packageName: String): String? = runCatching {
        val pm: PackageManager = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    private companion object {
        const val DEFAULT_BRAND_COLOR = 0xFF64748B

        // These destinations must always remain reachable to revoke permissions or recover the
        // device. OverlayController enforces the same rule at display time.
        val NEVER_WATCH = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )
    }
}
