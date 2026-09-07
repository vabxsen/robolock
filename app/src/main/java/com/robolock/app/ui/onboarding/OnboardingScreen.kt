package com.robolock.app.ui.onboarding

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolock.app.data.apps.InstalledAppState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.robolock.app.ui.components.AppIcon
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.IconTile
import com.robolock.app.ui.components.NightRidgeScene
import com.robolock.app.ui.components.PagerDots
import com.robolock.app.ui.components.RobolockGroupCard
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SunsetLakeScene
import com.robolock.app.ui.components.WaveScaffold
import com.robolock.app.ui.theme.RobolockGradients
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * Onboarding.
 *
 * Two pages of argument before the permissions step: what Robolock leaves alone, and what the
 * time it gives back is for. It never claims to block a specific feed, because it can't.
 */
@Composable
fun OnboardingScreen(
    page: Int,
    pageCount: Int,
    apps: List<InstalledAppState>,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    WaveScaffold(waveHeight = 240.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = dimens.screenPadding),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Skip",
                    style = RobolockType.label,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clickable(onClick = onSkip)
                        .padding(8.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            when (page) {
                0 -> PageOne(apps)
                else -> PageTwo()
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PagerDots(count = pageCount, selectedIndex = page)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(RobolockGradients.primaryPill))
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageOne(apps: List<InstalledAppState>) {
    val colors = RobolockTheme.colors

    Text(
        text = "Same apps.\nLess distractions.",
        style = RobolockType.display,
        color = colors.textPrimary,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Use Instagram, YouTube and WhatsApp normally — Robolock helps you reduce the " +
            "parts that consume your time.",
        style = RobolockType.body,
        color = colors.textSecondary,
    )

    Spacer(Modifier.height(30.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        apps.take(3).forEach { app ->
            AppIcon(packageName = app.app.packageName, icon = app.icon, size = 68.dp, cornerRadius = 18.dp)
        }
    }

    Spacer(Modifier.height(30.dp))

    RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
        BenefitRow(Icons.Outlined.Chat, Color(0xFF3E86F7), "Keep chatting", "Stay connected with what matters")
        RowDivider()
        BenefitRow(Icons.Outlined.PlayCircleOutline, Color(0xFFFB7185), "Watch what you want", "Enjoy content intentionally")
        RowDivider()
        BenefitRow(Icons.Outlined.Timelapse, Color(0xFF34D399), "Reduce time-wasters", "Break the endless scroll")
    }
}

@Composable
private fun PageTwo() {
    val colors = RobolockTheme.colors

    Text(
        text = "More time\nfor what matters.",
        style = RobolockType.display,
        color = colors.textPrimary,
    )
    Spacer(Modifier.height(26.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PurposeCard("Study", "Be more productive", Modifier.weight(1f)) {
                NightRidgeScene(Modifier.fillMaxSize(), horizon = 0.72f, showPines = false)
            }
            PurposeCard("Create", "Turn ideas into reality", Modifier.weight(1f)) {
                SunsetLakeScene(Modifier.fillMaxSize(), horizon = 0.58f)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PurposeCard("Play", "Enjoy guilt-free", Modifier.weight(1f)) {
                // A high horizon gives this one mostly sky, so it reads differently from
                // "Create" even though both are the same scene.
                SunsetLakeScene(Modifier.fillMaxSize(), horizon = 0.34f)
            }
            PurposeCard("Be present", "In the real world", Modifier.weight(1f)) {
                NightRidgeScene(Modifier.fillMaxSize(), horizon = 0.55f)
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    Text(
        text = "“A better you is a more intentional you.”",
        style = RobolockType.script,
        color = colors.textSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

/**
 * An illustrated card.
 *
 * The scene is painted procedurally rather than shipped as a bitmap, so it costs nothing in the
 * APK and stays sharp on any screen. A scrim under the caption keeps the text legible whatever
 * the artwork happens to be doing behind it.
 */
@Composable
private fun PurposeCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    scene: @Composable () -> Unit,
) {
    val colors = RobolockTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RobolockTheme.dimens.cardRadiusSmall))
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        ) {
            scene()
        }
        Column(Modifier.padding(12.dp)) {
            Text(text = title, style = RobolockType.labelStrong, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = RobolockType.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, tint: Color, title: String, subtitle: String) {
    val colors = RobolockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(size = 42.dp, background = tint.copy(alpha = 0.16f)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
        HSpace(14.dp)
        Column {
            Text(text = title, style = RobolockType.labelStrong, color = colors.textPrimary)
            Text(text = subtitle, style = RobolockType.caption, color = colors.textSecondary)
        }
    }
}
