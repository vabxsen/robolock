package com.robolock.app.data.rules

import com.robolock.app.data.db.AppRuleDao
import com.robolock.app.data.db.AppRuleEntity
import com.robolock.rules.AppRules
import com.robolock.rules.Schedule
import com.robolock.rules.ScheduleCodec
import com.robolock.rules.SupportedAppRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-app limits.
 *
 * Apps with no stored row fall back to a disabled default rather than being silently unprotected
 * or silently protected: nothing is enforced until the user turns it on, and the controls screen
 * reflects exactly what is stored.
 */
class RulesRepository(
    private val dao: AppRuleDao,
    private val registry: SupportedAppRegistry,
) {

    fun observeAll(): Flow<Map<String, AppRules>> = dao.observeAll().map { rows ->
        val stored = rows.associate { it.packageName to it.toDomain() }
        rulesWithBuiltInDefaults(stored)
    }

    suspend fun current(): Map<String, AppRules> {
        val stored = dao.all().associate { it.packageName to it.toDomain() }
        return rulesWithBuiltInDefaults(stored)
    }

    suspend fun save(rules: AppRules) = dao.upsert(rules.toEntity())

    suspend fun update(packageName: String, transform: (AppRules) -> AppRules) {
        val existing = current()[packageName] ?: defaultFor(packageName)
        save(transform(existing))
    }

    suspend fun clear() = dao.clear()

    private fun defaultFor(packageName: String) = AppRules(packageName = packageName, enabled = false)

    /** Keeps explicit rules for user-selected apps outside the small built-in catalog. */
    private fun rulesWithBuiltInDefaults(stored: Map<String, AppRules>): Map<String, AppRules> = buildMap {
        registry.apps.forEach { app ->
            put(app.packageName, stored[app.packageName] ?: defaultFor(app.packageName))
        }
        stored.forEach { (packageName, rules) -> put(packageName, rules) }
    }
}

private fun AppRuleEntity.toDomain() = AppRules(
    packageName = packageName,
    enabled = enabled,
    sessionLimitMillis = sessionLimitMillis,
    dailyLimitMillis = dailyLimitMillis,
    launchFriction = launchFriction,
    schedule = ScheduleCodec.decode(scheduleSpec),
    includeInFocus = includeInFocus,
)

private fun AppRules.toEntity() = AppRuleEntity(
    packageName = packageName,
    enabled = enabled,
    sessionLimitMillis = sessionLimitMillis,
    dailyLimitMillis = dailyLimitMillis,
    launchFriction = launchFriction,
    includeInFocus = includeInFocus,
    scheduleSpec = ScheduleCodec.encode(schedule),
)

/** Convenience for the controls screen, which offers a fixed set of durations. */
object LimitOptions {
    val sessionMinutes = listOf(5, 10, 15, 20, 30, 45, 60)
    val dailyMinutes = listOf(15, 30, 45, 60, 90, 120, 180)

    fun label(millis: Long?): String = when (millis) {
        null -> "Unlimited"
        else -> {
            val minutes = millis / 60_000L
            if (minutes >= 60 && minutes % 60 == 0L) {
                val hours = minutes / 60
                if (hours == 1L) "1 hour" else "$hours hours"
            } else {
                "$minutes minutes"
            }
        }
    }
}

fun Schedule.displayLabel(): String = summary()
