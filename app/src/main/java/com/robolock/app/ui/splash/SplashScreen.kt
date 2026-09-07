package com.robolock.app.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.robolock.app.ui.components.RobolockMark
import com.robolock.app.ui.components.RobolockPrimaryButton
import com.robolock.app.ui.components.WaveScaffold
import com.robolock.app.ui.theme.RobolockGradients
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.app.ui.theme.RobolockType

/**
 * The welcome screen.
 *
 * Entrance is a single soft fade-and-rise rather than a sequence of staggered effects — the first
 * thing the app says about itself is that it is calm.
 */
@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    val colors = RobolockTheme.colors
    val dimens = RobolockTheme.dimens

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    // Deliberately animates between two *visible* values. An entrance that starts at zero opacity
    // can leave the whole screen blank if the animation never runs, which is a far worse outcome
    // than a missing flourish — so the floor is 1f-visible content with only a slight rise.
    val rise by animateFloatAsState(
        targetValue = if (shown) 0f else 18f,
        animationSpec = tween(durationMillis = 620),
        label = "splashRise",
    )
    val markScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.94f,
        animationSpec = tween(durationMillis = 620),
        label = "splashMarkScale",
    )

    WaveScaffold(waveHeight = 300.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = dimens.screenPadding)
                // Lambda overload: reads the animated value at layout time rather than
                // recomposing the whole tree on every frame.
                .offset { IntOffset(0, rise.roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Weighted spacers on both sides of the brand group centre it in the upper half,
            // leaving the lower third for the tagline and the button, as the reference does.
            Spacer(Modifier.weight(0.9f))

            RobolockMark(
                size = 168.dp,
                modifier = Modifier.scale(markScale),
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Robolock",
                style = RobolockType.wordmark.copy(
                    brush = Brush.horizontalGradient(RobolockGradients.wordmark),
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Block the rabbit holes.\nKeep the apps.",
                style = RobolockType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1.1f))

            Text(
                text = "Same apps. A better tomorrow.",
                style = RobolockType.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            RobolockPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                showArrow = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}
