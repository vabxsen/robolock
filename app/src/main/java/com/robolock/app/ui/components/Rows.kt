package com.robolock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The Settings list row: a saturated icon tile, a title, an optional subtitle, and a trailing
 * affordance.
 *
 * The tile's colour is the row's identity in the reference — the list is scanned by colour before
 * it is read — so [iconBrush] is the parameter callers reach for most.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconBrush: Brush? = null,
    iconTint: Color = Color.White,
    trailingText: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(
                size = RobolockTheme.dimens.iconTile,
                radius = RobolockTheme.dimens.iconTileRadius,
                brush = iconBrush,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp),
                )
            }
            HSpace(14.dp)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = RobolockType.labelStrong, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = RobolockType.caption, color = colors.textSecondary)
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            if (trailingText != null) {
                Text(
                    trailingText,
                    style = RobolockType.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(end = if (showChevron) 6.dp else 0.dp),
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * A single-choice row: icon tile, title, subtitle, radio mark.
 *
 * The selected row lifts its own background and tints its tile, which is how the reference makes
 * the current choice readable without relying on the small mark alone.
 */
@Composable
fun RadioRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            size = RobolockTheme.dimens.iconTile,
            radius = RobolockTheme.dimens.iconTileRadius,
            background = if (selected) colors.accent.copy(alpha = 0.26f) else colors.surfaceElevated,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }
        HSpace(14.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = RobolockType.labelStrong, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = RobolockType.caption, color = colors.textSecondary)
            }
        }
        RadioMark(selected = selected)
    }
}

/**
 * A single "Block X" switch row inside an app control card.
 *
 * [subtitle] is used only where the label alone would over-promise -- an entry-point switch such as
 * "Block Shorts Shelf" cannot remove the shelf from someone else's feed, and saying so beats a
 * control that appears to do more than it does.
 */
@Composable
fun SurfaceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    activeColor: Color = RobolockTheme.colors.accent,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = RobolockType.label,
                color = if (enabled) colors.textPrimary else colors.textTertiary,
            )
            if (subtitle != null) {
                Text(subtitle, style = RobolockType.captionSmall, color = colors.textTertiary)
            }
        }
        RobolockSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            activeColor = activeColor,
        )
    }
}

/** A ranked entry in the "Most blocked" card: app mark, name, count. */
@Composable
fun RankRow(
    name: String,
    count: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        HSpace(14.dp)
        Text(
            text = name,
            style = RobolockType.label,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = count, style = RobolockType.label, color = colors.textPrimary)
    }
}

/** Full-width tappable line with no icon: the "Friction settings" rows. */
@Composable
fun PlainRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = RobolockType.labelStrong, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = RobolockType.caption, color = colors.textSecondary)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Left-aligned label with a value on the right; used by the About screen's version row. */
@Composable
fun ValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconBrush: Brush? = null,
) {
    val colors = RobolockTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(
                size = RobolockTheme.dimens.iconTile,
                radius = RobolockTheme.dimens.iconTileRadius,
                brush = iconBrush,
                background = colors.surfaceElevated,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
            HSpace(14.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = RobolockType.labelStrong, color = colors.textPrimary)
            Text(value, style = RobolockType.caption, color = colors.textSecondary)
        }
    }
}
