package com.robolock.rules

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test

class RuleEngineTest {

    private val ig = SupportedPackages.INSTAGRAM
    private val boot = "boot-a"
    private val monday9am: LocalDateTime = LocalDateTime.of(2026, 3, 2, 9, 0)

    private fun input(
        rules: AppRules = AppRules(ig),
        sessionMillis: Long = 0L,
        todayUsage: Long = 0L,
        grants: List<BypassGrant> = emptyList(),
        focusTier: FocusTier = FocusTier.BALANCED,
        focusActive: Boolean = false,
        localNow: LocalDateTime = monday9am,
        protectionEnabled: Boolean = true,
        foreground: ForegroundRef? = ForegroundRef.App(ig),
    ) = RuleInput(
        foreground = foreground,
        nowUtcMillis = 1_000_000L,
        nowElapsedMillis = 50_000L,
        bootId = boot,
        localNow = localNow,
        sessionMillis = sessionMillis,
        todayUsageMillis = mapOf(ig to todayUsage),
        grants = grants,
        rules = mapOf(ig to rules),
        focusTier = focusTier,
        focusActive = focusActive,
        protectionEnabled = protectionEnabled,
    )

    @Test
    fun `allows when nothing is configured`() {
        assertThat(BlockingRuleEngine.evaluate(input())).isEqualTo(Decision.Allow)
    }

