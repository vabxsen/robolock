package com.robolock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.robolock.rules.ChartBucket
import com.robolock.app.ui.theme.RobolockGradients
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import kotlin.math.ceil
import kotlin.math.max

/**
 * The seven capsules under the Home dashboard ring.
 *
 * Each day is a dim full-height track with a cyan-to-cobalt capsule filled from the bottom, so a
 * quiet day still reads as a day rather than as missing data.
 */
@Composable
fun WeeklyCapsuleChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 62.dp,
    capsuleWidth: Dp = 11.dp,
    /** The chart sits on the night illustration, so its labels do not follow the page palette. */
    labelColor: Color = RobolockTheme.colors.textPrimary,
) {
    val colors = RobolockTheme.colors
    val maxCount = max(1, buckets.maxOfOrNull(ChartBucket::count) ?: 1)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { index, bucket ->
            val fraction by animateFloatAsState(
                targetValue = (bucket.count.toFloat() / maxCount).coerceIn(0f, 1f),
                animationSpec = tween(700, delayMillis = index * 45),
                label = "weeklyCapsule",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .width(capsuleWidth)
                        .height(barHeight)
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // A tiny baseline is kept as a visual anchor, but zero activity never
                            // masquerades as an actual bar.
                            .height(
                                if (bucket.count == 0) 4.dp
                                else (barHeight * fraction).coerceAtLeast(10.dp),
                            )
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(RobolockGradients.cyanBar)),
                    )
                }
                Text(
                    text = bucket.label,
                    style = RobolockType.captionSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

/**
 * The larger chart on the Statistics screen: a labelled y-axis, faint gridlines, cobalt bars, and
 * an amber callout over the busiest day.
 *
 * The axis is rounded up to a clean step so the top gridline is a round number, which is what makes
 * the reference chart read as calm rather than arbitrary.
 */
@Composable
fun StatsBarChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
) {
    val colors = RobolockTheme.colors
    val peak = buckets.maxOfOrNull(ChartBucket::count) ?: 0
    val axisMax = niceAxisMax(peak)
    val steps = 3
    // Only worth calling out a busiest day when there is activity to call out.
    val highlight = if (peak > 0) buckets.indexOfFirst { it.count == peak } else -1

    Column(modifier = modifier.fillMaxWidth()) {
        if (highlight >= 0) {
            Row(Modifier.fillMaxWidth().padding(start = 30.dp)) {
                buckets.indices.forEach { index ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (index == highlight) {
                            // The callout is wider than one column, so it is allowed to overhang
                            // its neighbours rather than being squeezed to a single glyph.
                            ChartCallout(
                                text = peak.toString() + " blocked",
                                modifier = Modifier.wrapContentWidth(unbounded = true),
                            )
                        }
                    }
                }
            }
            Box(Modifier.height(6.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.height(chartHeight).width(30.dp).padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                for (step in steps downTo 0) {
                    Text(
                        text = (axisMax * step / steps).toString(),
                        style = RobolockType.captionSmall,
                        color = colors.textSecondary,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        // The grid gives the values a stable frame and prevents the chart from
                        // reading as a row of floating bars.
                        .drawBehind {
                            repeat(steps + 1) { tick ->
                                val y = size.height * tick / steps
                                drawLine(
                                    color = colors.border,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    buckets.forEachIndexed { index, bucket ->
                        val fraction by animateFloatAsState(
                            targetValue = if (axisMax == 0) {
                                0f
                            } else {
                                (bucket.count.toFloat() / axisMax).coerceIn(0f, 1f)
                            },
                            animationSpec = tween(700, delayMillis = index * 50),
                            label = "statsBar",
                        )
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(
                                        if (bucket.count == 0) 0.dp
                                        else (chartHeight * fraction).coerceAtLeast(6.dp),
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (index == highlight) {
                                                RobolockGradients.amberBar
                                            } else {
                                                RobolockGradients.blueBar
                                            },
                                        ),
                                    ),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    buckets.forEachIndexed { index, bucket ->
                        Text(
                            text = bucket.label,
                            style = RobolockType.captionSmall,
                            color = if (index == highlight) colors.accentWarm else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** The amber pill with a downward pointer that sits over the busiest bar. */
@Composable
private fun ChartCallout(text: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(RobolockGradients.amberBar))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                style = RobolockType.captionSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF3A2708),
                maxLines = 1,
            )
        }
        Canvas(Modifier.size(width = 12.dp, height = 7.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, Color(0xFFF3A44B))
        }
    }
}

/**
 * The Insights trend line.
 *
 * A teal-to-white stroke over a faint area fill, with a dot on every reading. Values are scaled to
 * the range's own peak, because the point of this chart is the shape of the week rather than an
 * absolute total.
 */
@Composable
fun TrendLineChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 118.dp,
) {
    val colors = RobolockTheme.colors
    if (buckets.isEmpty()) return
    val peak = max(1, buckets.maxOfOrNull(ChartBucket::count) ?: 1)
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800),
        label = "trendLine",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.fillMaxSize()) {
                val count = buckets.size
                val stepX = if (count > 1) size.width / (count - 1) else size.width
                val topInset = size.height * 0.12f
                val usable = size.height - topInset - size.height * 0.10f

                val points = buckets.mapIndexed { index, bucket ->
                    val x = stepX * index
                    val y = topInset + usable * (1f - bucket.count.toFloat() / peak)
                    Offset(x, y)
                }

                // Faint verticals under each reading, as in the reference.
                points.forEach { point ->
                    drawLine(
                        color = colors.accentCool.copy(alpha = 0.12f),
                        start = Offset(point.x, point.y),
                        end = Offset(point.x, size.height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
                    )
                }

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(colors.accentCool.copy(alpha = 0.22f), Color.Transparent),
                    ),
                )
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(RobolockGradients.tealLine),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                points.forEachIndexed { index, point ->
                    val revealed = index.toFloat() / points.size <= progress
                    if (!revealed) return@forEachIndexed
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = point)
                    drawCircle(
                        color = colors.accentCool.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = point,
                        style = Stroke(width = 2f),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = RobolockType.captionSmall,
                    color = colors.accentCool.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Rounds an axis maximum up to the next clean step so the labels stay legible. */
internal fun niceAxisMax(peak: Int): Int {
    if (peak <= 0) return 3
    val steps = intArrayOf(3, 6, 9, 15, 30, 60, 90, 150, 300, 600, 900)
    return steps.firstOrNull { it >= peak } ?: (ceil(peak / 300.0) * 300).toInt()
}
