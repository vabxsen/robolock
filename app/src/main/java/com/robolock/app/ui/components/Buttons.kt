package com.robolock.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockBrushes
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The filled pill button.
 *
 * On the reference this is a full-width capsule carrying a soft lavender-to-periwinkle wash with
 * near-black label text. The press feedback is a restrained scale-down rather than a ripple,
 * matching the premium feel of the design where nothing flashes.
 */
@Composable
fun RobolockPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = false,
    height: Dp = RobolockTheme.dimens.buttonHeight,
    brush: Brush = RobolockBrushes.primaryPill(),
    contentColor: Color = RobolockTheme.colors.onButtonPrimary,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, label = "primaryButtonScale")
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(shape)
            .background(brush)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = RobolockType.labelStrong.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
            )
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp),
                )
            }
        }
    }
}

/**
 * The cobalt-to-violet call to action inside the permission cards.
 *
 * Slightly shorter than the page-level button so it reads as belonging to its card rather than to
 * the screen.
 */
@Composable
fun RobolockAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 46.dp,
) {
    RobolockPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        brush = RobolockBrushes.accentPill(),
        contentColor = Color(0xFF0B1020),
    )
}

/**
 * Outlined pill: "Continue anyway" on the blocking screens.
 *
 * The border takes the blocking app's colour, which is what ties the escape hatch to the app the
 * user is being held back from without filling the button and competing with "Go back".
 */
@Composable
fun RobolockSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = RobolockTheme.dimens.buttonHeight,
    borderColor: Color? = null,
    textColor: Color? = null,
) {
    val colors = RobolockTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, label = "secondaryButtonScale")
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(shape)
            .border(BorderStroke(1.5.dp, borderColor ?: colors.borderStrong), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = RobolockType.labelStrong.copy(fontWeight = FontWeight.Bold),
            color = textColor ?: colors.textPrimary,
        )
    }
}
