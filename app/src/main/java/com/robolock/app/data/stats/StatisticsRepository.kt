package com.robolock.app.data.stats

import com.robolock.app.core.time.Clock
import com.robolock.app.data.db.DailyUsageDao
import com.robolock.app.data.db.InterventionEventDao
import com.robolock.app.data.db.InterventionEventEntity
import com.robolock.app.data.db.SessionDao
import com.robolock.app.data.db.SessionEntity
import com.robolock.app.data.db.toEntity
import com.robolock.rules.ChartBucket
import com.robolock.rules.ClosedSession
import com.robolock.rules.DetectionCapability
import com.robolock.rules.InterventionAction
import com.robolock.rules.ObservedIntervention
import com.robolock.rules.SurfaceCatalog
import com.robolock.rules.SurfaceStats
import com.robolock.rules.TimeReclaimedCalculator
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Everything the Home and Stats screens show for a period. */
data class StatsSnapshot(
    val interventions: Int = 0,
    val exits: Int = 0,
    val continued: Int = 0,
    val timeReclaimedMillis: Long = 0,
    val focusRate: Int? = null,
    val buckets: List<ChartBucket> = emptyList(),
    val perApp: List<AppUsage> = emptyList(),
) {
    val hasData: Boolean get() = interventions > 0 || perApp.isNotEmpty()

    val formattedTimeReclaimed: String get() = TimeReclaimedCalculator.format(timeReclaimedMillis)
}

data class AppUsage(val packageName: String, val foregroundMillis: Long) {
    val formatted: String get() = TimeReclaimedCalculator.format(foregroundMillis)
}

/**
 * Reads the local event log.
 *
 * Every figure here is derived from something Robolock actually observed. There are no seeded or
 * sample values: a fresh install reports zeros and a null focus rate, and the UI renders that as
 * a dash rather than inventing a flattering number.
 */
class StatisticsRepository(
    private val events: InterventionEventDao,
    private val sessions: SessionDao,
    private val daily: DailyUsageDao,
    private val clock: Clock,
) {

    /**
     * The only way an intervention reaches storage.
     *
     * [ObservedIntervention] cannot be constructed outside the rules module, so this signature is
     * what makes fabricated statistics impossible rather than merely discouraged.
     */
    suspend fun record(intervention: ObservedIntervention) {
        events.insert(intervention.toEntity(clock.dateKeyFor(intervention.timestampUtc)))
    }

    /** Persists a completed sitting. Usage totals are derived from these rows. */
    suspend fun recordSession(session: ClosedSession) {
        sessions.upsert(
            SessionEntity(
                id = UUID.randomUUID().toString(),
                packageName = session.packageName,
                startedAtUtc = session.startedAtUtc,
                endedAtUtc = session.endedAtUtc,
                foregroundMillis = session.foregroundMillis,
                // Attributed to the day it started, so a sitting that runs past midnight counts
                // against the budget it was actually spending.
                dateKey = clock.dateKeyFor(session.startedAtUtc),
                endReason = session.reason.id,
            ),
        )
    }

    /**
     * Foreground time today for one app.
     *
     * Closed sessions only — the sitting in progress is tracked live by the monitor and added
     * there, so counting it here would double it.
     */
    suspend fun foregroundMillisToday(packageName: String): Long =
        sessions.foregroundMillisForDay(clock.todayKey(), packageName)

    fun observeToday(): Flow<StatsSnapshot> {
        val today = clock.today()
        return observeRange(today, today)
    }

    fun observeWeek(endingOn: LocalDate = clock.today()): Flow<StatsSnapshot> =
        observeRange(endingOn.minusDays(6), endingOn)

    fun observeRange(from: LocalDate, to: LocalDate): Flow<StatsSnapshot> {
        val fromKey = from.format(Clock.DATE_KEY)
        val toKey = to.format(Clock.DATE_KEY)

        return combine(
            events.observeBetween(fromKey, toKey),
            sessions.observeBetween(fromKey, toKey),
        ) { eventRows, sessionRows ->
            snapshot(eventRows, sessionRows, from, to)
        }
    }

    private fun snapshot(
        eventRows: List<InterventionEventEntity>,
        sessionRows: List<SessionEntity>,
        from: LocalDate,
        to: LocalDate,
    ): StatsSnapshot {
        val exits = eventRows.count { it.actionEnum == InterventionAction.EXITED }
        val continued = eventRows.count { it.actionEnum == InterventionAction.CONTINUED }

        // A typical sitting is measured per app from that app's own completed sessions.
        val typicalByPackage = sessionRows
            .groupBy { it.packageName }
            .mapValues { (_, rows) ->
                TimeReclaimedCalculator.typicalSessionMillis(rows.map { it.foregroundMillis })
            }

        val reclaimed = eventRows.sumOf { row ->
            val action = row.actionEnum ?: return@sumOf 0L
            val typical = typicalByPackage[row.packageName]
                ?: TimeReclaimedCalculator.DEFAULT_TYPICAL_SESSION_MILLIS
            TimeReclaimedCalculator.creditFor(action, row.sessionMillisBefore, typical)
        }

        val perApp = sessionRows
            .groupBy { it.packageName }
            .map { (pkg, rows) -> AppUsage(pkg, rows.sumOf { it.foregroundMillis }) }
            .sortedByDescending { it.foregroundMillis }

        return StatsSnapshot(
            interventions = eventRows.size,
            exits = exits,
            continued = continued,
            timeReclaimedMillis = reclaimed,
            focusRate = TimeReclaimedCalculator.focusRate(exits, eventRows.size),
            buckets = buckets(eventRows, from, to),
            perApp = perApp,
        )
    }

    /** One column per day across the range, including days with nothing on them. */
    private fun buckets(
        rows: List<InterventionEventEntity>,
        from: LocalDate,
        to: LocalDate,
    ): List<ChartBucket> {
        val counts = rows.groupingBy { it.dateKey }.eachCount()
        val days = mutableListOf<ChartBucket>()
        var day = from
        while (!day.isAfter(to)) {
            val key = day.format(Clock.DATE_KEY)
            days += ChartBucket(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                count = counts[key] ?: 0,
            )
            day = day.plusDays(1)
        }
        return days
    }

    /**
     * Per-surface figures.
     *
     * Surfaces Robolock cannot observe report [SurfaceStats.NoData], which is a different thing
     * from zero and is rendered differently. Reporting "0 blocked" for WhatsApp Status would imply
     * we were watching and saw nothing, which is not true.
     */
    suspend fun surfaceStats(): List<SurfaceStats> = SurfaceCatalog.selectable.map { surface ->
        if (surface.capability == DetectionCapability.UNSUPPORTED) {
            SurfaceStats.NoData(surface)
        } else {
            val count = events.countForSurface(surface.id)
            SurfaceStats.Counted(surface, interventions = count, exits = 0)
        }
    }

    suspend fun clearAll() {
        events.clear()
        sessions.clear()
        daily.clear()
    }
}
