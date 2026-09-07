package com.robolock.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The reference design's gradients, in one place.
 *
 * These are the handful of multi-stop washes that give the design its identity: the lavender pill
 * on the welcome screen, the periwinkle permission buttons, the cyan-to-pink progress ring, and the
 * violet-to-cobalt quote cards. Every one of them is defined once here so a screen never invents a
 * gradient of its own.
 */
object RobolockGradients {

    /**
     * The signature pill: "Get Started", "Continue", "Save Changes", the onboarding arrow button.
     *
     * Blue on the left running to violet on the right, carrying white text. This is the gradient
     * the mascot and the wordmark are built from, so it is the app's identity more than any single
     * flat colour is.
     */
    val primaryPill = listOf(
        Color(0xFF2F7BF6),
        Color(0xFF5B6BF5),
        Color(0xFF7C5CFC),
    )

    /** "Enable" / "Allow" / "Configure" on the permission cards. A shorter throw of the same idea. */
    val periwinklePill = listOf(
        Color(0xFF3B8BF7),
        Color(0xFF6C6FF8),
    )

    /** The wordmark, which fades white into blue and then violet across the letters. */
    val wordmark = listOf(
        Color(0xFFFFFFFF),
        Color(0xFF7FB2FF),
        Color(0xFF7C5CFC),
    )

    /**
     * The Home progress ring.
     *
     * The reference sweeps a single blue from light to deep rather than running the full spectrum,
     * which keeps the dashboard calm — a rainbow ring would fight the app's own argument about
     * reducing stimulation.
     */
    val ring = listOf(
        Color(0xFF6FB4FF),
        Color(0xFF3E86F7),
        Color(0xFF2F6BE0),
    )

    /** Home's weekly capsules. */
    val cyanBar = listOf(
        Color(0xFF5AA6FF),
        Color(0xFF2E6FE8),
    )

    /** Statistics bars: lighter at the cap, deeper at the base. */
    val blueBar = listOf(
        Color(0xFF6FB4FF),
        Color(0xFF2765D8),
    )

    /** The highlighted day on the Statistics chart, plus its tooltip. */
    val amberBar = listOf(
        Color(0xFFFFE0A8),
        Color(0xFFF3A44B),
    )

    /** Quote card on Home: violet on the left, cobalt on the right. */
    val violetToCobalt = listOf(
        Color(0xFF4C2C6E),
        Color(0xFF1E3E82),
    )

    /** Quote card on Insights: deeper cobalt, no violet. */
    val cobaltQuote = listOf(
        Color(0xFF15316F),
        Color(0xFF1D4CAE),
    )

    /** The Insights trend line. */
    val tealLine = listOf(
        Color(0xFF2FD3B0),
        Color(0xFFFFFFFF),
    )

    fun horizontal(colors: List<Color>): Brush = Brush.horizontalGradient(colors)

    fun vertical(colors: List<Color>): Brush = Brush.verticalGradient(colors)
}

/**
 * Gradients that must invert with the theme.
 *
 * The brand washes above are fixed — they are the product's colours — but the light theme needs
 * slightly deeper stops to keep white text legible, so the filled button asks for its brush here
 * rather than reaching for the raw list.
 */
object RobolockBrushes {

    @Composable
    @ReadOnlyComposable
    fun primaryPill(): Brush = Brush.horizontalGradient(
        if (LocalRobolockColors.current.isDark) {
            RobolockGradients.primaryPill
        } else {
            listOf(Color(0xFF1F6FEB), Color(0xFF5B4BE0))
        },
    )

    @Composable
    @ReadOnlyComposable
    fun accentPill(): Brush {
        val colors = LocalRobolockColors.current
        return if (colors.isDark) {
            Brush.horizontalGradient(RobolockGradients.periwinklePill)
        } else {
            Brush.horizontalGradient(listOf(colors.accent, colors.accentViolet))
        }
    }
}
