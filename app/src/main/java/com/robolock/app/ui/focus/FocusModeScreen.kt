package com.robolock.app.ui.focus

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.FocusUiState
import com.robolock.app.ui.components.RadioRow
import com.robolock.app.ui.components.RobolockGroupCard
import com.robolock.app.ui.components.RobolockPrimaryButton
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.components.RowDivider
import com.robolock.app.ui.components.SectionHeader
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import com.robolock.rules.FocusSchedule
import com.robolock.rules.FocusTier

/**
 * Focus Mode.
 *
 * The three tiers are presented as a choice about how much friction someone wants, not as
 * severity levels — the copy describes what each one does rather than ranking them.
 */
@Composable
fun FocusModeScreen(
    state: FocusUiState,
    onSelectTier: (FocusTier) -> Unit,
    onSelectSchedule: (FocusSchedule) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "Focus Mode")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                FocusTier.entries.forEachIndexed { index, tier ->
                    if (index > 0) RowDivider()
                    RadioRow(
                        title = tier.displayName,
                        subtitle = tier.description,
                        icon = iconFor(tier),
                        selected = state.tier == tier,
                        onClick = { onSelectTier(tier) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionHeader(text = "Schedule")
            Spacer(Modifier.height(10.dp))

            RobolockGroupCard(modifier = Modifier.fillMaxWidth()) {
                FocusSchedule.entries.forEachIndexed { index, schedule ->
                    if (index > 0) RowDivider()
                    RadioRow(
                        title = schedule.displayName,
                        subtitle = subtitleFor(schedule, state),
                        icon = Icons.Outlined.Timeline,
                        selected = state.schedule == schedule,
                        onClick = { onSelectSchedule(schedule) },
                    )
                }
            }

            if (state.saved) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Saved.",
                    style = RobolockType.caption,
                    color = colors.success,
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
                text = "Save Changes",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun iconFor(tier: FocusTier) = when (tier) {
    FocusTier.BALANCED -> Icons.Outlined.Balance
    FocusTier.FRICTION -> Icons.Outlined.Timeline
    FocusTier.STRICT -> Icons.Outlined.Lock
}

private fun subtitleFor(schedule: FocusSchedule, state: FocusUiState): String? = when (schedule) {
    FocusSchedule.ALWAYS -> "Focus Mode is on all the time."
    FocusSchedule.SELECTED_HOURS ->
        if (state.windows.isSet) state.windows.summary() else "No hours set yet."
    FocusSchedule.CUSTOM -> "Set your own windows."
}
