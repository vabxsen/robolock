package com.robolock.app.data.bypass

import com.robolock.app.core.time.Clock
import com.robolock.app.data.db.BypassGrantDao
import com.robolock.app.data.db.BypassGrantEntity
import com.robolock.rules.BypassGrant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Live "continue anyway" grants.
 *
 * A grant is a promise not to interrupt again for a while. It has to survive the process being
 * killed — otherwise force-stopping Robolock would revoke the grace period the user was just
 * given — so it lives in the database rather than in memory.
 */
class BypassRepository(
    private val dao: BypassGrantDao,
    private val clock: Clock,
) {

    fun observeActive(): Flow<List<BypassGrant>> = dao.observeAll().map { rows ->
        val now = clock.nowUtcMillis()
        val elapsed = clock.elapsedRealtime()
        val boot = clock.bootId()
        rows.map { it.toDomain() }.filter { it.isActive(now, elapsed, boot) }
    }

    suspend fun active(): List<BypassGrant> {
        val now = clock.nowUtcMillis()
        val elapsed = clock.elapsedRealtime()
        val boot = clock.bootId()
        return dao.all().map { it.toDomain() }.filter { it.isActive(now, elapsed, boot) }
    }

    suspend fun grant(scopeKey: String, durationMillis: Long): BypassGrant {
        val grant = BypassGrant.create(
            scopeKey = scopeKey,
            durationMillis = durationMillis,
            nowUtc = clock.nowUtcMillis(),
            nowElapsed = clock.elapsedRealtime(),
            bootId = clock.bootId(),
        )
        dao.put(grant.toEntity())
        return grant
    }

    suspend fun revoke(scopeKey: String) = dao.remove(scopeKey)

    /**
     * Re-anchors wall-clock deadlines after the system clock jumped.
     *
     * Called from the time-changed receiver. Grants from a previous boot are left alone because
     * their monotonic reading no longer means anything.
     */
    suspend fun reanchorAfterClockChange() {
        val now = clock.nowUtcMillis()
        val elapsed = clock.elapsedRealtime()
        val boot = clock.bootId()
        dao.all()
            .map { it.toDomain() }
            .filter { it.bootId == boot }
            .forEach { dao.put(it.reanchoredTo(now, elapsed, boot).toEntity()) }
    }

    suspend fun pruneExpired() = dao.pruneExpired(clock.nowUtcMillis())

    suspend fun clear() = dao.clear()
}

private fun BypassGrantEntity.toDomain() = BypassGrant(
    scopeKey = scopeKey,
    grantedAtUtc = grantedAtUtc,
    durationMillis = durationMillis,
    wallExpiryUtc = wallExpiryUtc,
    elapsedExpiry = elapsedExpiry,
    bootId = bootId,
)

private fun BypassGrant.toEntity() = BypassGrantEntity(
    scopeKey = scopeKey,
    grantedAtUtc = grantedAtUtc,
    durationMillis = durationMillis,
    wallExpiryUtc = wallExpiryUtc,
    elapsedExpiry = elapsedExpiry,
    bootId = bootId,
)
