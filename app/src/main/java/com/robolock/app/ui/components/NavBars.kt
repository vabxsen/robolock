package com.robolock.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/** A tappable icon with a comfortable touch target but the small visual size of the reference. */
@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val colors = RobolockTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The bar at the top of a pushed screen.
 *
 * The reference puts the back arrow on its own line above a large left-aligned page title, so the
 * default here carries no title at all; [centerTitle] is for the one screen (Statistics) that does
 * pair the arrow with a heading on the same line.
 */
@Composable
fun RobolockTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    centerTitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth().height(52.dp)) {
        if (centerTitle != null) {
            Text(
                text = centerTitle,
                style = RobolockType.topBar,
                color = RobolockTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.padding(start = 0.dp),
                )
            } else {
                Box(Modifier.size(44.dp))
            }
            Box(Modifier.weight(1f))
            if (trailing != null) trailing() else Box(Modifier.size(44.dp))
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String,
)

/**
 * Home / Stats / Focus / Settings.
 *
 * A floating rounded bar rather than an edge-to-edge strip, with the selected tab carrying its own
 * tinted panel behind the icon and label. That panel is what makes the current tab legible at a
 * glance on a near-black ground, where a colour change alone would be too quiet.
 */
@Composable
fun RobolockBottomBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onSelect: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RobolockTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RobolockTheme.dimens.bottomBarHeight)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val tint by animateColorAsState(
                    targetValue = if (selected) colors.accentSky else colors.textPrimary,
                    label = "navTint",
                )
                val panel by animateColorAsState(
                    targetValue = if (selected) colors.accentSky.copy(alpha = 0.14f) else Color.Transparent,
                    label = "navPanel",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(panel)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(item) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(23.dp),
                    )
                    Box(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = RobolockType.captionSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) colors.accentSky else colors.textSecondary,
                    )
                }
            }
        }
    }
}
