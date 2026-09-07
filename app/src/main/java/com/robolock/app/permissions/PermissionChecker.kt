package com.robolock.app.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/** The three permissions Robolock asks for, and nothing else. Accessibility is not among them. */
enum class RobolockPermission(val title: String, val subtitle: String, val actionLabel: String) {
    USAGE_ACCESS(
        title = "Usage Access",
        subtitle = "Detect when selected apps are being used",
        actionLabel = "Enable",
    ),
    OVERLAY(
        title = "Display Over Other Apps",
        subtitle = "Show focus and blocking screens",
        actionLabel = "Allow",
    ),
    BATTERY(
        title = "Battery Optimization",
        subtitle = "Keep Robolock running reliably",
        actionLabel = "Configure",
    ),
}

data class PermissionState(
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val batteryUnrestricted: Boolean = false,
) {
    /** Usage access is the one Robolock genuinely cannot work without. */
    val canMonitor: Boolean get() = usageAccess

    /** Overlays make interventions possible; without them we degrade to a notification. */
    val canShowOverlay: Boolean get() = overlay

    val allGranted: Boolean get() = usageAccess && overlay && batteryUnrestricted

    fun isGranted(permission: RobolockPermission): Boolean = when (permission) {
        RobolockPermission.USAGE_ACCESS -> usageAccess
        RobolockPermission.OVERLAY -> overlay
        RobolockPermission.BATTERY -> batteryUnrestricted
    }
}

/**
 * Reads real permission state from the system, every time.
 *
 * Nothing is cached or assumed: a permission the user revoked in Settings must show as revoked
 * the moment they come back, and the app must never present itself as protecting something it
 * cannot see.
 */
class PermissionChecker(private val context: Context) {

    fun state(): PermissionState = PermissionState(
        usageAccess = hasUsageAccess(),
        overlay = hasOverlay(),
        batteryUnrestricted = isIgnoringBatteryOptimizations(),
    )

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            // DEFAULT means "fall back to the manifest permission", so it must be checked, not assumed.
            AppOpsManager.MODE_DEFAULT -> context.checkSelfPermission(
                android.Manifest.permission.PACKAGE_USAGE_STATS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }

    fun hasOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** The settings screen for a given permission. Each opens the real destination, not a guess. */
    fun intentFor(permission: RobolockPermission): Intent = when (permission) {
        RobolockPermission.USAGE_ACCESS ->
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

        RobolockPermission.OVERLAY ->
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )

        // Deliberately the list screen rather than ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
        // the direct request dialog is policy-restricted, and this always resolves.
        RobolockPermission.BATTERY ->
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}
