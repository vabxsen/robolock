package com.robolock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.robolock.app.ui.RobolockViewModelFactory
import com.robolock.app.ui.SettingsViewModel
import com.robolock.app.ui.navigation.RobolockNavHost
import com.robolock.app.ui.navigation.Routes
import com.robolock.app.ui.theme.RobolockTheme

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels {
        RobolockViewModelFactory.create(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by settingsViewModel.state.collectAsStateWithLifecycle()

            RobolockTheme(preference = state.settings.theme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(RobolockTheme.colors.background),
                ) {
                    // Returning users land on Home; the welcome flow only runs once.
                    RobolockNavHost(
                        startDestination = if (state.settings.onboardingComplete) {
                            Routes.HOME
                        } else {
                            Routes.SPLASH
                        },
                        versionName = BuildConfig.VERSION_NAME,
                    )
                }
            }
        }
    }
}
