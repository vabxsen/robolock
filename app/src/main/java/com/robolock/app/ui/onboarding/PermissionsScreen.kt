package com.robolock.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.robolock.app.permissions.PermissionState
import com.robolock.app.permissions.RobolockPermission
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.IconTile
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockPrimaryButton
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The permissions step.
 *
 * There is no Accessibility entry here and there never will be. What the app does ask for is
 * stated plainly, together with what it does not do — the reassurance is only worth printing
 * because it is true.
 */
@Composable
fun PermissionsScreen(
    state: PermissionState,
    onRequest: (RobolockPermission) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            Text(
                text = "Almost there!",
                style = RobolockType.display,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "We need a few permissions to help you stay on track. " +
                    "Robolock does not read your messages or screen contents.",
                style = RobolockType.body,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))

            PermissionCard(
                permission = RobolockPermission.USAGE_ACCESS,
                icon = Icons.Outlined.BarChart,
                tint = Color(0xFF34D399),
                granted = state.usageAccess,
                onRequest = onRequest,
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                permission = RobolockPermission.OVERLAY,
                icon = Icons.Outlined.Layers,
                tint = Color(0xFFFB7185),
                granted = state.overlay,
                onRequest = onRequest,
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                permission = RobolockPermission.BATTERY,
                icon = Icons.Outlined.BatteryChargingFull,
                tint = Color(0xFF4ADE80),
                granted = state.batteryUnrestricted,
                onRequest = onRequest,
            )

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
                HSpace(12.dp)
                Text(
                    text = "Your data stays on your device.\nWe don't collect your screen contents.",
                    style = RobolockType.caption,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = dimens.screenPadding)
                .navigationBarsPadding(),
        ) {
            RobolockPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                showArrow = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    permission: RobolockPermission,
    icon: ImageVector,
    tint: Color,
    granted: Boolean,
    onRequest: (RobolockPermission) -> Unit,
) {
    val colors = RobolockTheme.colors

    RobolockCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(size = 46.dp, background = tint.copy(alpha = 0.16f)) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
            HSpace(14.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = permission.title,
                    style = RobolockType.labelStrong,
                    color = colors.textPrimary,
                )
                Text(
                    text = permission.subtitle,
                    style = RobolockType.caption,
                    color = colors.textSecondary,
                )
                // Status comes from the system on every resume, never from a local guess.
                Text(
                    text = if (granted) "Granted" else notGrantedLabel(permission),
                    style = RobolockType.captionSmall,
                    color = if (granted) colors.success else colors.danger,
                )
            }
            HSpace(10.dp)
            if (!granted) {
                PermissionActionButton(
                    label = permission.actionLabel,
                    onClick = { onRequest(permission) },
                )
            }
        }
    }
}

@Composable
private fun PermissionActionButton(label: String, onClick: () -> Unit) {
    com.robolock.app.ui.components.RobolockPrimaryButton(
        text = label,
        onClick = onClick,
        height = 42.dp,
        brush = Brush.horizontalGradient(
            com.robolock.app.ui.theme.RobolockGradients.periwinklePill,
        ),
        modifier = Modifier.width(112.dp),
    )
}

private fun notGrantedLabel(permission: RobolockPermission): String = when (permission) {
    RobolockPermission.BATTERY -> "Not optimized"
    else -> "Not granted"
}
