package com.robolock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Robolock mark: a padlock whose body is a robot's visored head.
 *
 * Drawn rather than shipped as a bitmap so it stays crisp at any size and can be tinted from the
 * theme. The shell carries the same blue-to-violet gradient as the primary button, which is what
 * ties the mascot, the wordmark and the buttons together as one identity.
 *
 * The eyes are curved rather than round on purpose — this app interrupts people, and a mark that
 * looks friendly rather than stern does a lot of quiet work.
 */
@Composable
fun RobolockMark(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    glowColor: Color = Color(0xFF5A7BFF),
    eyeColor: Color = Color(0xFFFFFFFF),
    showGlow: Boolean = true,
) {
    val shellStart = Color(0xFF6FA8FF)
    val shellEnd = Color(0xFF8A5CF6)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val shell = Brush.linearGradient(
                colors = listOf(shellStart, shellEnd),
                start = Offset(w * 0.1f, h * 0.1f),
                end = Offset(w * 0.95f, h * 0.9f),
            )

            if (showGlow) {
                val centre = Offset(w / 2f, h * 0.55f)
                val radius = w * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.34f),
                            glowColor.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = centre,
                        radius = radius,
                    ),
                    radius = radius,
                    center = centre,
                )
            }

            // --- Shackle -------------------------------------------------------------------
            val shackleStroke = w * 0.105f
            val shackleWidth = w * 0.44f
            val shackleTop = h * 0.11f
            val shackleArc = w * 0.40f
            val legBottom = h * 0.46f
            val shackleLeft = (w - shackleWidth) / 2f

            drawArc(
                brush = shell,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(shackleLeft, shackleTop),
                size = Size(shackleWidth, shackleArc),
                style = Stroke(width = shackleStroke, cap = StrokeCap.Round),
            )
            listOf(shackleLeft, shackleLeft + shackleWidth).forEach { x ->
                drawLine(
                    brush = shell,
                    start = Offset(x, shackleTop + shackleArc / 2f),
                    end = Offset(x, legBottom),
                    strokeWidth = shackleStroke,
                    cap = StrokeCap.Butt,
                )
            }

            // --- Body ----------------------------------------------------------------------
            val bodyWidth = w * 0.80f
            val bodyHeight = h * 0.40f
            val bodyLeft = (w - bodyWidth) / 2f
            val bodyTop = h * 0.42f
            val bodyRadius = bodyHeight * 0.40f

            // --- Side pods, drawn behind so the body overlaps their inner edge ---------------
            val podWidth = w * 0.085f
            val podHeight = bodyHeight * 0.52f
            val podY = bodyTop + bodyHeight * 0.24f
            listOf(bodyLeft - podWidth * 0.62f, bodyLeft + bodyWidth - podWidth * 0.38f).forEach { x ->
                drawRoundRect(
                    brush = shell,
                    topLeft = Offset(x, podY),
                    size = Size(podWidth, podHeight),
                    cornerRadius = CornerRadius(podWidth * 0.5f),
                )
                drawRoundRect(
                    color = Color(0xFFEAF1FF),
                    topLeft = Offset(x + podWidth * 0.34f, podY + podHeight * 0.20f),
                    size = Size(podWidth * 0.30f, podHeight * 0.60f),
                    cornerRadius = CornerRadius(podWidth * 0.16f),
                )
            }

            drawRoundRect(
                brush = shell,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyRadius),
            )

            // --- Visor ---------------------------------------------------------------------
            val visorInsetX = bodyWidth * 0.085f
            val visorTop = bodyTop + bodyHeight * 0.15f
            val visorHeight = bodyHeight * 0.60f
            val visorWidth = bodyWidth - visorInsetX * 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141A28), Color(0xFF05070E)),
                    startY = visorTop,
                    endY = visorTop + visorHeight,
                ),
                topLeft = Offset(bodyLeft + visorInsetX, visorTop),
                size = Size(visorWidth, visorHeight),
                cornerRadius = CornerRadius(visorHeight * 0.38f),
            )

            // --- Eyes: two upward arcs, i.e. a smile ----------------------------------------
            val eyeStroke = visorHeight * 0.14f
            val eyeWidth = visorWidth * 0.22f
            val eyeHeight = visorHeight * 0.34f
            val eyeY = visorTop + visorHeight * 0.34f
            listOf(
                bodyLeft + visorInsetX + visorWidth * 0.16f,
                bodyLeft + visorInsetX + visorWidth * 0.62f,
            ).forEach { x ->
                drawArc(
                    color = eyeColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(x, eyeY),
                    size = Size(eyeWidth, eyeHeight),
                    style = Stroke(width = eyeStroke, cap = StrokeCap.Round),
                )
            }

            // --- Chin tab ------------------------------------------------------------------
            drawRoundRect(
                color = Color(0xFF2A3352),
                topLeft = Offset(w / 2f - bodyWidth * 0.09f, bodyTop + bodyHeight * 0.83f),
                size = Size(bodyWidth * 0.18f, bodyHeight * 0.07f),
                cornerRadius = CornerRadius(bodyHeight * 0.035f),
            )
        }
    }
}

