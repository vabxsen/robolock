package com.robolock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

/**
 * The illustrated landscapes behind the welcome, onboarding, dashboard and about screens.
 *
 * They are painted rather than shipped as bitmaps for three reasons: they stay crisp on every
 * density, they cost nothing in APK size, and every colour in them is a token that moves with the
 * palette. Each scene is deterministic — the ridge lines and star positions come from a fixed
 * seed — so the artwork never changes between recompositions or launches.
 */

/** Night sky, magenta horizon glow, layered purple ridges and a black pine foreground. */
@Composable
fun NightRidgeScene(
    modifier: Modifier = Modifier,
    /** Where the horizon sits, as a fraction of the height. */
    horizon: Float = 0.62f,
    showPines: Boolean = true,
    dimming: Float = 0f,
) {
    // A plain Canvas does not clip, and the horizon glow is deliberately wider than the scene, so
    // the box has to hold it in or it washes over the whole page.
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            // Everything below the horizon is measured against the ground, so a scene with a high
            // horizon keeps the same layering as one with a low horizon.
            val ground = 1f - horizon

            drawNightSky(horizon)
            drawStars(seed = 41, topFraction = 0f, bottomFraction = horizon - 0.08f, count = 70)
            drawHorizonGlow(horizon)

            // Far ridge: lilac peaks catching the last of the light, poking just above the glow.
            drawRidge(
                crest = horizon + ground * 0.13f,
                amplitude = ground * 0.13f,
                peaks = 7,
                seed = 7,
                color = Color(0xFF544878),
                snow = Color(0xFFC0B8E0),
            )
            // Middle ridge: deeper violet.
            drawRidge(
                crest = horizon + ground * 0.34f,
                amplitude = ground * 0.10f,
                peaks = 5,
                seed = 19,
                color = Color(0xFF2A2349),
                snow = Color(0xFF6E679A),
            )
            // Near hills: a dark mass rather than a skyline, so the foreground stays quiet.
            drawRidge(
                crest = horizon + ground * 0.58f,
                amplitude = ground * 0.06f,
                peaks = 4,
                seed = 31,
                color = Color(0xFF121531),
            )
            drawRidge(
                crest = horizon + ground * 0.82f,
                amplitude = ground * 0.04f,
                peaks = 3,
                seed = 55,
                color = Color(0xFF06081A),
            )

            if (showPines) {
                drawPineRow(
                    baseline = horizon + ground * 0.94f,
                    heightFraction = ground * 0.52f,
                    seed = 3,
                    count = 4,
                    fromLeft = true,
                    color = Color(0xFF03050D),
                )
                drawPineRow(
                    baseline = horizon + ground * 0.97f,
                    heightFraction = ground * 0.60f,
                    seed = 12,
                    count = 4,
                    fromLeft = false,
                    color = Color(0xFF03050D),
                )
            }

            if (dimming > 0f) {
                drawRect(color = Color(0xFF050A11).copy(alpha = dimming.coerceIn(0f, 1f)))
            }
        }
    }
}

/** Warm sunset over a lake, with snow-capped peaks and a reflected sun. */
@Composable
fun SunsetLakeScene(
    modifier: Modifier = Modifier,
    horizon: Float = 0.55f,
) {
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawSunsetSky(horizon)
            drawSun(horizon)

            drawRidge(
                crest = horizon - 0.02f,
                amplitude = 0.13f,
                peaks = 5,
                seed = 23,
                color = Color(0xFF6F72AB),
                snow = Color(0xFFE4E8FA),
            )
            drawRidge(
                crest = horizon + 0.01f,
                amplitude = 0.08f,
                peaks = 4,
                seed = 61,
                color = Color(0xFF454C86),
                snow = Color(0xFFA9B0DA),
            )

            drawLake(horizon)

            // Wooded banks framing the water on both sides.
            drawPineRow(
                baseline = horizon + 0.16f,
                heightFraction = (1f - horizon) * 0.40f,
                seed = 9,
                count = 3,
                fromLeft = true,
                color = Color(0xFF12321F),
            )
            drawPineRow(
                baseline = horizon + 0.20f,
                heightFraction = (1f - horizon) * 0.45f,
                seed = 44,
                count = 3,
                fromLeft = false,
                color = Color(0xFF0E2A1B),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Painting primitives
// ---------------------------------------------------------------------------------------------

/**
 * The sky, painted across the whole canvas rather than stopping at the horizon.
 *
 * Cutting the gradient at the horizon and filling below it with a flat colour leaves a hard line
 * wherever no ridge covers it, so the stops are positioned as fractions of the full height and the
 * wash simply keeps darkening past the horizon.
 */
private fun DrawScope.drawNightSky(horizon: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to Color(0xFF060A1C),
            horizon * 0.30f to Color(0xFF101A46),
            horizon * 0.55f to Color(0xFF2B2160),
            horizon * 0.83f to Color(0xFF6B3A80),
            horizon to Color(0xFFC66BA2),
            (horizon + (1f - horizon) * 0.22f).coerceAtMost(0.999f) to Color(0xFF3A2450),
            1.00f to Color(0xFF090B1E),
            startY = 0f,
            endY = size.height,
        ),
    )
}

