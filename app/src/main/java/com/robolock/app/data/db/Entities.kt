package com.robolock.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.robolock.rules.InterventionAction
import com.robolock.rules.InterventionReason
import com.robolock.rules.ObservedIntervention
import com.robolock.rules.SessionEndReason
import java.util.UUID

/**
 * One recorded interruption.
 *
 * Rows are only ever created from [ObservedIntervention], which in turn can only be produced from
 * detection evidence. That is what keeps the statistics honest: there is no code path that writes
 * an event for something Robolock did not actually observe.
 */
@Entity(tableName = "intervention_events", indices = [Index("dateKey"), Index("packageName")])
data class InterventionEventEntity(
    @PrimaryKey val id: String,
    val surfaceId: String,
    val packageName: String,
    val timestampUtc: Long,
    /** Local calendar day, yyyy-MM-dd, resolved when written. Statistics group on this. */
    val dateKey: String,
    val reason: String,
    val action: String,
    val sessionMillisBefore: Long,
    val bypassMillis: Long?,
) {
    val reasonEnum: InterventionReason? get() = InterventionReason.fromId(reason)
    val actionEnum: InterventionAction? get() = InterventionAction.fromId(action)
}

internal fun ObservedIntervention.toEntity(dateKey: String): InterventionEventEntity =
    InterventionEventEntity(
        id = UUID.randomUUID().toString(),
        surfaceId = surfaceId,
        packageName = packageName,
        timestampUtc = timestampUtc,
        dateKey = dateKey,
        reason = reason.id,
        action = action.id,
        sessionMillisBefore = sessionMillisBefore,
        bypassMillis = bypassMillis,
    )

/** A completed sitting. Closed sessions are the source of all usage totals. */
@Entity(tableName = "sessions", indices = [Index("dateKey"), Index("packageName")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val startedAtUtc: Long,
    val endedAtUtc: Long,
    val foregroundMillis: Long,
    val dateKey: String,
    val endReason: String,
) {
    val endReasonEnum: SessionEndReason?
        get() = SessionEndReason.entries.firstOrNull { it.id == endReason }
}

/**
 * Per-day, per-app rollup.
 *
 * [zoneId] is stored with the row because the day boundary was decided in that zone. When someone
 * flies across timezones, past days keep the meaning they had when they were lived; only future
 * ones use the new zone.
 */
@Entity(tableName = "daily_usage", primaryKeys = ["dateKey", "packageName"])
data class DailyUsageEntity(
    val dateKey: String,
    val packageName: String,
    val foregroundMillis: Long,
    val launches: Int,
    val interventions: Int,
    val zoneId: String,
)

/**
 * A live "continue anyway" grant.
 *
 * Kept in Room rather than DataStore: it is written on the hot path, needs to be replaced
 * atomically per scope, and carries the monotonic elapsed deadline used to defeat clock tampering.
 */
@Entity(tableName = "bypass_grants")
data class BypassGrantEntity(
    @PrimaryKey val scopeKey: String,
    val grantedAtUtc: Long,
    val durationMillis: Long,
    val wallExpiryUtc: Long,
    val elapsedExpiry: Long,
    val bootId: String,
)

/** Per-app limits as configured on the App Controls screen. */
@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val enabled: Boolean,
    val sessionLimitMillis: Long?,
    val dailyLimitMillis: Long?,
    val launchFriction: Boolean,
    val includeInFocus: Boolean,
    /** Serialized schedule windows; see ScheduleCodec. */
    val scheduleSpec: String,
)
