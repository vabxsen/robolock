package com.robolock.rules

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A recurring window of local wall-clock time, e.g. "10 PM to 7 AM on weeknights".
 *
 * Windows that cross midnight are the normal case here rather than an edge case — bedtime rules
 * are the main reason schedules exist — so the wrap is handled explicitly.
 *
 * [days] names the day the window *starts* on. A Monday 22:00–07:00 window therefore covers
 * Monday night into Tuesday morning, and Tuesday 06:00 belongs to Monday's window, not Tuesday's.
 * Getting that attribution wrong is what makes naive implementations unblock at midnight.
 */
data class ScheduleWindow(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    /** True when the window runs past midnight into the following day. */
    val crossesMidnight: Boolean get() = !end.isAfter(start)

    fun contains(at: LocalDateTime): Boolean {
        val time = at.toLocalTime()
        return if (crossesMidnight) {
            // Started today and still running, or started yesterday and not yet finished.
            (at.dayOfWeek in days && !time.isBefore(start)) ||
                (at.minusDays(1).dayOfWeek in days && time.isBefore(end))
        } else {
            at.dayOfWeek in days && !time.isBefore(start) && time.isBefore(end)
        }
    }

    /** Human-readable form for the controls screen, e.g. "9:00 PM – 7:00 AM". */
    fun label(): String = "${format(start)} – ${format(end)}"

    private fun format(t: LocalTime): String {
        val hour = when (val h = t.hour % 12) {
            0 -> 12
            else -> h
        }
        val suffix = if (t.hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(hour, t.minute, suffix)
    }

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    }
}

/** A named group of windows. Empty means "never", which is different from "always". */
data class Schedule(val windows: List<ScheduleWindow> = emptyList()) {
    val isSet: Boolean get() = windows.isNotEmpty()

    fun isActiveAt(at: LocalDateTime): Boolean = windows.any { it.contains(at) }

    fun summary(): String = when {
        windows.isEmpty() -> "Not set"
        windows.size == 1 -> windows.first().label()
        else -> "${windows.size} active"
    }
}
