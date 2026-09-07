package com.robolock.app.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.robolock.app.core.log.RLog

/** One foreground transition, normalised away from the framework type. */
data class RawUsageEvent(
    val packageName: String,
    val className: String?,
    val timestamp: Long,
    val type: Int,
)

/**
 * Supplies raw usage events.
 *
 * Extracted behind an interface so the parsing below — the part with the actual logic — is
 * testable on the JVM without an Android device. Only the thin adapter is untested.
 */
interface UsageEventSource {
    fun queryEvents(beginTime: Long, endTime: Long): List<RawUsageEvent>
}

class AndroidUsageEventSource(context: Context) : UsageEventSource {

    private val usageStats =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override fun queryEvents(beginTime: Long, endTime: Long): List<RawUsageEvent> {
        val manager = usageStats ?: return emptyList()
        val out = mutableListOf<RawUsageEvent>()
        val event = UsageEvents.Event()

        // Throws SecurityException if Usage Access was revoked while we were running.
        val events = try {
            manager.queryEvents(beginTime, endTime)
        } catch (e: SecurityException) {
            RLog.w("UsageEvents query refused; access likely revoked", e)
            return emptyList()
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            out += RawUsageEvent(
                packageName = event.packageName ?: continue,
                className = event.className,
                timestamp = event.timeStamp,
                type = event.eventType,
            )
        }
        return out
    }
}

/**
 * Works out which app is in front, from a stream of usage events.
 *
 * Two properties of UsageStatsManager shape this design:
 *
 * 1. Events arrive in batches and are not strictly ordered relative to when you ask. A poll can
 *    surface an event *older* than one already seen, so the query window deliberately overlaps
 *    the previous one by [OVERLAP_MILLIS] and duplicates are filtered by identity. Advancing a
 *    cursor straight to `now` silently drops transitions.
 *
 * 2. It reports app-level activity transitions only. It does not say which screen is showing
 *    inside the app, and this class never pretends otherwise — it answers "which package", full
 *    stop.
 */
class ForegroundAppResolver(
    private val source: UsageEventSource,
    private val selfPackage: String,
) {
    private val seen = ArrayDeque<Int>()
    private val seenSet = HashSet<Int>()
    private var lastQueriedUntil = 0L
    private var currentForeground: String? = null

    /** Returns the foreground package, or null if it is unchanged/unknown. */
    fun poll(nowUtc: Long): String? {
        val begin = if (lastQueriedUntil == 0L) nowUtc - INITIAL_LOOKBACK_MILLIS
        else lastQueriedUntil - OVERLAP_MILLIS
        val end = nowUtc + FUTURE_SLACK_MILLIS

        val fresh = source.queryEvents(begin, end)
            .filter { it.packageName != selfPackage }
            .filter { remember(it) }

        lastQueriedUntil = nowUtc

        val resolved = resolve(fresh)
        if (resolved != null) currentForeground = resolved
        return currentForeground
    }

    /** The most recent resume with no later pause or stop for the same package. */
    private fun resolve(events: List<RawUsageEvent>): String? {
        val ordered = events.sortedBy { it.timestamp }
        var candidate: String? = null
        for (event in ordered) {
            when (event.type) {
                UsageEvents.Event.ACTIVITY_RESUMED -> candidate = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> if (candidate == event.packageName) candidate = null
            }
        }
        return candidate
    }

    /** False when this exact event has already been processed. */
    private fun remember(event: RawUsageEvent): Boolean {
        val key = identityOf(event)
        if (!seenSet.add(key)) return false
        seen.addLast(key)
        while (seen.size > RING_CAPACITY) {
            seenSet.remove(seen.removeFirst())
        }
        return true
    }

    private fun identityOf(event: RawUsageEvent): Int {
        var h = event.packageName.hashCode()
        h = 31 * h + (event.className?.hashCode() ?: 0)
        h = 31 * h + event.timestamp.hashCode()
        h = 31 * h + event.type
        return h
    }

    fun reset() {
        seen.clear()
        seenSet.clear()
        lastQueriedUntil = 0L
        currentForeground = null
    }

    companion object {
        /** Query windows overlap this much, because event delivery is batched and can lag. */
        const val OVERLAP_MILLIS = 10_000L
        private const val INITIAL_LOOKBACK_MILLIS = 60_000L
        private const val FUTURE_SLACK_MILLIS = 1_000L
        private const val RING_CAPACITY = 256
    }
}

/**
 * How often to look, given what is happening.
 *
 * A fixed fast loop would be the easy implementation and a genuinely bad one — it would burn
 * battery all day to catch an event that only matters while a watched app is open. Cadence
 * therefore follows the situation, and drops to nothing whenever the screen is off.
 */
enum class MonitorCadence(val intervalMillis: Long) {
    /** Screen on, nothing we watch is in front. */
    IDLE(3_000L),

    /** A watched app is in front; a limit could trip at any moment. */
    HOT(1_000L),

    /** Screen off, or protection disabled. No polling at all. */
    SUSPENDED(Long.MAX_VALUE),
    ;

    companion object {
        fun of(screenOn: Boolean, watchedInForeground: Boolean, protectionEnabled: Boolean): MonitorCadence =
            when {
                !protectionEnabled || !screenOn -> SUSPENDED
                watchedInForeground -> HOT
                else -> IDLE
            }
    }
}
