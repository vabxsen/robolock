package com.robolock.rules

/**
 * A temporary permission to keep using an app, granted when someone chooses "Continue anyway".
 *
 * Expiry is tracked two ways on purpose. [wallExpiryUtc] survives a reboot but moves when the
 * system clock moves; [elapsedExpiry] is monotonic since boot and cannot be tampered with, but is
 * meaningless once the device restarts. Taking the *earlier* of the two while still on the same
 * boot means winding the clock backwards cannot extend a grant, which is the obvious way to cheat.
 *
 * [bootId] identifies the boot session that produced [elapsedExpiry]. After a reboot the elapsed
 * value refers to a different timeline, so only the wall clock is trusted.
 */
data class BypassGrant(
    /** What the grant covers — a package name, or a surface id for link-level grants. */
    val scopeKey: String,
    val grantedAtUtc: Long,
    val durationMillis: Long,
    val wallExpiryUtc: Long,
    val elapsedExpiry: Long,
    val bootId: String,
) {
    fun isActive(nowUtc: Long, nowElapsed: Long, currentBootId: String): Boolean =
        nowUtc < effectiveWallExpiry(nowUtc, nowElapsed, currentBootId)

    /**
     * The moment this grant really ends, expressed on the wall clock so the UI can count down.
     *
     * On the same boot the monotonic deadline wins if it is sooner. Across a reboot there is no
     * comparable monotonic reading, so the wall clock stands alone.
     */
    fun effectiveWallExpiry(nowUtc: Long, nowElapsed: Long, currentBootId: String): Long {
        if (bootId != currentBootId) return wallExpiryUtc
        val remainingByElapsed = elapsedExpiry - nowElapsed
        val wallDeadlineByElapsed = nowUtc + remainingByElapsed
        return minOf(wallExpiryUtc, wallDeadlineByElapsed)
    }

    /**
     * Re-anchors the wall-clock deadline after the system clock jumped.
     *
     * The monotonic remaining time is authoritative; the grant is never lengthened beyond the
     * duration originally given.
     */
    fun reanchoredTo(nowUtc: Long, nowElapsed: Long, currentBootId: String): BypassGrant {
        if (bootId != currentBootId) return this
        val remaining = (elapsedExpiry - nowElapsed).coerceIn(0, durationMillis)
        return copy(wallExpiryUtc = nowUtc + remaining)
    }

    companion object {
        fun create(
            scopeKey: String,
            durationMillis: Long,
            nowUtc: Long,
            nowElapsed: Long,
            bootId: String,
        ): BypassGrant = BypassGrant(
            scopeKey = scopeKey,
            grantedAtUtc = nowUtc,
            durationMillis = durationMillis,
            wallExpiryUtc = nowUtc + durationMillis,
            elapsedExpiry = nowElapsed + durationMillis,
            bootId = bootId,
        )
    }
}
