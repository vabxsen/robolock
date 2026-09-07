package com.robolock.rules

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Serializes a [Schedule] for storage as a single column.
 *
 * Format: `MONDAY,TUESDAY|22:00|07:00;SATURDAY|23:00|09:00`. Day names are the enum constants and
 * times are ISO local times, so a stored value stays readable in a database browser and survives
 * a locale change — both of which a numeric packing would cost us for no real gain.
 *
 * Decoding is deliberately forgiving: a malformed window is dropped rather than throwing, because
 * a corrupt preference should degrade to "no schedule" instead of crashing the monitor.
 */
object ScheduleCodec {

    fun encode(schedule: Schedule): String = schedule.windows.joinToString(";") { w ->
        val days = w.days.sortedBy { it.value }.joinToString(",") { it.name }
        "$days|${w.start}|${w.end}"
    }

    fun decode(spec: String): Schedule {
        if (spec.isBlank()) return Schedule()
        val windows = spec.split(";").mapNotNull { part -> decodeWindow(part) }
        return Schedule(windows)
    }

    private fun decodeWindow(part: String): ScheduleWindow? {
        val fields = part.split("|")
        if (fields.size != 3) return null

        val days = fields[0].split(",")
            .mapNotNull { name -> runCatching { DayOfWeek.valueOf(name.trim()) }.getOrNull() }
            .toSet()
        if (days.isEmpty()) return null

        val start = runCatching { LocalTime.parse(fields[1]) }.getOrNull() ?: return null
        val end = runCatching { LocalTime.parse(fields[2]) }.getOrNull() ?: return null

        return ScheduleWindow(days, start, end)
    }
}
