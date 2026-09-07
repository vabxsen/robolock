package com.robolock.app.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robolock.app.ui.HomeUiState
import com.robolock.app.ui.components.AppIcon
import com.robolock.app.ui.components.CircularProgressRing
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.IconAction
import com.robolock.app.ui.components.MetricCard
import com.robolock.app.ui.components.QuoteCard
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockMark
import com.robolock.app.ui.components.SectionHeader
import com.robolock.app.ui.components.WeeklyCapsuleChart
import com.robolock.app.ui.components.animatedCount
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The dashboard.
 *
 * Every number here comes from the local event log, so a fresh install shows zeros rather than
 * invented activity, and the focus rate shows a dash until there is something to divide.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenAppControls: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The mascot itself, not a generic badge — the reference puts the brand mark here.
            RobolockMark(size = 40.dp, showGlow = false)
            HSpace(10.dp)
            Text(
                text = "Robolock",
                style = RobolockType.topBar.copy(fontSize = 24.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconAction(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(horizontal = dimens.screenPadding)) {
                Text(
                    text = "Good to see you back 👋",
                    style = RobolockType.greeting,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Same apps. Less distractions.",
                    style = RobolockType.body,
                    color = colors.textSecondary,
                )
            }

            // Shown only when the app cannot do what the switch claims it is doing.
            if (state.needsAttention) {
                Spacer(Modifier.height(16.dp))
                PermissionBanner(
                    onClick = onFixPermissions,
                    modifier = Modifier.padding(horizontal = dimens.screenPadding),
                )
            }

            Spacer(Modifier.height(24.dp))

            IntentionalSessionsRing(state)

            Spacer(Modifier.height(26.dp))

            WeeklyCapsuleChart(
                buckets = state.week.buckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPadding),
            )

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    value = state.week.formattedTimeReclaimed,
                    label = "Time reclaimed",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    value = state.week.exits.toString(),
                    label = "Intentional exits",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    // A dash, not a zero: with nothing recorded there is no rate to report.
                    value = state.week.focusRate?.let { "$it%" } ?: "—",
                    label = "Focus rate",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(26.dp))

            SectionHeader(
                text = "Active protections",
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(10.dp))
            ActiveProtections(state, onOpenAppControls)

            Spacer(Modifier.height(22.dp))

            QuoteCard(
                firstLine = "Small choices make a bigger you.",
                secondLine = "Keep going.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPadding),
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun IntentionalSessionsRing(state: HomeUiState) {
    val colors = RobolockTheme.colors

    // The ring fills against a soft daily target. It is a visual scale, not a claim about a goal
    // the user set, so it is never labelled as one.
    val target = 12f
    val count = state.today.exits
    val progress = (count / target).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressRing(progress = progress, diameter = 208.dp, strokeWidth = 16.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = animatedCount(count).toString(),
                    style = RobolockType.metric,
                    color = colors.textPrimary,
                )
                Text(
                    text = "intentional\nsessions today",
                    style = RobolockType.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ActiveProtections(state: HomeUiState, onOpen: () -> Unit) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens
    val installed = state.apps.filter { it.installed }

    RobolockCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding),
        onClick = onOpen,
    ) {
        if (installed.isEmpty()) {
            Text(
                text = "None of the supported apps are installed yet.",
                style = RobolockType.body,
                color = colors.textSecondary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                installed.take(5).forEach { app ->
                    AppIcon(
                        packageName = app.app.packageName,
                        icon = app.icon,
                        size = 42.dp,
                    )
                    HSpace(10.dp)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open app controls",
                    tint = colors.textTertiary,
                )
            }
        }
    }
}

/**
 * The banner shown when protection is switched on but cannot actually run.
 *
 * This is the single most important honesty affordance on the dashboard: without it the app would
 * look like it was working while doing nothing at all.
 */
@Composable
private fun PermissionBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RobolockTheme.colors
    RobolockCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = colors.danger.copy(alpha = 0.5f),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = colors.danger,
            )
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Protection isn't running",
                    style = RobolockType.labelStrong,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Usage Access is off, so Robolock can't see which apps you're using.",
                    style = RobolockType.caption,
                    color = colors.textSecondary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
            )
        }
    }
}
