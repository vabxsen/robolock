package com.robolock.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Robolock's palette.
 *
 * Every colour in the app comes from here. Material 3 supplies the component framework, but its
 * default tonal palettes are far lighter and greyer than the reference design, so the app reads its
 * colours from this token set instead of from MaterialTheme.colorScheme.
 *
 * The reference is a deep navy-black ground with slightly lifted navy cards, and a small family of
 * saturated accents that each own a meaning: cyan for the live "today" figures, cobalt for
 * controls, violet/pink for the brand gradients, amber for a highlighted day, green for gains.
 */
@Immutable
data class RobolockColors(
    val background: Color,
    /** Top stop of the page's very subtle vertical wash. */
    val backgroundTop: Color,
    /** Standard card fill. */
    val surface: Color,
    /** Slightly lifted fill for nested elements: icon tiles, segmented tracks. */
    val surfaceElevated: Color,
    /** Faint cool highlight at the upper edge of a card. */
    val surfaceHighlight: Color,
    /** Hairline stroke that gives cards their edge on a near-black ground. */
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Cobalt: switches, the selected segment, statistics bars. */
    val accent: Color,
    /** Second stop for cobalt gradients. */
    val accentAlt: Color,
    /** Cyan: the ring and today's count. */
    val accentCool: Color,
    /** Sky blue: the selected tab and other "live" affordances. */
    val accentSky: Color,
    /** Amber: the highlighted day and the streak card. */
    val accentWarm: Color,
    val accentViolet: Color,
    val accentPink: Color,
    /** Quiet cobalt wash behind informational blocks. */
    val accentMuted: Color,
    val success: Color,
    val successSurface: Color,
    val danger: Color,
    /** Filled pill button: white on dark, near-black on light. */
    val buttonPrimary: Color,
    val onButtonPrimary: Color,
    val switchTrackOff: Color,
    val switchThumbOff: Color,
    val chartTrack: Color,
    /** Unselected radio ring. */
    val controlOutline: Color,
    val isDark: Boolean,
)

val DarkColors = RobolockColors(
    background = Color(0xFF050A11),
    backgroundTop = Color(0xFF0A1420),
    surface = Color(0xFF101A26),
    surfaceElevated = Color(0xFF18242F),
    surfaceHighlight = Color(0xFF1B2A3C),
    border = Color(0xFF1D2A3A),
    borderStrong = Color(0xFF2B3B4E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9BAABB),
    textTertiary = Color(0xFF6B7B8D),
    accent = Color(0xFF3E86F7),
    accentAlt = Color(0xFF6EA8FF),
    accentCool = Color(0xFF4FD8E8),
    accentSky = Color(0xFF4FC3F7),
    accentWarm = Color(0xFFF5A85B),
    accentViolet = Color(0xFFA78BFA),
    accentPink = Color(0xFFF072C8),
    accentMuted = Color(0xFF12233A),
    success = Color(0xFF22C55E),
    successSurface = Color(0xFF10593F),
    danger = Color(0xFFFF4D6D),
    // The filled pill carries the blue-to-violet brand gradient, so its label is white.
    buttonPrimary = Color(0xFF3E86F7),
    onButtonPrimary = Color(0xFFFFFFFF),
    switchTrackOff = Color(0xFF3A4553),
    switchThumbOff = Color(0xFFB9C4D1),
    chartTrack = Color(0xFF1B2836),
    controlOutline = Color(0xFF4A5768),
    isDark = true,
)

/**
 * Light palette.
 *
 * Same structure, same spacing, inverted surfaces. The dark palette is the tuned one the reference
 * specifies; this exists so the Appearance screen's System option is a real choice rather than a
 * switch that does nothing.
 */
val LightColors = RobolockColors(
    background = Color(0xFFF2F4F8),
    backgroundTop = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDF1F7),
    surfaceHighlight = Color(0xFFF7F9FC),
    border = Color(0xFFDFE5EE),
    borderStrong = Color(0xFFC7D0DE),
    textPrimary = Color(0xFF0B111A),
    textSecondary = Color(0xFF5A6779),
    textTertiary = Color(0xFF8B98A9),
    accent = Color(0xFF1F6FEB),
    accentAlt = Color(0xFF3E86F7),
    accentCool = Color(0xFF0E9AAE),
    accentSky = Color(0xFF0B84C7),
    accentWarm = Color(0xFFD8842B),
    accentViolet = Color(0xFF7C5CE0),
    accentPink = Color(0xFFD64BA8),
    accentMuted = Color(0xFFE3ECFC),
    success = Color(0xFF15803D),
    successSurface = Color(0xFFD6F2E1),
    danger = Color(0xFFDC2626),
    buttonPrimary = Color(0xFF0B111A),
    onButtonPrimary = Color(0xFFFFFFFF),
    switchTrackOff = Color(0xFFCBD4E1),
    switchThumbOff = Color(0xFFFFFFFF),
    chartTrack = Color(0xFFE2E8F0),
    controlOutline = Color(0xFFA5B0C0),
    isDark = false,
)

val LocalRobolockColors = staticCompositionLocalOf { DarkColors }
