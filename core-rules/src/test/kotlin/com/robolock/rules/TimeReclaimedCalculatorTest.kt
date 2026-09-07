package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeReclaimedCalculatorTest {

    private val calc = TimeReclaimedCalculator

    @Test
    fun `only leaving the app earns credit`() {
        val typical = 10 * 60_000L
        assertThat(calc.creditFor(InterventionAction.EXITED, 0L, typical)).isGreaterThan(0L)
        assertThat(calc.creditFor(InterventionAction.CONTINUED, 0L, typical)).isEqualTo(0L)
        assertThat(calc.creditFor(InterventionAction.TIMED_OUT, 0L, typical)).isEqualTo(0L)
    }

    @Test
    fun `credit is the remainder of a typical sitting`() {
        val typical = 10 * 60_000L
        val already = 4 * 60_000L
        assertThat(calc.creditFor(InterventionAction.EXITED, already, typical))
            .isEqualTo(6 * 60_000L)
    }

    @Test
    fun `no credit when already past a typical sitting`() {
        val typical = 10 * 60_000L
        assertThat(calc.creditFor(InterventionAction.EXITED, 15 * 60_000L, typical)).isEqualTo(0L)
    }

    @Test
    fun `a single intervention cannot claim an unbounded amount`() {
        val absurd = 10 * 60 * 60_000L
        assertThat(calc.creditFor(InterventionAction.EXITED, 0L, absurd))
            .isEqualTo(TimeReclaimedCalculator.MAX_CREDIT_PER_INTERVENTION_MILLIS)
    }

    @Test
    fun `typical session falls back to the default without history`() {
        assertThat(calc.typicalSessionMillis(emptyList()))
            .isEqualTo(TimeReclaimedCalculator.DEFAULT_TYPICAL_SESSION_MILLIS)
    }

    @Test
    fun `typical session ignores trivially short sittings`() {
        // Only the 10-minute session is meaningful; the 1s ones are noise.
        assertThat(calc.typicalSessionMillis(listOf(1_000L, 1_000L, 600_000L)))
            .isEqualTo(600_000L)
    }

    @Test
    fun `typical session uses the median so one outlier cannot distort it`() {
        val sessions = listOf(60_000L, 120_000L, 180_000L, 4 * 60 * 60_000L)
        val median = calc.typicalSessionMillis(sessions)
        assertThat(median).isEqualTo(150_000L)
        // A mean would be over an hour.
        assertThat(median).isLessThan(sessions.sum() / sessions.size)
    }

    @Test
    fun `focus rate is null when there is nothing to measure`() {
        assertThat(calc.focusRate(exits = 0, total = 0)).isNull()
    }

    @Test
    fun `focus rate is a percentage of interventions that ended in leaving`() {
        assertThat(calc.focusRate(exits = 8, total = 10)).isEqualTo(80)
        assertThat(calc.focusRate(exits = 0, total = 10)).isEqualTo(0)
        assertThat(calc.focusRate(exits = 10, total = 10)).isEqualTo(100)
    }

    @Test
    fun `totals sum only the qualifying interventions`() {
        val typical = 10 * 60_000L
        val events = listOf(
            InterventionAction.EXITED to 0L,
            InterventionAction.CONTINUED to 0L,
            InterventionAction.EXITED to 5 * 60_000L,
        )
        assertThat(calc.total(events, typical)).isEqualTo(10 * 60_000L + 5 * 60_000L)
    }

    @Test
    fun `formats durations the way the dashboard reads them`() {
        assertThat(calc.format(45 * 60_000L)).isEqualTo("45m")
        assertThat(calc.format((4 * 60 + 32) * 60_000L)).isEqualTo("4h 32m")
        assertThat(calc.format(60 * 60_000L)).isEqualTo("1h 0m")
    }

    @Test
    fun `sub-minute durations are shown in seconds not rounded away`() {
        // "0m" would read as though nothing had happened.
        assertThat(calc.format(0L)).isEqualTo("0s")
        assertThat(calc.format(8_000L)).isEqualTo("8s")
        assertThat(calc.format(59_999L)).isEqualTo("59s")
        assertThat(calc.format(60_000L)).isEqualTo("1m")
    }

    @Test
    fun `negative input cannot produce a nonsense duration`() {
        assertThat(calc.format(-5_000L)).isEqualTo("0s")
    }
}
