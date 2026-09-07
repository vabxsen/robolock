package com.robolock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockGradients
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import kotlin.math.roundToInt

/**
 * Number that counts up when it changes.
 *
 * Deliberately quick and eased-out: the reference feels premium because motion settles, not because
 * it is long.
 */
@Composable
fun animatedCount(target: Int, durationMillis: Int = 750): Int {
    val value by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis),
        label = "count",
    )
    return value.roundToInt()
}

/**
 * The progress ring on the Home dashboard.
 *
 * The arc carries a full sweep gradient — cyan at twelve o'clock running through periwinkle,
 * violet and pink — so the ring reads as one continuous light rather than as a tinted stroke. The
 * canvas is rotated so the gradient's own origin lines up with the arc's start at the top.
 */
@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 190.dp,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = RobolockTheme.colors.chartTrack,
    content: @Composable () -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ringProgress",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter).rotate(-90f)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )

            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = RobolockGradients.ring,
                        center = Offset(size.width / 2f, size.height / 2f),
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * One of the three summary cards under the dashboard.
 *
 * The icon sits in its own coloured glow — the only place in the design where a soft light is used
 * decoratively, and the thing that keeps three near-identical cards distinguishable at a glance.
 */
@Composable
fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = RobolockTheme.colors.accentCool,
    valueColor: Color? = null,
    /**
     * Change against the previous period, as a signed percentage.
     *
     * Null when there is no previous period to compare with, in which case nothing is shown —
     * a fresh install must not display an improvement it cannot have measured.
     */
    deltaPercent: Int? = null,
) {
    val colors = RobolockTheme.colors
    RobolockCard(
        modifier = modifier.fillMaxHeight(),
        contentPadding = 14.dp,
        radius = 18.dp,
        borderColor = iconColor.copy(alpha = 0.18f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                GlowIcon(icon = icon, tint = iconColor)
            }
            Text(
                text = value,
                style = RobolockType.metricSmall,
                color = valueColor ?: colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = RobolockType.captionSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (deltaPercent != null) {
                val rising = deltaPercent >= 0
                Text(
                    text = (if (rising) "↑ " else "↓ ") + "${kotlin.math.abs(deltaPercent)}%",
                    style = RobolockType.captionSmall,
                    color = if (rising) colors.success else colors.danger,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** An icon sitting in a soft radial glow of its own colour. */
@Composable
fun GlowIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.30f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Row of three metric cards.
 *
 * Height is driven by the tallest card so a two-line label on one of them does not leave the other
 * two visually short, which is how the reference reads.
 */
@Composable
fun MetricRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** The flat statistic tile used on the Statistics screen: a cobalt value over a quiet label. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    RobolockCard(
        modifier = modifier.fillMaxHeight(),
        contentPadding = 14.dp,
        radius = 18.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = RobolockType.metricSmall,
                color = colors.accentAlt,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = label,
                style = RobolockType.captionSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
