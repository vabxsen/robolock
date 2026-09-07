package com.robolock.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.SettingsUiState
import com.robolock.app.ui.components.RobolockGroupCard
import com.robolock.app.ui.components.RobolockSwitch
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SettingRow
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    onOpenFocus: () -> Unit,
    onOpenAppControls: () -> Unit,
    onOpenDistractions: () -> Unit,
    onOpenDetectionInfo: () -> Unit,
    onOpenAbout: () -> Unit,
    onSetDailySummary: (Boolean) -> Unit,
    onClearData: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(centerTitle = "Settings")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    title = "Appearance",
                    subtitle = "Dark mode, theme",
                    icon = Icons.Outlined.Palette,
                    showChevron = false,
                    trailingText = state.settings.theme.displayName,
                )
                RowDivider()
                SettingRow(
                    title = "Focus Mode",
                    subtitle = "Mode, schedule",
                    icon = Icons.Outlined.Schedule,
                    onClick = onOpenFocus,
                )
                RowDivider()
                SettingRow(
                    title = "Notifications",
                    subtitle = "Daily summary",
                    icon = Icons.Outlined.Notifications,
                    showChevron = false,
                    trailing = {
                        RobolockSwitch(
                            checked = state.settings.dailySummaryEnabled,
                            onCheckedChange = onSetDailySummary,
                        )
                    },
                )
                RowDivider()
                SettingRow(
                    title = "App Controls",
                    subtitle = "Manage your apps",
                    icon = Icons.Outlined.Apps,
                    onClick = onOpenAppControls,
                )
                RowDivider()
                SettingRow(
                    title = "Distractions",
                    subtitle = "Choose what you're trying to avoid",
                    icon = Icons.Outlined.Waves,
                    onClick = onOpenDistractions,
                )
            }

            Spacer(Modifier.height(16.dp))

            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    title = "Data & Privacy",
                    subtitle = "Everything stays on this device",
                    icon = Icons.Outlined.Shield,
                    showChevron = false,
                    onClick = onClearData,
                    trailingText = "Clear",
                )
                RowDivider()
                SettingRow(
                    title = "What Robolock can detect",
                    subtitle = "How it works, and its limits",
                    icon = Icons.Outlined.HelpOutline,
                    onClick = onOpenDetectionInfo,
                )
                RowDivider()
                SettingRow(
                    title = "About Robolock",
                    subtitle = "Version $versionName",
                    icon = Icons.Outlined.Info,
                    onClick = onOpenAbout,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Block the rabbit holes. Keep the apps.",
                style = RobolockType.quiet,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}
