package com.robolock.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and shape scale taken from the reference design.
 *
 * Screens never hardcode a dp value; they compose from this scale, which is what keeps the rhythm
 * identical across screens and makes a global adjustment a one-line change.
 */
@Immutable
data class RobolockDimens(
    val screenPadding: Dp = 22.dp,
    val cardRadius: Dp = 22.dp,
    val cardRadiusSmall: Dp = 18.dp,
    val cardPadding: Dp = 16.dp,
    val cardPaddingLarge: Dp = 18.dp,
    val sectionSpacing: Dp = 18.dp,
    val itemSpacing: Dp = 12.dp,
    val tightSpacing: Dp = 6.dp,
    val buttonHeight: Dp = 56.dp,
    val rowHeight: Dp = 54.dp,
    /** Colour-filled squircle beside a title, e.g. the Settings and Focus Mode rows. */
    val iconTile: Dp = 42.dp,
    val iconTileRadius: Dp = 13.dp,
    val appIcon: Dp = 44.dp,
    val appIconRadius: Dp = 13.dp,
    val hairline: Dp = 1.dp,
    val bottomBarHeight: Dp = 62.dp,
)

val LocalRobolockDimens = staticCompositionLocalOf { RobolockDimens() }
