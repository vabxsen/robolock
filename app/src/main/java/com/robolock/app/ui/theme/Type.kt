package com.robolock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Type scale matched to the reference: large, tightly tracked bold headlines, a clear three-step
 * hierarchy inside cards, and generous line height on body copy.
 */
private val Sans = FontFamily.SansSerif

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

object RobolockType {
    /** The splash and About wordmark. */
    val wordmark = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.4).sp,
    )

    /** Onboarding headlines: "Same apps. Less distractions." */
    val display = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.9).sp,
        lineHeightStyle = tightLineHeight,
    )

    /** Screen titles: "App Control", "Focus Mode", "Settings", "Insights". */
    val title = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = tightLineHeight,
    )

    /** Centred header on the Statistics screen. */
    val topBar = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Bold heading between grouped cards: "When to block", "Most blocked". */
    val sectionHeader = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
    )

    /** The big number inside the progress ring. */
    val metric = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.6).sp,
    )

    /** "4h 32m" on the Insights hero card. */
    val metricLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.3).sp,
    )

    /** Values inside the three small summary cards. */
    val metricSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.6).sp,
    )

    val greeting = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Row titles, app names, button labels. */
    val label = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp,
    )

    val labelStrong = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
    )

    /** Body copy and card subtitles. */
    val body = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    )

    val caption = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
    )

    val captionSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
    )

    /** The italic line that closes the welcome and blocking screens. */
    val quiet = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    )

    /** "A better you is a brighter tomorrow." across the onboarding landscape. */
    val script = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 20.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp,
    )

    /** Blocking overlay headline. */
    val overlayTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    )
}

/** Material 3 still needs a Typography for its own components; keep it consistent with the scale. */
val RobolockTypography = Typography(
    titleLarge = RobolockType.title,
    titleMedium = RobolockType.topBar,
    bodyLarge = RobolockType.body,
    bodyMedium = RobolockType.body,
    labelLarge = RobolockType.label,
    labelMedium = RobolockType.caption,
)
