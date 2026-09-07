package com.robolock.rules

/** Why Robolock interrupted. Persisted, so the ids are storage keys. */
enum class InterventionReason(val id: String) {
    SESSION_LIMIT("session_limit"),
    DAILY_LIMIT("daily_limit"),
    FOCUS_MODE("focus_mode"),
    LAUNCH_FRICTION("launch_friction"),
    SCHEDULE("schedule"),
    DEEP_LINK("deep_link");

    companion object {
        fun fromId(id: String): InterventionReason? = entries.firstOrNull { it.id == id }
    }
}

/** What the person did about it. */
enum class InterventionAction(val id: String) {
    /** Left the app. The outcome Robolock exists to produce. */
    EXITED("exited"),

    /** Chose to continue after the countdown. A legitimate choice, recorded as such. */
    CONTINUED("continued"),

    /** Neither — the overlay aged out on its own. Counts as neither a win nor a bypass. */
    TIMED_OUT("timed_out");

    companion object {
        fun fromId(id: String): InterventionAction? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Proof that a surface was genuinely observed.
 *
 * This is the only way to produce an [ObservedIntervention], and each subtype accepts only the
 * surface kind whose evidence it actually represents. An [UnsupportedSurface] therefore has no
 * constructor that will take it: writing a fabricated "Status blocked" statistic is a compile
 * error rather than a thing we promise not to do.
 */
sealed interface DetectionEvidence {
    val surface: DistractingSurface
    val atUtcMillis: Long
}

/** Android reported this app moved to the foreground. Says nothing about what is on screen. */
class AppForegroundEvidence(
    override val surface: AppLevelSurface,
    override val atUtcMillis: Long,
    val sessionMillisBefore: Long,
) : DetectionEvidence

/** Android handed Robolock this URI, so the destination is known from the link itself. */
class DeepLinkEvidence(
    override val surface: DeepLinkSurface,
    override val atUtcMillis: Long,
    val uri: String,
) : DetectionEvidence

/**
 * A recorded intervention.
 *
 * The constructor is internal to this module on purpose: the app module can read and persist these
 * but cannot invent one. They originate only from [DetectionEvidence.toIntervention].
 */
class ObservedIntervention internal constructor(
    val surfaceId: String,
    val packageName: String,
    val timestampUtc: Long,
    val reason: InterventionReason,
    val action: InterventionAction,
    val sessionMillisBefore: Long,
    val bypassMillis: Long?,
)

fun DetectionEvidence.toIntervention(
    reason: InterventionReason,
    action: InterventionAction,
    bypassMillis: Long? = null,
): ObservedIntervention = ObservedIntervention(
    surfaceId = surface.id,
    packageName = surface.appPackage,
    timestampUtc = atUtcMillis,
    reason = reason,
    action = action,
    sessionMillisBefore = (this as? AppForegroundEvidence)?.sessionMillisBefore ?: 0L,
    bypassMillis = bypassMillis,
)

/**
 * What statistics can say about a surface.
 *
 * [NoData] is deliberately a different thing from a zero count. "We never saw this" and "this
 * happened zero times" are different claims, and the UI must not render the first as the second.
 */
sealed interface SurfaceStats {
    val surface: DistractingSurface

    data class Counted(
        override val surface: DistractingSurface,
        val interventions: Int,
        val exits: Int,
    ) : SurfaceStats

    data class NoData(override val surface: DistractingSurface) : SurfaceStats
}