private fun DrawScope.drawSunsetSky(horizon: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to Color(0xFF1B2350),
            0.30f to Color(0xFF4A3C7B),
            0.58f to Color(0xFF8E5B90),
            0.80f to Color(0xFFD9835F),
            1.00f to Color(0xFFF9C489),
            startY = 0f,
            endY = size.height * horizon,
        ),
        size = Size(size.width, size.height * horizon),
    )
}

private fun DrawScope.drawHorizonGlow(horizon: Float) {
    val centre = Offset(size.width * 0.5f, size.height * horizon)
    val radius = size.width * 0.95f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFA8D2).copy(alpha = 0.72f),
                Color(0xFFB25FC0).copy(alpha = 0.30f),
                Color.Transparent,
            ),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

private fun DrawScope.drawSun(horizon: Float) {
    val centre = Offset(size.width * 0.78f, size.height * (horizon - 0.06f))
    val radius = size.width * 0.10f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD9A0).copy(alpha = 0.75f), Color.Transparent),
            center = centre,
            radius = radius * 3.2f,
        ),
        radius = radius * 3.2f,
        center = centre,
    )
    drawCircle(color = Color(0xFFFFE3B4), radius = radius, center = centre)
}

private fun DrawScope.drawStars(
    seed: Int,
    topFraction: Float,
    bottomFraction: Float,
    count: Int,
) {
    val random = Random(seed)
    repeat(count) {
        val x = random.nextFloat() * size.width
        val y = (topFraction + random.nextFloat() * (bottomFraction - topFraction)) * size.height
        val r = 0.6f + random.nextFloat() * 1.3f
        // Stars fade out towards the lit horizon so they never sit on top of the glow.
        val depth = 1f - ((y / size.height) / bottomFraction).coerceIn(0f, 1f)
        drawCircle(
            color = Color.White.copy(alpha = (0.20f + random.nextFloat() * 0.55f) * depth),
            radius = r,
            center = Offset(x, y),
        )
    }
}

/**
 * One mountain silhouette across the full width.
 *
 * Control points alternate valley, peak, valley, so the flanks stay long and straight and the
 * summits stay sharp — the shape of a mountain range rather than the noise a per-pixel jitter
 * produces. Only the peaks are randomised, which is what keeps two ridges from rhyming.
 */
private fun DrawScope.drawRidge(
    crest: Float,
    amplitude: Float,
    peaks: Int,
    seed: Int,
    color: Color,
    snow: Color? = null,
) {
    val random = Random(seed)
    val points = peaks * 2
    // A little horizontal drift stops the summits landing on a regular grid.
    val drift = FloatArray(points + 1) { (random.nextFloat() - 0.5f) * 0.5f }
    val heights = FloatArray(points + 1) { index ->
        if (index % 2 == 1) {
            crest - amplitude * (0.55f + random.nextFloat() * 0.75f)
        } else {
            crest + amplitude * (0.15f + random.nextFloat() * 0.45f)
        }
    }

    fun pointX(index: Int): Float =
        size.width * ((index + drift[index]) / points).coerceIn(0f, 1f)

    fun pointY(index: Int): Float = heights[index] * size.height

    val path = Path().apply {
        moveTo(0f, pointY(0))
        for (i in 1..points) lineTo(pointX(i), pointY(i))
        lineTo(size.width, pointY(points))
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color)

    if (snow == null) return

    // A cap on each summit, sized to how far that summit stands above its own valleys.
    for (i in 1 until points step 2) {
        val x = pointX(i)
        val y = pointY(i)
        val drop = (minOf(pointY(i - 1), pointY(i + 1)) - y) * 0.34f
        if (drop <= 0f) continue
        val leftSlope = (x - pointX(i - 1)) / (pointY(i - 1) - y).coerceAtLeast(1f)
        val rightSlope = (pointX(i + 1) - x) / (pointY(i + 1) - y).coerceAtLeast(1f)
        val cap = Path().apply {
            moveTo(x, y)
            lineTo(x + rightSlope * drop, y + drop)
            lineTo(x + rightSlope * drop * 0.35f, y + drop * 0.62f)
            lineTo(x - leftSlope * drop * 0.30f, y + drop * 0.80f)
            lineTo(x - leftSlope * drop, y + drop)
            close()
        }
        drawPath(cap, snow.copy(alpha = 0.80f))
    }
}

