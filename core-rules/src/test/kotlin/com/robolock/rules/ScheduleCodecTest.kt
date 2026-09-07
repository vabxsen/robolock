package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Test

class ScheduleCodecTest {

    private val bedtime = ScheduleWindow(
        days = ScheduleWindow.WEEKDAYS,
        start = LocalTime.of(22, 0),
        end = LocalTime.of(7, 0),
    )

    @Test
    fun `round trips a single window`() {
        val schedule = Schedule(listOf(bedtime))
        assertThat(ScheduleCodec.decode(ScheduleCodec.encode(schedule))).isEqualTo(schedule)
    }

    @Test
    fun `round trips multiple windows`() {
        val weekend = ScheduleWindow(
            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            LocalTime.of(23, 30),
            LocalTime.of(9, 15),
        )
        val schedule = Schedule(listOf(bedtime, weekend))
        assertThat(ScheduleCodec.decode(ScheduleCodec.encode(schedule))).isEqualTo(schedule)
    }

    @Test
    fun `empty schedule round trips`() {
        assertThat(ScheduleCodec.encode(Schedule())).isEmpty()
        assertThat(ScheduleCodec.decode("")).isEqualTo(Schedule())
        assertThat(ScheduleCodec.decode("   ")).isEqualTo(Schedule())
    }

    @Test
    fun `encoded form is stable and readable`() {
        val schedule = Schedule(
            listOf(ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(7, 0))),
        )
        assertThat(ScheduleCodec.encode(schedule)).isEqualTo("MONDAY|22:00|07:00")
    }

    @Test
    fun `days are encoded in week order regardless of set ordering`() {
        val jumbled = Schedule(
            listOf(
                ScheduleWindow(
                    setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                ),
            ),
        )
        assertThat(ScheduleCodec.encode(jumbled)).startsWith("MONDAY,WEDNESDAY,FRIDAY")
    }

    @Test
    fun `corrupt input degrades to no schedule rather than throwing`() {
        listOf(
            "garbage",
            "MONDAY|notatime|07:00",
            "NOTADAY|22:00|07:00",
            "MONDAY|22:00",
            "|22:00|07:00",
        ).forEach { spec ->
            assertThat(ScheduleCodec.decode(spec).windows).isEmpty()
        }
    }

    @Test
    fun `a corrupt window does not discard the valid ones`() {
        val decoded = ScheduleCodec.decode("MONDAY|22:00|07:00;NOTADAY|1|2")
        assertThat(decoded.windows).hasSize(1)
        assertThat(decoded.windows.first().days).containsExactly(DayOfWeek.MONDAY)
    }
}
