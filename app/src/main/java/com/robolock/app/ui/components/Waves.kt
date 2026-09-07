package com.robolock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockTheme

/**
 * The layered blue swells along the bottom of the reference screens.
 *
 * Drawn rather than shipped as bitmaps: it costs nothing in the APK, stays sharp at any density,
 * and can take its colours from the theme instead of being baked in. Three overlapping bands at
 * different opacities give the depth the reference has without any glow.
 *
 * Deliberately static. An animated background would work against the point of the product.
 */
@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    tint: Color = RobolockTheme.colors.accent,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height

        // Back band: highest, faintest, gentlest curve.
        drawPath(
            path = wavePath(w, h, baseline = 0.34f, amplitude = 0.16f, phase = 0.0f),
            brush = Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.04f)),
            ),
        )

        // Middle band.
        drawPath(
            path = wavePath(w, h, baseline = 0.52f, amplitude = 0.19f, phase = 0.9f),
            brush = Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.08f)),
            ),
        )

        // Front band: lowest and most saturated, which is what reads as the near edge.
        drawPath(
            path = wavePath(w, h, baseline = 0.72f, amplitude = 0.14f, phase = 2.1f),
            brush = Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.42f), tint.copy(alpha = 0.16f)),
            ),
        )
    }
}

/**
 * A single swell, closed off along the bottom of the box.
 *
 * Built from cubic segments rather than a sine sample so the crests stay smooth at any width.
 */
private fun wavePath(
    width: Float,
    height: Float,
    baseline: Float,
    amplitude: Float,
    phase: Float,
): Path = Path().apply {
    val y0 = height * baseline
    val amp = height * amplitude

    moveTo(0f, y0 + amp * kotlin.math.sin(phase).toFloat())

    val segments = 3
    val step = width / segments
    for (i in 0 until segments) {
        val x0 = step * i
        val x1 = step * (i + 1)
        val cp1x = x0 + step * 0.35f
        val cp2x = x0 + step * 0.65f
        val yStart = y0 + amp * kotlin.math.sin(phase + i * 1.7).toFloat()
        val yEnd = y0 + amp * kotlin.math.sin(phase + (i + 1) * 1.7).toFloat()
        val bulge = amp * 0.9f * if (i % 2 == 0) -1f else 1f

        cubicTo(cp1x, yStart + bulge, cp2x, yEnd + bulge, x1, yEnd)
    }

    lineTo(width, height)
    lineTo(0f, height)
    close()
}

/** Places waves along the bottom edge of whatever they are layered behind. */
@Composable
fun BoxScope.BottomWaves(height: Dp = 260.dp, tint: Color = RobolockTheme.colors.accent) {
    WaveBackground(
        modifier = Modifier.align(Alignment.BottomCenter),
        height = height,
        tint = tint,
    )
}

/** A full-bleed dark ground with waves at the foot of it. The app's default backdrop. */
@Composable
fun WaveScaffold(
    modifier: Modifier = Modifier,
    waveHeight: Dp = 260.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = RobolockTheme.colors
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.background),
                ),
            )
        }
        BottomWaves(height = waveHeight)
        content()
    }
}
