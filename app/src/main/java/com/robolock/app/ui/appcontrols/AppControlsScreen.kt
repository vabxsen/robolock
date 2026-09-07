package com.robolock.app.ui.appcontrols

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robolock.app.data.apps.InstalledAppState
import com.robolock.app.data.rules.LimitOptions
import com.robolock.app.ui.AppControlsUiState
import com.robolock.app.ui.components.AppIcon
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockSwitch
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SettingRow
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import com.robolock.rules.AppRules

/**
 * Per-app limits.
 *
 * Every switch and row on this screen is wired to a rule the engine actually reads. Nothing here
 * is decorative, and an app that is not installed says so rather than offering controls that
 * could never fire.
 */
@Composable
fun AppControlsScreen(
    state: AppControlsUiState,
    onSetEnabled: (String, Boolean) -> Unit,
    onSetSessionLimit: (String, Long?) -> Unit,
    onSetDailyLimit: (String, Long?) -> Unit,
    onSetLaunchFriction: (String, Boolean) -> Unit,
    onOpenDistractions: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val dimens = RobolockTheme.dimens
    val colors = RobolockTheme.colors

    var picker by remember { mutableStateOf<LimitPicker?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "App Controls")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            Text(
                text = "Set limits and add friction for the apps that distract you most.",
                style = RobolockType.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(18.dp))

            state.apps.forEach { app ->
                val rules = state.rules[app.app.packageName] ?: AppRules(app.app.packageName)
                AppControlCard(
                    app = app,
                    rules = rules,
                    onSetEnabled = onSetEnabled,
                    onSetLaunchFriction = onSetLaunchFriction,
                    onPickSession = { picker = LimitPicker(app.app.packageName, session = true) },
                    onPickDaily = { picker = LimitPicker(app.app.packageName, session = false) },
                )
                Spacer(Modifier.height(14.dp))
            }

            SettingRow(
                title = "Distractions",
                subtitle = "Choose what you're trying to avoid",
                icon = Icons.Outlined.Waves,
                onClick = onOpenDistractions,
            )

            Spacer(Modifier.height(24.dp))
        }

        Spacer(Modifier.navigationBarsPadding())
    }

    picker?.let { active ->
        LimitPickerDialog(
            title = if (active.session) "Session limit" else "Daily limit",
            minutes = if (active.session) LimitOptions.sessionMinutes else LimitOptions.dailyMinutes,
            onPick = { millis ->
                if (active.session) onSetSessionLimit(active.packageName, millis)
                else onSetDailyLimit(active.packageName, millis)
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

private data class LimitPicker(val packageName: String, val session: Boolean)

@Composable
private fun AppControlCard(
    app: InstalledAppState,
    rules: AppRules,
    onSetEnabled: (String, Boolean) -> Unit,
    onSetLaunchFriction: (String, Boolean) -> Unit,
    onPickSession: () -> Unit,
    onPickDaily: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val pkg = app.app.packageName

    RobolockCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = pkg, icon = app.icon, size = 46.dp)
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.app.displayName,
                    style = RobolockType.labelStrong,
                    color = colors.textPrimary,
                )
                Text(
                    // Showing the package is a small honesty: it says exactly what is being watched.
                    text = if (app.installed) pkg else "Not installed",
                    style = RobolockType.captionSmall,
                    color = if (app.installed) colors.textTertiary else colors.danger,
                )
            }
            RobolockSwitch(
                checked = rules.enabled,
                onCheckedChange = { onSetEnabled(pkg, it) },
                enabled = app.installed,
            )
        }

        if (rules.enabled && app.installed) {
            RowDivider()
            SettingRow(
                title = "Session limit",
                icon = Icons.Outlined.Timer,
                trailingText = LimitOptions.label(rules.sessionLimitMillis),
                onClick = onPickSession,
            )
            RowDivider()
            SettingRow(
                title = "Daily limit",
                icon = Icons.Outlined.HourglassEmpty,
                trailingText = LimitOptions.label(rules.dailyLimitMillis),
                onClick = onPickDaily,
            )
            RowDivider()
            SettingRow(
                title = "Launch friction",
                icon = Icons.Outlined.Waves,
                showChevron = false,
                trailing = {
                    RobolockSwitch(
                        checked = rules.launchFriction,
                        onCheckedChange = { onSetLaunchFriction(pkg, it) },
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Schedule",
                icon = Icons.Outlined.CalendarMonth,
                trailingText = rules.schedule.summary(),
                showChevron = false,
            )
        }
    }
}

@Composable
private fun LimitPickerDialog(
    title: String,
    minutes: List<Int>,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = RobolockTheme.colors

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        RobolockCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = RobolockType.sectionHeader, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            SettingRow(
                title = "Unlimited",
                showChevron = false,
                onClick = { onPick(null) },
            )
            minutes.forEach { m ->
                RowDivider()
                SettingRow(
                    title = LimitOptions.label(m * 60_000L),
                    showChevron = false,
                    onClick = { onPick(m * 60_000L) },
                )
            }
        }
    }
}
