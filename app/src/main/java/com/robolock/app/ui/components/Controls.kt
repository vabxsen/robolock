package com.robolock.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * Track-and-thumb switch matching the reference.
 *
 * Material 3's own Switch is noticeably chunkier and carries an icon in the thumb, which does not
 * match the design, so this draws the two shapes directly. Accessibility semantics come from
 * [Modifier.clickable] with [Role.Switch] plus the toggle state passed by the caller.
 */
@Composable
fun RobolockSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = RobolockTheme.colors.accent,
) {
    val colors = RobolockTheme.colors
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val inset = (trackHeight - thumbSize) / 2

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else colors.switchTrackOff,
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - inset else inset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
        label = "switchThumb",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else colors.switchThumbOff,
        label = "switchThumbColor",
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(48.dp)
            // A switch that cannot be operated must not look live, or an uninstalled app reads as
            // actively blocked.
            .alpha(if (enabled) 1f else 0.35f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor),
        ) {
            Box(
                Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), inset.roundToPx()) }
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

/**
 * The single-choice mark on the Focus Mode rows.
 *
 * Selected is a filled cobalt disc with the page colour punched out of the middle; unselected is a
 * thin grey ring. Drawn rather than using Material's RadioButton, whose ring weight and 48dp
 * bounding box do not match the reference.
 */
@Composable
fun RadioMark(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    activeColor: Color = RobolockTheme.colors.accent,
) {
    val colors = RobolockTheme.colors
    val ring by animateColorAsState(
        targetValue = if (selected) activeColor else colors.controlOutline,
        label = "radioRing",
    )
    Canvas(modifier.size(size)) {
        val radius = this.size.width / 2f
        val centre = Offset(radius, radius)
        if (selected) {
            drawCircle(color = ring, radius = radius, center = centre)
            drawCircle(color = colors.background, radius = radius * 0.44f, center = centre)
        } else {
            val stroke = this.size.width * 0.075f
            drawCircle(
                color = ring,
                radius = radius - stroke / 2f,
                center = centre,
                style = Stroke(width = stroke),
            )
        }
    }
}

/** Day/Week/Month/All selector from the Statistics screen. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceElevated)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) colors.accent else Color.Transparent,
                label = "segmentBackground",
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Color.White else colors.textSecondary,
                label = "segmentText",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = RobolockType.label.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = textColor,
                )
            }
        }
    }
}

/** Onboarding page indicator. */
@Composable
fun PagerDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == selectedIndex
            val diameter by animateDpAsState(if (selected) 8.dp else 7.dp, label = "dotWidth")
            val color by animateColorAsState(
                targetValue = if (selected) Color.White else colors.accent.copy(alpha = 0.38f),
                label = "dotColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .size(diameter)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
