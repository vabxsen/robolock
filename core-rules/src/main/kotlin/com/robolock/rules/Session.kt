package com.robolock.rules

/** Why a sitting ended. Recorded so gaps caused by crashes are visible rather than silently lost. */
enum class SessionEndReason(val id: String) {
    LEFT_APP("left_app"),
    SCREEN_OFF("screen_off"),
    MIDNIGHT("midnight"),
    REBOOT("reboot"),
    CRASH("crash"),
    PROTECTION_OFF("protection_off"),
}

data class ActiveSession(
    val packageName: String,
    val startedAtUtc: Long,
    val foregroundMillis: Long,
    val lastSeenUtc: Long,
    /** Set while the user is briefly elsewhere; a short excursion does not end the sitting. */
    val awaySinceUtc: Long? = null,
    val screenOffSinceUtc: Long? = null,
)

data class ClosedSession(
    val packageName: String,
    val startedAtUtc: Long,
    val endedAtUtc: Long,
    val foregroundMillis: Long,
    val reason: SessionEndReason,
)

sealed interface SegmenterResult {
    data class Continuing(val session: ActiveSession) : SegmenterResult
    data class Closed(val closed: ClosedSession, val next: ActiveSession?) : SegmenterResult
    data object Idle : SegmenterResult
}

/**
 * Turns a stream of foreground observations into sittings.
 *
 * The tolerances matter more than they look. Without them, hopping to the keyboard, glancing at a
 * notification or pocketing the phone for a moment would each start a fresh session and silently
 * reset every session limit — which is exactly how a user would defeat the feature by accident.
 */
class SessionSegmenter(
    private val awayToleranceMillis: Long = 30_000L,
    private val screenOffToleranceMillis: Long = 60_000L,
) {

    fun onForeground(
        current: ActiveSession?,
        packageName: String,
        nowUtc: Long,
    ): SegmenterResult {
        if (current == null) {
            return SegmenterResult.Continuing(
                ActiveSession(packageName, nowUtc, 0L, nowUtc),
            )
        }

        if (current.packageName == packageName) {
            // Returning from a tolerated excursion: resume without crediting the time away.
            val resumed = current.copy(
                foregroundMillis = current.foregroundMillis +
                    if (current.awaySinceUtc == null && current.screenOffSinceUtc == null) {
                        (nowUtc - current.lastSeenUtc).coerceAtLeast(0)
                    } else 0L,
                lastSeenUtc = nowUtc,
                awaySinceUtc = null,
                screenOffSinceUtc = null,
            )
            return SegmenterResult.Continuing(resumed)
        }

        // A different app is in front. Hold the old session open briefly in case they come back.
        val away = current.awaySinceUtc
        if (away == null) {
            return SegmenterResult.Continuing(
                current.copy(awaySinceUtc = nowUtc, lastSeenUtc = nowUtc),
            )
        }
        if (nowUtc - away < awayToleranceMillis) {
            return SegmenterResult.Continuing(current.copy(lastSeenUtc = nowUtc))
        }

        return SegmenterResult.Closed(
            closed = close(current, away, SessionEndReason.LEFT_APP),
            next = ActiveSession(packageName, nowUtc, 0L, nowUtc),
        )
    }

    fun onScreenOff(current: ActiveSession?, nowUtc: Long): SegmenterResult {
        if (current == null) return SegmenterResult.Idle
        return SegmenterResult.Continuing(
            current.copy(screenOffSinceUtc = current.screenOffSinceUtc ?: nowUtc),
        )
    }

    /** Called on a tick to retire sessions whose tolerance has run out while nothing else happened. */
    fun onTick(current: ActiveSession?, nowUtc: Long): SegmenterResult {
        if (current == null) return SegmenterResult.Idle

        current.screenOffSinceUtc?.let { off ->
            if (nowUtc - off >= screenOffToleranceMillis) {
                return SegmenterResult.Closed(close(current, off, SessionEndReason.SCREEN_OFF), null)
            }
            return SegmenterResult.Continuing(current)
        }
        current.awaySinceUtc?.let { away ->
            if (nowUtc - away >= awayToleranceMillis) {
                return SegmenterResult.Closed(close(current, away, SessionEndReason.LEFT_APP), null)
            }
            return SegmenterResult.Continuing(current)
        }

        return SegmenterResult.Continuing(
            current.copy(
                foregroundMillis = current.foregroundMillis + (nowUtc - current.lastSeenUtc).coerceAtLeast(0),
                lastSeenUtc = nowUtc,
            ),
        )
    }

    fun forceClose(current: ActiveSession, atUtc: Long, reason: SessionEndReason): ClosedSession =
        close(current, atUtc, reason)

    /** Ends the sitting at the moment it really stopped being used, not when we noticed. */
    private fun close(session: ActiveSession, endedAtUtc: Long, reason: SessionEndReason): ClosedSession {
        val boundary = session.awaySinceUtc ?: session.screenOffSinceUtc ?: endedAtUtc
        val credited = session.foregroundMillis +
            (boundary - session.lastSeenUtc).coerceAtLeast(0)
        return ClosedSession(
            packageName = session.packageName,
            startedAtUtc = session.startedAtUtc,
            endedAtUtc = endedAtUtc,
            foregroundMillis = credited,
            reason = reason,
        )
    }
}