    @Test
    fun `master switch off allows everything`() {
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = 1L), sessionMillis = 999L, protectionEnabled = false),
        )
        assertThat(decision).isEqualTo(Decision.Allow)
    }

    @Test
    fun `per-app switch off allows that app`() {
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, enabled = false, sessionLimitMillis = 1L), sessionMillis = 999L),
        )
        assertThat(decision).isEqualTo(Decision.Allow)
    }

    @Test
    fun `unmonitored app is left alone`() {
        val decision = BlockingRuleEngine.evaluate(input(foreground = ForegroundRef.App("com.example.other")))
        assertThat(decision).isEqualTo(Decision.Allow)
    }

    @Test
    fun `session limit trips once reached`() {
        val fiveMin = 5 * 60_000L
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = fiveMin), sessionMillis = fiveMin),
        )
        assertThat(decision).isInstanceOf(Decision.Intervene::class.java)
        assertThat((decision as Decision.Intervene).reason).isEqualTo(InterventionReason.SESSION_LIMIT)
    }

    @Test
    fun `session limit does not trip a moment early`() {
        val fiveMin = 5 * 60_000L
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = fiveMin), sessionMillis = fiveMin - 1),
        )
        assertThat(decision).isEqualTo(Decision.Allow)
    }

    @Test
    fun `daily limit outranks session limit`() {
        val decision = BlockingRuleEngine.evaluate(
            input(
                rules = AppRules(ig, sessionLimitMillis = 1L, dailyLimitMillis = 1L),
                sessionMillis = 10L,
                todayUsage = 10L,
            ),
        ) as Decision.Intervene

        assertThat(decision.reason).isEqualTo(InterventionReason.DAILY_LIMIT)
        assertThat(decision.alsoTripped).contains(InterventionReason.SESSION_LIMIT)
    }

    @Test
    fun `schedule outranks everything else`() {
        val bedtime = Schedule(
            listOf(ScheduleWindow(setOf(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(10, 0))),
        )
        val decision = BlockingRuleEngine.evaluate(
            input(
                rules = AppRules(ig, sessionLimitMillis = 1L, dailyLimitMillis = 1L, schedule = bedtime),
                sessionMillis = 10L,
                todayUsage = 10L,
                focusActive = true,
            ),
        ) as Decision.Intervene

        assertThat(decision.reason).isEqualTo(InterventionReason.SCHEDULE)
    }

    @Test
    fun `focus mode outranks accumulated limits`() {
        val decision = BlockingRuleEngine.evaluate(
            input(
                rules = AppRules(ig, dailyLimitMillis = 1L),
                todayUsage = 10L,
                focusActive = true,
            ),
        ) as Decision.Intervene

        assertThat(decision.reason).isEqualTo(InterventionReason.FOCUS_MODE)
    }

    @Test
    fun `launch friction loses to every real rule`() {
        val decision = BlockingRuleEngine.evaluate(
            input(
                rules = AppRules(ig, sessionLimitMillis = 0L, launchFriction = true),
                sessionMillis = 0L,
            ),
        ) as Decision.Intervene

        assertThat(decision.reason).isEqualTo(InterventionReason.SESSION_LIMIT)
        assertThat(decision.alsoTripped).contains(InterventionReason.LAUNCH_FRICTION)
    }

    @Test
    fun `launch friction fires on entry when nothing else applies`() {
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, launchFriction = true), sessionMillis = 0L),
        ) as Decision.Intervene

        assertThat(decision.reason).isEqualTo(InterventionReason.LAUNCH_FRICTION)
    }

    @Test
    fun `launch friction does not fire mid-session`() {
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, launchFriction = true), sessionMillis = 60_000L),
        )
        assertThat(decision).isEqualTo(Decision.Allow)
    }

    @Test
    fun `an active bypass allows an otherwise blocked app`() {
        val g = BypassGrant.create(ig, 10 * 60_000L, 1_000_000L, 50_000L, boot)
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = 1L), sessionMillis = 999L, grants = listOf(g)),
        )
        assertThat(decision).isInstanceOf(Decision.AllowedByBypass::class.java)
    }

    @Test
    fun `an expired bypass does not`() {
        val g = BypassGrant.create(ig, 1L, 1L, 1L, boot)
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = 1L), sessionMillis = 999L, grants = listOf(g)),
        )
        assertThat(decision).isInstanceOf(Decision.Intervene::class.java)
    }

    @Test
    fun `a bypass for another app does not apply`() {
        val g = BypassGrant.create(SupportedPackages.YOUTUBE, 10 * 60_000L, 1_000_000L, 50_000L, boot)
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = 1L), sessionMillis = 999L, grants = listOf(g)),
        )
        assertThat(decision).isInstanceOf(Decision.Intervene::class.java)
    }

    @Test
    fun `strict tier offers no way through`() {
        val decision = BlockingRuleEngine.evaluate(
            input(
                rules = AppRules(ig, sessionLimitMillis = 1L),
                sessionMillis = 999L,
                focusTier = FocusTier.STRICT,
            ),
        ) as Decision.Intervene

        assertThat(decision.allowContinue).isFalse()
        assertThat(decision.graceMillis).isEqualTo(0L)
    }

    @Test
    fun `balanced tier offers a short countdown and a grace period`() {
        val decision = BlockingRuleEngine.evaluate(
            input(rules = AppRules(ig, sessionLimitMillis = 1L), sessionMillis = 999L),
        ) as Decision.Intervene

        assertThat(decision.allowContinue).isTrue()
        assertThat(decision.countdownSeconds).isEqualTo(5)
        assertThat(decision.graceMillis).isEqualTo(15 * 60_000L)
    }

    @Test
    fun `a deep link is attributed to its surface`() {
        val decision = BlockingRuleEngine.evaluate(
            input(foreground = ForegroundRef.DeepLink(YouTubeShorts), rules = AppRules(ig)),
        )
        // The rules map is keyed by Instagram, so a YouTube link finds no rules and is allowed.
        assertThat(decision).isEqualTo(Decision.Allow)

        val configured = BlockingRuleEngine.evaluate(
            RuleInput(
                foreground = ForegroundRef.DeepLink(YouTubeShorts),
                nowUtcMillis = 1_000_000L,
                nowElapsedMillis = 50_000L,
                bootId = boot,
                localNow = monday9am,
                sessionMillis = 0L,
                todayUsageMillis = emptyMap(),
                grants = emptyList(),
                rules = mapOf(SupportedPackages.YOUTUBE to AppRules(SupportedPackages.YOUTUBE)),
                focusTier = FocusTier.BALANCED,
                focusActive = false,
            ),
        ) as Decision.Intervene

        assertThat(configured.reason).isEqualTo(InterventionReason.DEEP_LINK)
        assertThat(configured.surface).isEqualTo(YouTubeShorts)
    }
}
