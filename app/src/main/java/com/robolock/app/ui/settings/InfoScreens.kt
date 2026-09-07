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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockMark
import com.robolock.app.ui.components.RobolockTopBar
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The one place the technical limits are explained in full.
 *
 * Written for someone who does not know what an accessibility service is, and framed as a
 * trade-off Robolock has chosen rather than a defect it is apologising for.
 */
@Composable
fun DetectionInfoScreen(onBack: () -> Unit) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "What Robolock can detect")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            Section(
                title = "What it can see",
                body = "Android tells Robolock which app is open, when you switch apps, and how " +
                    "long you spend in each one. That's enough for session limits, daily budgets, " +
                    "schedules and Focus Mode — the things that do most of the work.",
            )

            Section(
                title = "What it can't see",
                body = "Robolock can't tell which screen you're on inside another app. It knows " +
                    "Instagram is open; it doesn't know whether you're reading messages or " +
                    "watching Reels. Apps that can tell would have to read the contents of your " +
                    "screen, and Robolock doesn't do that.",
            )

            Section(
                title = "The one exception",
                body = "If you tap or share a link that clearly points at a Short or a Reel, " +
                    "Android hands that link to Robolock — so in that case it genuinely does know " +
                    "where you were heading, and can offer you a pause first.",
            )

            Section(
                title = "Why it's built this way",
                body = "Reading another app's screen would mean Robolock could see your messages, " +
                    "your passwords and everything else you look at. We decided that price was too " +
                    "high, so anything Robolock can't honestly detect is marked as such instead of " +
                    "being quietly guessed at.",
            )

            Section(
                title = "How quickly it reacts",
                body = "Robolock usually notices a few seconds after you open an app, so you may " +
                    "see a moment of it before a limit appears. That delay is what keeps battery " +
                    "use low.",
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        RobolockTopBar(onBack = onBack, centerTitle = "About Robolock")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            RobolockMark(size = 120.dp)
            Spacer(Modifier.height(18.dp))

            Text(
                text = "Robolock",
                style = RobolockType.title,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Block the rabbit holes.\nKeep the apps.",
                style = RobolockType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Version $versionName",
                style = RobolockType.captionSmall,
                color = colors.textTertiary,
            )

            Spacer(Modifier.height(26.dp))

            RobolockCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your data stays here",
                    style = RobolockType.labelStrong,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Robolock stores your settings and your own activity on this device. " +
                        "It has no account, sends nothing to a server, and never reads the " +
                        "contents of other apps.",
                    style = RobolockType.body,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    val colors = RobolockTheme.colors
    Spacer(Modifier.height(18.dp))
    Text(text = title, style = RobolockType.sectionHeader, color = colors.textPrimary)
    Spacer(Modifier.height(6.dp))
    Text(text = body, style = RobolockType.body, color = colors.textSecondary)
}
