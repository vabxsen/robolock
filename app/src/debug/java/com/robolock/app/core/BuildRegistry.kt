package com.robolock.app.core

import com.robolock.rules.DefaultSupportedApps
import com.robolock.rules.SupportedApp
import com.robolock.rules.SupportedAppRegistry

/**
 * Debug watch list: the production apps plus stand-ins that exist on a bare emulator.
 *
 * Emulator system images ship without Instagram, YouTube or WhatsApp, so enforcement could not
 * otherwise be exercised end to end. These entries run the real pipeline — real UsageEvents, real
 * overlay, real limits — against apps that are actually present. Nothing here is a stub, and none
 * of it reaches a release build.
 */
internal object BuildRegistry {
    private val standIns = listOf(
        SupportedApp(
            packageName = "com.android.chrome",
            displayName = "Chrome",
            brandColor = 0xFF4285F4,
            controlTagline = "Stand-in for testing limits.",
        ),
        SupportedApp(
            packageName = "com.google.android.deskclock",
            displayName = "Clock",
            brandColor = 0xFF6EA8FF,
            controlTagline = "Stand-in for testing limits.",
        ),
        // Settings is deliberately absent: the overlay must never cover a system permission
        // dialog, so watching Settings would contradict a safety rule the controller enforces.
    )

    val registry: SupportedAppRegistry = object : SupportedAppRegistry {
        override val apps: List<SupportedApp> = DefaultSupportedApps.apps + standIns
    }
}
