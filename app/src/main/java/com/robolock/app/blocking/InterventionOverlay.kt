package com.robolock.app.blocking

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robolock.app.ui.components.AppIcon
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockPrimaryButton
import com.robolock.app.ui.components.RobolockSecondaryButton
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import com.robolock.rules.InterventionReason
import com.robolock.rules.SupportedPackages
import com.robolock.rules.TimeReclaimedCalculator
import kotlinx.coroutines.delay

/** Everything the overlay needs. Assembled by the service so the composable stays free of state. */
data class InterventionUiState(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val reason: InterventionReason,
    val sessionMillis: Long,
    val allowContinue: Boolean,
    val countdownSeconds: Int,
)

/**
 * The intervention screen.
 *
 * Written to be a pause, not a punishment: it states plainly what happened, offers the way out
 * first, and never hides the way through. "Continue anyway" is disabled only while the countdown
 * runs, and "Go back" is always available — the overlay must never trap anyone.
 */
@Composable
fun InterventionOverlay(
    state: InterventionUiState,
    onGoBack: () -> Unit,
    onContinue: () -> Unit,
    onAdjustLimits: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens
    val accent = accentFor(state.packageName)

    var remaining by remember(state.packageName) { mutableIntStateOf(state.countdownSeconds) }
    LaunchedEffect(state.packageName, state.countdownSeconds) {
        remaining = state.countdownSeconds
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 260),
        label = "overlayAppear",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(
                packageName = state.packageName,
                icon = state.icon,
                size = 72.dp,
                cornerRadius = 20.dp,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "${state.appName} paused",
                style = RobolockType.overlayTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = subtitleFor(state.reason),
                style = RobolockType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            RobolockCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(
                    icon = Icons.Outlined.Schedule,
                    text = "You've already spent ${TimeReclaimedCalculator.format(state.sessionMillis)} in this session.",
                    accent = accent,
                )
                Spacer(Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Outlined.SelfImprovement,
                    text = "A short break helps you stay in control.",
                    accent = accent,
                )
                if (state.allowContinue) {
                    Spacer(Modifier.height(12.dp))
                    InfoRow(
                        icon = Icons.Outlined.CheckCircle,
                        text = if (remaining > 0) {
                            "You can continue in $remaining seconds."
                        } else {
                            "You can continue whenever you're ready."
                        },
                        accent = accent,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            RobolockPrimaryButton(
                text = "Go back",
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.allowContinue) {
                Spacer(Modifier.height(12.dp))
                RobolockSecondaryButton(
                    text = if (remaining > 0) "Continue anyway (${remaining}s)" else "Continue anyway",
                    onClick = onContinue,
                    enabled = remaining == 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Adjust limits",
                style = RobolockType.label,
                color = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdjustLimits)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = quoteFor(state.packageName),
                style = RobolockType.quiet,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, accent: Color) {
    val colors = RobolockTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        HSpace(12.dp)
        Text(
            text = text,
            style = RobolockType.body.copy(fontSize = 14.sp),
            color = colors.textSecondary,
        )
    }
}

private fun subtitleFor(reason: InterventionReason): String = when (reason) {
    InterventionReason.SESSION_LIMIT -> "Take a quick pause. You've hit your session limit for now."
    InterventionReason.DAILY_LIMIT -> "That's your time for today. Tomorrow is a fresh start."
    InterventionReason.FOCUS_MODE -> "You opened this during a focus period."
    InterventionReason.SCHEDULE -> "This app is on a schedule right now."
    InterventionReason.LAUNCH_FRICTION -> "Before you go in — what did you come here to do?"
    InterventionReason.DEEP_LINK -> "This link opens a short video feed."
}

/** Each app keeps a hint of its own colour so the screen feels placed, not generic. */
private fun accentFor(packageName: String): Color = when (packageName) {
    SupportedPackages.INSTAGRAM -> Color(0xFFE1306C)
    SupportedPackages.YOUTUBE -> Color(0xFFFF4444)
    SupportedPackages.WHATSAPP -> Color(0xFF25D366)
    else -> Color(0xFF3E86F7)
}

private fun quoteFor(packageName: String): String = when (packageName) {
    SupportedPackages.INSTAGRAM -> "Better things are waiting for you."
    SupportedPackages.YOUTUBE -> "Less scrolling. More living."
    SupportedPackages.WHATSAPP -> "Be present, not just online."
    else -> "Small choices make a bigger you."
}

