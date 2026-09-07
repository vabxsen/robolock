package com.robolock.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: InterventionEventEntity)

    @Query("SELECT * FROM intervention_events WHERE dateKey = :dateKey ORDER BY timestampUtc DESC")
    fun observeForDay(dateKey: String): Flow<List<InterventionEventEntity>>

    @Query("SELECT * FROM intervention_events WHERE dateKey BETWEEN :from AND :to ORDER BY timestampUtc")
    fun observeBetween(from: String, to: String): Flow<List<InterventionEventEntity>>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE dateKey = :dateKey")
    suspend fun countForDay(dateKey: String): Int

    @Query("SELECT COUNT(*) FROM intervention_events WHERE surfaceId = :surfaceId")
    suspend fun countForSurface(surfaceId: String): Int

    @Query("DELETE FROM intervention_events")
    suspend fun clear()
}

@Dao
interface SessionDao {

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE dateKey = :dateKey")
    fun observeForDay(dateKey: String): Flow<List<SessionEntity>>

    @Query(
        "SELECT COALESCE(SUM(foregroundMillis), 0) FROM sessions " +
            "WHERE dateKey = :dateKey AND packageName = :packageName",
    )
    suspend fun foregroundMillisForDay(dateKey: String, packageName: String): Long

    @Query("SELECT * FROM sessions WHERE dateKey BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Dao
interface DailyUsageDao {

    @Upsert
    suspend fun upsert(row: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE dateKey = :dateKey")
    suspend fun forDay(dateKey: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE dateKey BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<DailyUsageEntity>>

    @Query("DELETE FROM daily_usage")
    suspend fun clear()
}

@Dao
interface BypassGrantDao {

    /** Replaces any existing grant for the same scope, so a scope can only hold one live grant. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(grant: BypassGrantEntity)

    @Query("SELECT * FROM bypass_grants")
    suspend fun all(): List<BypassGrantEntity>

    @Query("SELECT * FROM bypass_grants")
    fun observeAll(): Flow<List<BypassGrantEntity>>

    @Query("DELETE FROM bypass_grants WHERE scopeKey = :scopeKey")
    suspend fun remove(scopeKey: String)

    @Query("DELETE FROM bypass_grants WHERE wallExpiryUtc < :nowUtc")
    suspend fun pruneExpired(nowUtc: Long)

    @Query("DELETE FROM bypass_grants")
    suspend fun clear()
}

@Dao
interface AppRuleDao {

    @Upsert
    suspend fun upsert(rule: AppRuleEntity)

    @Query("SELECT * FROM app_rules")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    suspend fun all(): List<AppRuleEntity>

    @Query("DELETE FROM app_rules")
    suspend fun clear()
}
