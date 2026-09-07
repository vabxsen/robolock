package com.robolock.rules

/**
 * Estimates how much time an interruption saved.
 *
 * This is an estimate and the app says so. Robolock cannot know what someone would have done had
 * it stayed quiet; claiming otherwise would be exactly the kind of invented statistic this app is
 * built to avoid. What it can do is apply one documented, centralised heuristic so the number is
 * at least consistent and tunable.
 *
 * The model: leaving an app early avoids the *rest* of a typical sitting. We estimate that
 * remainder from the person's own recent behaviour — their median completed session for that app —
 * rather than an invented industry figure, and cap it so a single outlier cannot inflate a day.
 *
 * Only [InterventionAction.EXITED] earns credit. Continuing earns nothing, and a timed-out overlay
 * earns nothing, because in neither case did the person actually stop.
 */
object TimeReclaimedCalculator {

    /** Used before there is enough history to compute a personal median. */
    const val DEFAULT_TYPICAL_SESSION_MILLIS: Long = 8 * 60_000L

    /** No single interruption may claim more than this, however long typical sessions are. */
    const val MAX_CREDIT_PER_INTERVENTION_MILLIS: Long = 30 * 60_000L

    /** Sessions shorter than this are ignored when computing a typical length. */
    private const val MIN_MEANINGFUL_SESSION_MILLIS: Long = 20_000L

    /**
     * The typical uninterrupted sitting for an app, as a median of recent completed sessions.
     *
     * A median rather than a mean: one forgotten two-hour session should not redefine "typical".
     */
    fun typicalSessionMillis(recentSessionMillis: List<Long>): Long {
        val meaningful = recentSessionMillis.filter { it >= MIN_MEANINGFUL_SESSION_MILLIS }.sorted()
        if (meaningful.isEmpty()) return DEFAULT_TYPICAL_SESSION_MILLIS
        val mid = meaningful.size / 2
        return if (meaningful.size % 2 == 1) {
            meaningful[mid]
        } else {
            (meaningful[mid - 1] + meaningful[mid]) / 2
        }
    }

    /**
     * Credit for one interruption.
     *
     * [sessionMillisBefore] is how long they had already been in the app. The estimate is what
     * remained of a typical sitting, so interrupting someone who was already past their usual
     * length credits nothing — there was little left to save.
     */
    fun creditFor(
        action: InterventionAction,
        sessionMillisBefore: Long,
        typicalSessionMillis: Long,
    ): Long {
        if (action != InterventionAction.EXITED) return 0L
        val remaining = typicalSessionMillis - sessionMillisBefore
        return remaining.coerceIn(0L, MAX_CREDIT_PER_INTERVENTION_MILLIS)
    }

    /** Total credit across a set of interruptions sharing one typical-session estimate. */
    fun total(
        interventions: List<Pair<InterventionAction, Long>>,
        typicalSessionMillis: Long,
    ): Long = interventions.sumOf { (action, before) ->
        creditFor(action, before, typicalSessionMillis)
    }

    /**
     * Share of interruptions that ended in actually leaving.
     *
     * Null when there is nothing to measure — a fresh install shows a dash, not a flattering 100%.
     */
    fun focusRate(exits: Int, total: Int): Int? {
        if (total <= 0) return null
        return ((exits.toDouble() / total) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * "4h 32m", "45m", "38s".
     *
     * Sub-minute durations are shown in seconds rather than rounded to "0m", which would read as
     * though nothing had happened.
     */
    fun format(millis: Long): String {
        val safe = millis.coerceAtLeast(0)
        if (safe < 60_000L) return "${safe / 1000L}s"
        val totalMinutes = safe / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
