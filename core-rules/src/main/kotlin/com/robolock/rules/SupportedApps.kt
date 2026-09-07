package com.robolock.rules

object SupportedPackages {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val WHATSAPP = "com.whatsapp"
}

/**
 * A third-party app Robolock can apply limits to.
 *
 * Adding an app means adding an entry here. Nothing else needs to change: the controls screen,
 * the settings storage, the monitor's watch list and the statistics are all driven off this
 * registry, which is also why the debug build can point it at apps that exist on an emulator.
 */
data class SupportedApp(
    val packageName: String,
    val displayName: String,
    /** Brand accent, used for the fallback tile when the real icon cannot be loaded. */
    val brandColor: Long,
    /** One line under the app name on the controls screen. */
    val controlTagline: String,
)

interface SupportedAppRegistry {
    val apps: List<SupportedApp>

    fun byPackage(packageName: String): SupportedApp? = apps.firstOrNull { it.packageName == packageName }

    val packages: Set<String> get() = apps.map { it.packageName }.toSet()
}

object DefaultSupportedApps : SupportedAppRegistry {
    override val apps: List<SupportedApp> = listOf(
        SupportedApp(
            packageName = SupportedPackages.INSTAGRAM,
            displayName = "Instagram",
            brandColor = 0xFFE1306C,
            controlTagline = "Less mindless scrolling.",
        ),
        SupportedApp(
            packageName = SupportedPackages.YOUTUBE,
            displayName = "YouTube",
            brandColor = 0xFFFF0000,
            controlTagline = "Watch with intention.",
        ),
        SupportedApp(
            packageName = SupportedPackages.WHATSAPP,
            displayName = "WhatsApp",
            brandColor = 0xFF25D366,
            controlTagline = "Stay in touch on your terms.",
        ),
    )
}
