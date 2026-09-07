package com.robolock.app.blocking

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolock.app.RobolockApplication
import com.robolock.app.core.log.RLog
import com.robolock.app.ui.components.RobolockCard
import com.robolock.app.ui.components.RobolockPrimaryButton
import com.robolock.app.ui.components.RobolockSecondaryButton
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType
import com.robolock.rules.DeepLinkMatch
import com.robolock.rules.DeepLinkRuleEngine
import com.robolock.rules.DeepLinkEvidence
import com.robolock.rules.InterventionAction
import com.robolock.rules.InterventionReason
import com.robolock.rules.toIntervention
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles a short-video link the user opened or shared.
 *
 * This is the one place Robolock legitimately knows *what* is about to open rather than merely
 * which app: Android handed it the URL. The intervention is therefore attributed to the specific
 * surface, and it is a real Activity rather than an overlay because we are already in the
 * foreground here.
 *
 * Reached only through an activity-alias that is disabled until the user opts in, so Robolock
 * never quietly becomes the default handler for anybody's links.
 */
class LinkInterceptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val url = extractUrl(intent)
        val match = url?.let { DeepLinkRuleEngine.match(it) }

        if (url == null || match == null) {
            // Not something we claim to understand: get out of the way immediately.
            RLog.d("Link not recognised, passing through: $url")
            url?.let { forward(it) }
            finish()
            return
        }

        setContent {
            RobolockTheme {
                LinkInterceptScreen(
                    match = match,
                    onOpen = {
                        record(match, InterventionAction.CONTINUED)
                        forward(match.uri)
                        finish()
                    },
                    onCancel = {
                        record(match, InterventionAction.EXITED)
                        finish()
                    },
                )
            }
        }
    }

    private fun extractUrl(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data?.toString()
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull { it.startsWith("http") }
        else -> null
    }

    /**
     * Sends the link on to whoever would normally have handled it.
     *
     * Our own alias is excluded so this cannot bounce straight back into Robolock.
     */
    private fun forward(url: String) {
        val outgoing = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            val chooser = Intent.createChooser(outgoing, null).apply {
                putExtra(
                    Intent.EXTRA_EXCLUDE_COMPONENTS,
                    arrayOf(android.content.ComponentName(this@LinkInterceptActivity, ALIAS)),
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            RLog.w("Nothing could open $url", e)
        }
    }

    private fun record(match: DeepLinkMatch, action: InterventionAction) {
        val container = (application as RobolockApplication).container
        val evidence = DeepLinkEvidence(
            surface = match.surface,
            atUtcMillis = container.clock.nowUtcMillis(),
            uri = match.uri,
        )
        lifecycleScope.launch {
            container.statistics.record(
                evidence.toIntervention(InterventionReason.DEEP_LINK, action),
            )
        }
    }

    companion object {
        const val ALIAS = "com.robolock.app.blocking.LinkCatcher"
    }
}

@Composable
private fun LinkInterceptScreen(
    match: DeepLinkMatch,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    var remaining by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

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
            Text(
                text = "This is a ${match.surface.shortLabel}.",
                style = RobolockType.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "You chose to pause before these. Open it anyway?",
                style = RobolockType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            RobolockCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = match.uri,
                    style = RobolockType.captionSmall,
                    color = colors.textTertiary,
                )
            }

            Spacer(Modifier.height(26.dp))

            RobolockPrimaryButton(
                text = "Not now",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            RobolockSecondaryButton(
                text = if (remaining > 0) "Open anyway (${remaining}s)" else "Open anyway",
                onClick = onOpen,
                enabled = remaining == 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
