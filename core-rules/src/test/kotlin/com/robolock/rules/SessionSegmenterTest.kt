package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionSegmenterTest {

    private val segmenter = SessionSegmenter()
    private val ig = SupportedPackages.INSTAGRAM
    private val yt = SupportedPackages.YOUTUBE

    private fun start(at: Long = 0L): ActiveSession =
        (segmenter.onForeground(null, ig, at) as SegmenterResult.Continuing).session

    @Test
    fun `first observation starts a session`() {
        val s = start(1_000L)
        assertThat(s.packageName).isEqualTo(ig)
        assertThat(s.startedAtUtc).isEqualTo(1_000L)
        assertThat(s.foregroundMillis).isEqualTo(0L)
    }

    @Test
    fun `time accumulates while the app stays in front`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 10_000L) as SegmenterResult.Continuing).session
        s = (segmenter.onTick(s, 20_000L) as SegmenterResult.Continuing).session
        assertThat(s.foregroundMillis).isEqualTo(20_000L)
    }

    @Test
    fun `a brief excursion to another app continues the session`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 60_000L) as SegmenterResult.Continuing).session

        // Away for 10s — inside the 30s tolerance.
        s = (segmenter.onForeground(s, yt, 60_000L) as SegmenterResult.Continuing).session
        val back = segmenter.onForeground(s, ig, 70_000L)

        assertThat(back).isInstanceOf(SegmenterResult.Continuing::class.java)
        val resumed = (back as SegmenterResult.Continuing).session
        assertThat(resumed.startedAtUtc).isEqualTo(0L)
        assertThat(resumed.awaySinceUtc).isNull()
        // The 10 seconds spent in the other app are not credited to Instagram.
        assertThat(resumed.foregroundMillis).isEqualTo(60_000L)
    }

    @Test
    fun `a long excursion ends the session`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 60_000L) as SegmenterResult.Continuing).session
        s = (segmenter.onForeground(s, yt, 60_000L) as SegmenterResult.Continuing).session

        val result = segmenter.onForeground(s, yt, 95_000L)

        assertThat(result).isInstanceOf(SegmenterResult.Closed::class.java)
        val closed = (result as SegmenterResult.Closed)
        assertThat(closed.closed.packageName).isEqualTo(ig)
        assertThat(closed.closed.reason).isEqualTo(SessionEndReason.LEFT_APP)
        // Credited up to the moment they left, not the moment we noticed.
        assertThat(closed.closed.foregroundMillis).isEqualTo(60_000L)
        assertThat(closed.next?.packageName).isEqualTo(yt)
    }

    @Test
    fun `a tick retires a session whose away tolerance ran out`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 30_000L) as SegmenterResult.Continuing).session
        s = (segmenter.onForeground(s, yt, 30_000L) as SegmenterResult.Continuing).session

        val result = segmenter.onTick(s, 61_000L)

        assertThat(result).isInstanceOf(SegmenterResult.Closed::class.java)
        assertThat((result as SegmenterResult.Closed).closed.reason).isEqualTo(SessionEndReason.LEFT_APP)
    }

    @Test
    fun `a brief screen-off does not end the session`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 30_000L) as SegmenterResult.Continuing).session
        s = (segmenter.onScreenOff(s, 30_000L) as SegmenterResult.Continuing).session

        val stillGoing = segmenter.onTick(s, 70_000L)
        assertThat(stillGoing).isInstanceOf(SegmenterResult.Continuing::class.java)

        // Coming back resumes the same sitting.
        val resumed = (segmenter.onForeground(s, ig, 75_000L) as SegmenterResult.Continuing).session
        assertThat(resumed.startedAtUtc).isEqualTo(0L)
        assertThat(resumed.screenOffSinceUtc).isNull()
    }

    @Test
    fun `a long screen-off ends the session`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 30_000L) as SegmenterResult.Continuing).session
        s = (segmenter.onScreenOff(s, 30_000L) as SegmenterResult.Continuing).session

        val result = segmenter.onTick(s, 95_000L)

        assertThat(result).isInstanceOf(SegmenterResult.Closed::class.java)
        val closed = (result as SegmenterResult.Closed).closed
        assertThat(closed.reason).isEqualTo(SessionEndReason.SCREEN_OFF)
        assertThat(closed.foregroundMillis).isEqualTo(30_000L)
    }

    @Test
    fun `tab hopping cannot reset a session limit`() {
        // The behaviour the tolerance exists to prevent: bouncing between two apps repeatedly
        // must not keep resetting the accumulated session time.
        var s = start(0L)
        var now = 0L
        repeat(5) {
            now += 20_000L
            s = (segmenter.onTick(s, now) as SegmenterResult.Continuing).session
            s = (segmenter.onForeground(s, yt, now) as SegmenterResult.Continuing).session
            now += 5_000L
            s = (segmenter.onForeground(s, ig, now) as SegmenterResult.Continuing).session
        }
        assertThat(s.startedAtUtc).isEqualTo(0L)
        assertThat(s.foregroundMillis).isAtLeast(100_000L)
    }

    @Test
    fun `force close credits time and records the reason`() {
        var s = start(0L)
        s = (segmenter.onTick(s, 45_000L) as SegmenterResult.Continuing).session

        val closed = segmenter.forceClose(s, 45_000L, SessionEndReason.REBOOT)

        assertThat(closed.reason).isEqualTo(SessionEndReason.REBOOT)
        assertThat(closed.foregroundMillis).isEqualTo(45_000L)
    }

    @Test
    fun `ticking with no session is idle`() {
        assertThat(segmenter.onTick(null, 1_000L)).isEqualTo(SegmenterResult.Idle)
        assertThat(segmenter.onScreenOff(null, 1_000L)).isEqualTo(SegmenterResult.Idle)
    }
}