/**
 * A cluster of conifers along one edge.
 *
 * [fromLeft] mirrors the cluster so a scene can be framed on both sides without two seeds that
 * happen to look alike.
 */
private fun DrawScope.drawPineRow(
    baseline: Float,
    heightFraction: Float,
    seed: Int,
    count: Int,
    fromLeft: Boolean,
    color: Color,
) {
    val random = Random(seed)
    repeat(count) { index ->
        // Pines frame the scene from the edges inwards; the middle stays open for the content.
        val spread = 0.26f
        val t = index.toFloat() / (count - 1).coerceAtLeast(1)
        val xFraction = if (fromLeft) t * spread else 1f - t * spread
        val x = size.width * (xFraction + (random.nextFloat() - 0.5f) * 0.06f)
        val h = size.height * heightFraction * (0.62f + random.nextFloat() * 0.55f)
        drawPine(
            centreX = x,
            baseY = size.height * baseline + random.nextFloat() * size.height * 0.03f,
            height = h,
            color = color,
        )
    }
}

private fun DrawScope.drawPine(centreX: Float, baseY: Float, height: Float, color: Color) {
    val halfWidth = height * 0.24f
    val trunkWidth = height * 0.05f
    drawRect(
        color = color,
        topLeft = Offset(centreX - trunkWidth / 2f, baseY - height * 0.16f),
        size = Size(trunkWidth, height * 0.18f),
    )
    // Three overlapping tiers, each narrower than the one below it.
    repeat(3) { tier ->
        val scale = 1f - tier * 0.24f
        val tierBase = baseY - height * (0.12f + tier * 0.24f)
        val tierTop = tierBase - height * 0.42f * scale
        val path = Path().apply {
            moveTo(centreX, tierTop)
            lineTo(centreX + halfWidth * scale, tierBase)
            lineTo(centreX - halfWidth * scale, tierBase)
            close()
        }
        drawPath(path, color)
    }
}

/** The lake: a darkened mirror of the sky with a few horizontal light bands. */
private fun DrawScope.drawLake(horizon: Float) {
    val top = size.height * horizon
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to Color(0xFF6E5E8E),
            0.25f to Color(0xFF3B4472),
            1.0f to Color(0xFF141B3A),
            startY = top,
            endY = size.height,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top),
    )
    // The sun's reflection: a soft pool of light on the water directly below it. A radial falloff
    // rather than a column, so it has no edge to give itself away as a rectangle.
    val depth = size.height - top
    val centre = Offset(size.width * 0.78f, top)
    val radius = depth * 1.1f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD2A0).copy(alpha = 0.30f),
                Color(0xFFFFD2A0).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

/**
 * A soft coloured wash used behind the blocking screens.
 *
 * Static by design: an animated background on a screen whose whole purpose is to reduce
 * stimulation would be working against the product.
 */
@Composable
fun AuroraBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    // x fraction, y fraction, radius (× width), alpha. Kept tight and few so the ground stays
    // near-black and the colour reads as light thrown onto it rather than as a coloured page.
    val bands = remember(tint) {
        listOf(
            Quad(0.10f, 0.42f, 0.62f, 0.34f),
            Quad(0.92f, 0.52f, 0.58f, 0.30f),
            Quad(0.50f, 0.36f, 0.44f, 0.16f),
        )
    }
    Canvas(modifier.fillMaxSize().clipToBounds()) {
        drawRect(color = Color(0xFF08090E))

        bands.forEach { band ->
            val centre = Offset(size.width * band.x, size.height * band.y)
            val radius = size.width * band.radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = band.alpha), Color.Transparent),
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
            )
        }

        // Soft ribbons keep the wash from reading as a flat vignette.
        repeat(3) { index ->
            val path = Path()
            val baseY = size.height * (0.36f + index * 0.11f)
            val amp = size.height * 0.045f
            path.moveTo(0f, baseY)
            var x = 0f
            val step = size.width / 40f
            while (x <= size.width) {
                val y = baseY + sin(x / size.width * 5.4f + index * 1.3f) * amp
                path.lineTo(x, y)
                x += step
            }
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, tint.copy(alpha = 0.05f))
        }

        // Top and bottom fade back to black so the headline and the buttons sit on a calm ground.
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color(0xFF06070C),
                0.22f to Color.Transparent,
                0.74f to Color.Transparent,
                1.00f to Color(0xFF06070C),
            ),
        )
    }
}

private data class Quad(val x: Float, val y: Float, val radius: Float, val alpha: Float)
