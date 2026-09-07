package com.robolock.rules

/**
 * How much Robolock can actually observe about a given surface.
 *
 * Robolock does not use an AccessibilityService, so Android never tells it which screen is
 * currently displayed inside another app. Everything here follows from that: the capability is a
 * statement about the evidence Android is willing to hand us, not about how badly we want it.
 */
enum class DetectionCapability {
    /** Android reports the app moved to the foreground. This is all UsageStats can tell us. */
    APP_LEVEL,

    /** A URI was explicitly routed to Robolock, so the destination is known from the link itself. */
    DEEP_LINK,

    /** Reserved. No shipped surface uses this; see SurfaceCatalogTest. */
    NETWORK_HINT,

    /** Recognisable to a human, invisible to Robolock. Modelled so the UI can say so honestly. */
    UNSUPPORTED,
}

/**
 * A section of a third-party app a user may want to spend less time in.
 *
 * The subtypes are not decoration. Each detection path accepts only the surface type it can
 * genuinely observe, so "Reels were blocked" cannot be recorded unless a Reels link actually
 * arrived. See [DetectionEvidence].
 */
sealed interface DistractingSurface {
    /** Stable storage key. Written to Room and DataStore, so it must never change once shipped. */
    val id: String
    val appPackage: String
    val displayName: String
    val capability: DetectionCapability

    /** Singular form for sentences like "This is a Short." Defaults to the display name. */
    val shortLabel: String get() = displayName
}

/** Observable because Android reports foreground app transitions. */
sealed interface AppLevelSurface : DistractingSurface

/** Observable only when Android hands Robolock a matching URI. */
sealed interface DeepLinkSurface : DistractingSurface

/**
 * Not observable without reading another app's screen, which Robolock will not do.
 *
 * These still exist as user preferences: someone can say they want less of it, and that shapes
 * the copy and the intentional-use prompts. It must never shape the statistics.
 */
sealed interface UnsupportedSurface : DistractingSurface

// --- The catalog -------------------------------------------------------------------------------

/**
 * The app itself, as a surface.
 *
 * Constructible for any package, and that is correct rather than lax: UsageStats will honestly
 * report a foreground transition for *any* installed app, so app-level evidence for one is never
 * a claim we cannot support. It also means adding a supported app needs no edit here, and the
 * debug build's stand-in apps behave exactly like the real ones.
 *
 * What stays impossible is the interesting part — no constructor anywhere accepts an
 * [UnsupportedSurface], so this openness cannot be used to smuggle in a fabricated Reels event.
 */
data class AppSurface(
    override val appPackage: String,
    override val displayName: String,
) : AppLevelSurface {
    override val id: String = "app:$appPackage"
    override val capability = DetectionCapability.APP_LEVEL
}

val InstagramApp = AppSurface(SupportedPackages.INSTAGRAM, "Instagram")
val YouTubeApp = AppSurface(SupportedPackages.YOUTUBE, "YouTube")
val WhatsAppApp = AppSurface(SupportedPackages.WHATSAPP, "WhatsApp")

/** Reels have public URLs, so a shared or intercepted link is honest evidence. In-app browsing is not. */
data object InstagramReels : DeepLinkSurface {
    override val id = "instagram_reels"
    override val appPackage = SupportedPackages.INSTAGRAM
    override val displayName = "Instagram Reels"
    override val shortLabel = "Reel"
    override val capability = DetectionCapability.DEEP_LINK
}

data object YouTubeShorts : DeepLinkSurface {
    override val id = "youtube_shorts"
    override val appPackage = SupportedPackages.YOUTUBE
    override val displayName = "YouTube Shorts"
    override val shortLabel = "Short"
    override val capability = DetectionCapability.DEEP_LINK
}

/** Status has no public URL form and lives entirely inside the app. Nothing legitimate observes it. */
data object WhatsAppStatus : UnsupportedSurface {
    override val id = "whatsapp_status"
    override val appPackage = SupportedPackages.WHATSAPP
    override val displayName = "WhatsApp Status"
    override val capability = DetectionCapability.UNSUPPORTED
}

object SurfaceCatalog {

    /** Surfaces that name a specific section of an app, rather than the app as a whole. */
    val namedSurfaces: List<DistractingSurface> = listOf(
        InstagramReels, YouTubeShorts, WhatsAppStatus,
    )

    val all: List<DistractingSurface> = listOf(
        InstagramApp, YouTubeApp, WhatsAppApp,
    ) + namedSurfaces

    /** The ones a user picks between on the Distractions screen. Whole-app entries are not choices. */
    val selectable: List<DistractingSurface> = namedSurfaces

    fun byId(id: String): DistractingSurface? {
        all.firstOrNull { it.id == id }?.let { return it }
        // App surfaces are derived, so one for an app outside the built-in list is still valid.
        val pkg = id.removePrefix(APP_PREFIX)
        return if (id.startsWith(APP_PREFIX) && pkg.isNotEmpty()) AppSurface(pkg, pkg) else null
    }

    /** The whole-app surface for a package. Always available, because UsageStats always reports it. */
    fun appSurfaceFor(packageName: String, displayName: String = packageName): AppSurface =
        all.filterIsInstance<AppSurface>().firstOrNull { it.appPackage == packageName }
            ?: AppSurface(packageName, displayName)

    fun namedSurfacesFor(pkg: String): List<DistractingSurface> =
        namedSurfaces.filter { it.appPackage == pkg }

    private const val APP_PREFIX = "app:"
}
