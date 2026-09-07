package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BypassGrantTest {

    private val boot = "boot-a"
    private val tenMinutes = 10 * 60_000L

    private fun grant(nowUtc: Long = 1_000_000L, nowElapsed: Long = 50_000L) =
        BypassGrant.create("com.instagram.android", tenMinutes, nowUtc, nowElapsed, boot)

    @Test
    fun `is active immediately after being granted`() {
        val g = grant()
        assertThat(g.isActive(1_000_000L, 50_000L, boot)).isTrue()
    }

    @Test
    fun `expires once the duration has genuinely passed`() {
        val g = grant()
        assertThat(g.isActive(1_000_000L + tenMinutes, 50_000L + tenMinutes, boot)).isFalse()
    }

    @Test
    fun `clock moved backwards does not extend the grant`() {
        val g = grant(nowUtc = 1_000_000L, nowElapsed = 50_000L)

        // The user winds the wall clock back an hour, but only five real minutes have elapsed.
        val fiveMinutes = 5 * 60_000L
        val tamperedNow = 1_000_000L - 3_600_000L
        val realElapsed = 50_000L + fiveMinutes

        val expiry = g.effectiveWallExpiry(tamperedNow, realElapsed, boot)

        // Five real minutes remain, not sixty-five.
        assertThat(expiry - tamperedNow).isEqualTo(tenMinutes - fiveMinutes)
        assertThat(g.isActive(tamperedNow, realElapsed, boot)).isTrue()

        // And it still ends on schedule in real time.
        val afterTenRealMinutes = 50_000L + tenMinutes
        assertThat(g.isActive(tamperedNow, afterTenRealMinutes, boot)).isFalse()
    }

    @Test
    fun `clock moved forwards cannot be undone by elapsed time`() {
        val g = grant()
        // Wall clock jumps past the deadline even though little real time passed.
        assertThat(g.isActive(1_000_000L + tenMinutes + 1, 50_001L, boot)).isFalse()
    }

    @Test
    fun `slept through the grace period so it expires`() {
        val g = grant()
        val muchLater = 1_000_000L + 8 * 3_600_000L
        assertThat(g.isActive(muchLater, 50_000L + 8 * 3_600_000L, boot)).isFalse()
    }

    @Test
    fun `after reboot falls back to the wall clock`() {
        val g = grant(nowUtc = 1_000_000L, nowElapsed = 50_000L)
        // New boot: elapsed restarts near zero and means nothing for this grant.
        val newBoot = "boot-b"
        val fiveMinutesLater = 1_000_000L + 5 * 60_000L

        assertThat(g.effectiveWallExpiry(fiveMinutesLater, 3_000L, newBoot)).isEqualTo(g.wallExpiryUtc)
        assertThat(g.isActive(fiveMinutesLater, 3_000L, newBoot)).isTrue()
        assertThat(g.isActive(1_000_000L + tenMinutes, 3_000L, newBoot)).isFalse()
    }

    @Test
    fun `reanchoring after a clock jump preserves the remaining real time`() {
        val g = grant(nowUtc = 1_000_000L, nowElapsed = 50_000L)
        val fourMinutes = 4 * 60_000L
        val jumpedNow = 9_999_999L

        val fixed = g.reanchoredTo(jumpedNow, 50_000L + fourMinutes, boot)

        assertThat(fixed.wallExpiryUtc - jumpedNow).isEqualTo(tenMinutes - fourMinutes)
    }

    @Test
    fun `reanchoring never lengthens a grant beyond its original duration`() {
        val g = grant()
        // Elapsed reads absurdly low, which would otherwise imply more time remaining than granted.
        val fixed = g.reanchoredTo(2_000_000L, 0L, boot)
        assertThat(fixed.wallExpiryUtc - 2_000_000L).isAtMost(tenMinutes)
    }
}