/**
 * The small app badge beside the wordmark in the Home header: a cobalt squircle holding the
 * shield-and-target glyph.
 */
@Composable
fun RobolockBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(
                Brush.linearGradient(listOf(Color(0xFF5D8BFF), Color(0xFF7C6BF5))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.56f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.11f
            val shield = Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                lineTo(w * 0.94f, h * 0.24f)
                lineTo(w * 0.94f, h * 0.56f)
                cubicTo(w * 0.94f, h * 0.82f, w * 0.74f, h * 0.94f, w * 0.5f, h * 0.99f)
                cubicTo(w * 0.26f, h * 0.94f, w * 0.06f, h * 0.82f, w * 0.06f, h * 0.56f)
                lineTo(w * 0.06f, h * 0.24f)
                close()
            }
            drawPath(shield, Color.White, style = Stroke(width = stroke))
            drawCircle(
                color = Color.White,
                radius = w * 0.17f,
                center = Offset(w * 0.5f, h * 0.50f),
                style = Stroke(width = stroke * 0.85f),
            )
        }
    }
}

/** The circular "no entry" disc at the centre of every blocking overlay. */
@Composable
fun ProhibitionMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = Color.White,
    glowColor: Color = color,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.085f
        val radius = (w - stroke) / 2f
        val centre = Offset(w / 2f, this.size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                center = centre,
                radius = radius * 1.7f,
            ),
            radius = radius * 1.7f,
            center = centre,
        )
        drawCircle(color = color, radius = radius, center = centre, style = Stroke(width = stroke))
        val diagonal = radius * 0.707f
        drawLine(
            color = color,
            start = Offset(centre.x - diagonal, centre.y + diagonal),
            end = Offset(centre.x + diagonal, centre.y - diagonal),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The phone-with-a-blocked-feed illustration behind every block screen headline.
 *
 * A dark device silhouette carrying a faint tinted feed, with the prohibition disc sitting over
 * it — the same composition as the reference, drawn so it takes the blocking app's colour.
 */
@Composable
fun BlockedPhoneIllustration(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val bodyWidth = w * 0.52f
            val bodyHeight = h * 0.86f
            val left = (w - bodyWidth) / 2f
            val top = (h - bodyHeight) / 2f
            val radius = bodyWidth * 0.16f

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.55f,
                ),
                topLeft = Offset(left - w * 0.12f, top - h * 0.06f),
                size = Size(bodyWidth + w * 0.24f, bodyHeight + h * 0.12f),
                cornerRadius = CornerRadius(radius * 2f),
            )

            // Device shell.
            drawRoundRect(
                color = Color(0xFF1A1D26),
                topLeft = Offset(left, top),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(radius),
            )
            // Screen.
            val inset = bodyWidth * 0.045f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.34f), Color(0xFF090C14)),
                    startY = top,
                    endY = top + bodyHeight,
                ),
                topLeft = Offset(left + inset, top + inset),
                size = Size(bodyWidth - inset * 2f, bodyHeight - inset * 2f),
                cornerRadius = CornerRadius(radius * 0.82f),
            )
            // Notch.
            drawRoundRect(
                color = Color(0xFF1A1D26),
                topLeft = Offset(w / 2f - bodyWidth * 0.14f, top + inset * 1.6f),
                size = Size(bodyWidth * 0.28f, bodyHeight * 0.022f),
                cornerRadius = CornerRadius(bodyHeight * 0.02f),
            )
            // Suggestion of a feed behind the block.
            repeat(6) { index ->
                val y = top + bodyHeight * (0.18f + index * 0.115f)
                val rowWidth = bodyWidth * (0.62f - (index % 3) * 0.11f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.09f),
                    topLeft = Offset(left + bodyWidth * 0.14f, y),
                    size = Size(rowWidth, bodyHeight * 0.028f),
                    cornerRadius = CornerRadius(bodyHeight * 0.014f),
                )
            }
        }
    }
}
