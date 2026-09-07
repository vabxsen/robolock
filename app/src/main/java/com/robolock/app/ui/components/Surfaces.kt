package com.robolock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robolock.app.ui.theme.RobolockGradients
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The card the whole design is built from: a slightly lifted navy surface on a near-black ground
 * with a hairline border that keeps the edge readable without any elevation shadow.
 */
@Composable
fun RobolockCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = RobolockTheme.dimens.cardPadding,
    radius: Dp = RobolockTheme.dimens.cardRadius,
    background: Color = RobolockTheme.colors.surface,
    brush: Brush? = null,
    bordered: Boolean = true,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RobolockTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(background))
            .then(
                if (bordered) {
                    Modifier.border(BorderStroke(1.dp, borderColor ?: colors.border), shape)
                } else {
                    Modifier
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Card that groups rows with dividers, as used by Settings, App Control and Focus Mode. */
@Composable
fun RobolockGroupCard(
    modifier: Modifier = Modifier,
    radius: Dp = RobolockTheme.dimens.cardRadius,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RobolockTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape),
        content = content,
    )
}

/** Hairline separator between rows inside a group card. */
@Composable
fun RowDivider(startInset: Dp = 0.dp, endInset: Dp = 0.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset, end = endInset)
            .height(1.dp)
            .background(RobolockTheme.colors.border)
    )
}

/** Bold heading that introduces a group of cards: "When to block", "Friction settings". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RobolockType.sectionHeader,
        color = RobolockTheme.colors.textPrimary,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

/**
 * The gradient quote card.
 *
 * A tinted badge carrying an oversized quotation glyph sits beside two lines of text, on a wash
 * that runs violet to cobalt across the card.
 */
@Composable
fun QuoteCard(
    firstLine: String,
    secondLine: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = RobolockGradients.violetToCobalt,
    badgeColor: Color = Color(0xFF6B3D9E),
    glyphColor: Color = RobolockTheme.colors.accentPink,
) {
    RobolockCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        radius = 18.dp,
        brush = Brush.horizontalGradient(gradient),
        borderColor = Color.White.copy(alpha = 0.10f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(badgeColor.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "“",
                    style = RobolockType.metricSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        lineHeight = 34.sp,
                    ),
                    color = glyphColor,
                    // The glyph's ink sits at the top of its box; nudging it down centres it.
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            HSpace(14.dp)
            // The wash behind this card is dark in both themes, so the text is fixed white rather
            // than taken from the palette.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(firstLine, style = RobolockType.label, color = Color.White)
                Text(secondLine, style = RobolockType.label, color = Color.White)
            }
        }
    }
}

/**
 * Rounded square that holds a leading icon.
 *
 * The reference gives each row's icon its own saturated fill rather than a neutral tile, so
 * [background] and [brush] are the interesting parameters here; the neutral default is used only
 * by the Focus Mode rows, where the tile stays dark until its option is selected.
 */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    size: Dp = RobolockTheme.dimens.iconTile,
    radius: Dp = RobolockTheme.dimens.iconTileRadius,
    background: Color = RobolockTheme.colors.surfaceElevated,
    brush: Brush? = null,
    bordered: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = RobolockTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(background))
            .then(
                if (bordered) Modifier.border(BorderStroke(1.dp, colors.border), shape) else Modifier,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** Small status pill, e.g. the "↑ 18%" badge on the Insights card. */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = RobolockTheme.colors.successSurface,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = RobolockType.captionSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

/** Fixed-width spacer helper used to keep row alignment consistent. */
@Composable
fun HSpace(width: Dp) = Box(Modifier.width(width))
