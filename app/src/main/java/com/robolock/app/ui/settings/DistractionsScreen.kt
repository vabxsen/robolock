package com.robolock.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.SettingsUiState
import com.robolock.app.ui.SurfacePreference
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockGroupCard
import com.robolock.app.ui.components.RobolockSwitch
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SettingRow
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import com.robolock.rules.DetectionCapability
import com.robolock.rules.DistractingSurface

/**
 * "Choose what you're trying to avoid".
 *
 * The delicate screen. These are preferences, and for some of them Robolock genuinely cannot tell
 * when the thing happens — so each row says which it is, once, without plastering warnings over
 * the rest of the app. A surface marked undetectable shapes the reminders and the link handling
 * and nothing else; it can never generate a statistic.
 */
@Composable
fun DistractionsScreen(
    state: SettingsUiState,
    onToggle: (DistractingSurface, Boolean) -> Unit,
    onSetInterceptLinks: (Boolean) -> Unit,
    onOpenDetectionInfo: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "Distractions")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            Text(
                text = "Choose what you're trying to avoid",
                style = RobolockType.sectionHeader,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "These shape the reminders Robolock shows you and how it handles links " +
                    "you open.",
                style = RobolockType.body,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(18.dp))

            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                state.surfaces.forEachIndexed { index, pref ->
                    if (index > 0) RowDivider()
                    SurfaceRow(pref, onToggle)
                }
            }

            Spacer(Modifier.height(16.dp))

            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    title = "Intercept Shorts and Reels links",
                    subtitle = "Ask before opening a short-video link you tap or share",
                    icon = Icons.Outlined.Link,
                    showChevron = false,
                    trailing = {
                        RobolockSwitch(
                            checked = state.settings.interceptLinks,
                            onCheckedChange = onSetInterceptLinks,
                        )
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            RobolockCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenDetectionInfo) {
                Text(
                    text = "Why can't Robolock see everything?",
                    style = RobolockType.labelStrong,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Robolock never reads what's on your screen. That keeps it private, " +
                        "but it also means it can't always tell which part of an app you're in. " +
                        "Tap to read what it can and can't do.",
                    style = RobolockType.caption,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SurfaceRow(
    pref: SurfacePreference,
    onToggle: (DistractingSurface, Boolean) -> Unit,
) {
    val colors = RobolockTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = pref.surface.displayName,
                style = RobolockType.label,
                color = colors.textPrimary,
            )
            Text(
                text = capabilityLabel(pref.surface.capability),
                style = RobolockType.captionSmall,
                color = if (pref.detectable) colors.textTertiary else colors.accentWarm,
            )
        }
        HSpace(10.dp)
        RobolockSwitch(
            checked = pref.selected,
            onCheckedChange = { onToggle(pref.surface, it) },
        )
    }
}

/**
 * What Robolock can honestly say about a surface.
 *
 * Deliberately phrased as a plain-language capability rather than an apology or a technical
 * error — this is a fact about Android, not a fault the user needs to fix.
 */
private fun capabilityLabel(capability: DetectionCapability): String = when (capability) {
    DetectionCapability.APP_LEVEL -> "Detected when the app is open"
    DetectionCapability.DEEP_LINK -> "Detected from links you open"
    DetectionCapability.NETWORK_HINT -> "Detected from network activity"
    DetectionCapability.UNSUPPORTED -> "Can't detect this yet — reminders only"
}
