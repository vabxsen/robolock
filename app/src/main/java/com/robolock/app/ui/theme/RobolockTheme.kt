package com.robolock.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.robolock.rules.ThemePreference

/**
 * Wraps Material 3 and layers Robolock's own tokens on top.
 *
 * Material's colour scheme is still populated so that stock components (switches, ripples) sit
 * correctly, but every surface the design specifies is drawn from [RobolockTheme.colors].
 */
@Composable
fun RobolockTheme(
    preference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors

    val materialScheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.borderStrong,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onButtonPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.borderStrong,
            error = colors.danger,
        )
    }

    // System bar icons must contrast with whichever palette is active. Doing this here rather than
    // in the manifest theme keeps it correct when the user switches theme at runtime, and avoids an
    // attribute that would raise the minimum API level.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalRobolockColors provides colors,
        LocalRobolockDimens provides RobolockDimens(),
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = RobolockTypography,
            content = content,
        )
    }
}

object RobolockTheme {
    val colors: RobolockColors
        @Composable @ReadOnlyComposable get() = LocalRobolockColors.current

    val dimens: RobolockDimens
        @Composable @ReadOnlyComposable get() = LocalRobolockDimens.current
}
