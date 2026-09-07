package com.robolock.app.ui.stats

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.StatsPeriod
import com.robolock.app.ui.StatsUiState
import com.robolock.app.ui.components.AppIcon
import com.robolock.app.ui.components.HSpace
import com.robolock.app.ui.components.MetricCard
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SectionHeader
import com.robolock.app.ui.components.SegmentedTabs
import com.robolock.app.ui.components.StatsBarChart
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * "Your Progress".
 *
 * When nothing has been recorded the screen says so in plain words instead of drawing an empty
 * chart that could be mistaken for a real result.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "Your Progress")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            SegmentedTabs(
                options = StatsPeriod.entries.map { it.label },
                selectedIndex = StatsPeriod.entries.indexOf(state.period),
                onSelect = { onSelectPeriod(StatsPeriod.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = state.rangeLabel,
                style = RobolockType.caption,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            if (!state.snapshot.hasData) {
                EmptyState()
            } else {
                Text(
                    text = state.snapshot.formattedTimeReclaimed,
                    style = RobolockType.metricLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Time reclaimed",
                    style = RobolockType.body,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(18.dp))

                StatsBarChart(
                    buckets = state.snapshot.buckets,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        value = state.snapshot.interventions.toString(),
                        label = "Interruptions",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        value = state.snapshot.exits.toString(),
                        label = "Intentional exits",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        value = state.snapshot.focusRate?.let { "$it%" } ?: "—",
                        label = "Focus rate",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(26.dp))

                SectionHeader(text = "Most distracting apps")
                Spacer(Modifier.height(10.dp))
                MostDistractingApps(state)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MostDistractingApps(state: StatsUiState) {
    val colors = RobolockTheme.colors
    val rows = state.snapshot.perApp.take(5)

    RobolockCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        if (rows.isEmpty()) {
            Text(
                text = "No app usage recorded yet.",
                style = RobolockType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(14.dp),
            )
        } else {
            rows.forEachIndexed { index, usage ->
                if (index > 0) RowDivider()
                val app = state.apps.firstOrNull { it.app.packageName == usage.packageName }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(packageName = usage.packageName, icon = app?.icon, size = 34.dp)
                    HSpace(12.dp)
                    Text(
                        text = app?.app?.displayName ?: usage.packageName,
                        style = RobolockType.label,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = usage.formatted,
                        style = RobolockType.label,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = RobolockTheme.colors
    RobolockCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nothing recorded yet",
            style = RobolockType.labelStrong,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Once Robolock steps in for an app you've set a limit on, your progress will " +
                "show up here. Nothing is estimated before then.",
            style = RobolockType.body,
            color = colors.textSecondary,
        )
    }
}
