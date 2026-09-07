package com.robolock.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"

    const val HOME = "home"
    const val STATS = "stats"
    const val FOCUS = "focus"
    const val SETTINGS = "settings"

    const val APP_CONTROLS = "app_controls"
    const val DISTRACTIONS = "distractions"
    const val INSIGHTS = "insights"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val DETECTION_INFO = "detection_info"
}

/** The four destinations in the bottom bar, in the order the reference shows them. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    STATS(Routes.STATS, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    FOCUS(Routes.FOCUS, "Focus", Icons.Outlined.Schedule, Icons.Outlined.Schedule),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Outlined.Settings),
    ;

    companion object {
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.firstOrNull { it.route == route }
    }
}
