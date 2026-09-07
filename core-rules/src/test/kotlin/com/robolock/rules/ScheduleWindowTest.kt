package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test

class ScheduleWindowTest {

    /** A bedtime window: Monday-night 22:00 running into Tuesday 07:00. */
    private val bedtime = ScheduleWindow(
        days = setOf(DayOfWeek.MONDAY),
        start = LocalTime.of(22, 0),
        end = LocalTime.of(7, 0),
    )

    private fun mon(h: Int, m: Int = 0) = LocalDateTime.of(2026, 3, 2, h, m) // a Monday
    private fun tue(h: Int, m: Int = 0) = LocalDateTime.of(2026, 3, 3, h, m)
    private fun wed(h: Int, m: Int = 0) = LocalDateTime.of(2026, 3, 4, h, m)

    @Test
    fun `window crossing midnight is recognised as such`() {
        assertThat(bedtime.crossesMidnight).isTrue()
    }

    @Test
    fun `active just before midnight on the starting day`() {
        assertThat(bedtime.contains(mon(23, 59))).isTrue()
    }

    @Test
    fun `stays active just after midnight`() {
        assertThat(bedtime.contains(tue(0, 1))).isTrue()
    }

    @Test
    fun `still active one minute before the end`() {
        assertThat(bedtime.contains(tue(6, 59))).isTrue()
    }

    @Test
    fun `ends exactly at the end time`() {
        assertThat(bedtime.contains(tue(7, 0))).isFalse()
    }

    @Test
    fun `not active before the start on the starting day`() {
        assertThat(bedtime.contains(mon(21, 59))).isFalse()
    }

    @Test
    fun `does not leak into an unscheduled following night`() {
        // Tuesday night is not in `days`, so Tuesday 23:00 and Wednesday 03:00 are both outside.
        assertThat(bedtime.contains(tue(23, 0))).isFalse()
        assertThat(bedtime.contains(wed(3, 0))).isFalse()
    }

    @Test
    fun `same-day window behaves normally`() {
        val study = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0))
        assertThat(study.crossesMidnight).isFalse()
        assertThat(study.contains(mon(8, 59))).isFalse()
        assertThat(study.contains(mon(9, 0))).isTrue()
        assertThat(study.contains(mon(16, 59))).isTrue()
        assertThat(study.contains(mon(17, 0))).isFalse()
    }

    @Test
    fun `a nonexistent local time during spring forward is simply outside the window`() {
        // US DST 2026 springs forward 2026-03-08 02:00 -> 03:00. A 00:30-01:30 window is unaffected,
        // and evaluating around the gap must not throw.
        val early = ScheduleWindow(setOf(DayOfWeek.SUNDAY), LocalTime.of(0, 30), LocalTime.of(1, 30))
        val duringGap = LocalDateTime.of(2026, 3, 8, 3, 0)
        assertThat(early.contains(duringGap)).isFalse()
        assertThat(early.contains(LocalDateTime.of(2026, 3, 8, 1, 0))).isTrue()
    }

    @Test
    fun `weekday set covers monday through friday only`() {
        assertThat(ScheduleWindow.WEEKDAYS).hasSize(5)
        assertThat(ScheduleWindow.WEEKDAYS).doesNotContain(DayOfWeek.SATURDAY)
    }

    @Test
    fun `label reads as a human would write it`() {
        assertThat(bedtime.label()).isEqualTo("10:00 PM – 7:00 AM")
        val noon = ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(12, 0), LocalTime.of(0, 30))
        assertThat(noon.label()).isEqualTo("12:00 PM – 12:30 AM")
    }

    @Test
    fun `empty schedule is never active`() {
        assertThat(Schedule().isActiveAt(mon(23, 0))).isFalse()
        assertThat(Schedule().summary()).isEqualTo("Not set")
    }
}
