package com.robolock.rules

import java.time.LocalDateTime

/** Per-app limits, as configured on the App Controls screen. */
data class AppRules(
    val packageName: String,
    val enabled: Boolean = true,
    /** Longest single sitting before Robolock steps in. Null means no session limit. */
    val sessionLimitMillis: Long? = null,
    /** Total per day before Robolock steps in. Null means no daily budget. */
    val dailyLimitMillis: Long? = null,
    /** Prompt on entering the app, before any limit is reached. */
    val launchFriction: Boolean = false,
    val schedule: Schedule = Schedule(),
    /** Whether Focus Mode applies to this app. */
    val includeInFocus: Boolean = true,
)

/** What Robolock is currently looking at. */
sealed interface ForegroundRef {
    data class App(val packageName: String) : ForegroundRef

    /** A link handed to Robolock. The only case where a specific surface is genuinely known. */
    data class DeepLink(val surface: DeepLinkSurface) : ForegroundRef
}

data class RuleInput(
    val foreground: ForegroundRef?,
    val nowUtcMillis: Long,
    val nowElapsedMillis: Long,
    val bootId: String,
    val localNow: LocalDateTime,
    /** How long the current uninterrupted sitting has lasted. */
    val sessionMillis: Long,
    /** Foreground time accumulated today, per package. */
    val todayUsageMillis: Map<String, Long>,
    val grants: List<BypassGrant>,
    val rules: Map<String, AppRules>,
    val focusTier: FocusTier,
    val focusActive: Boolean,
    /** Master switch. When off, Robolock does nothing at all. */
    val protectionEnabled: Boolean = true,
)

sealed interface Decision {
    data object Allow : Decision

    data class AllowedByBypass(val untilUtc: Long) : Decision

    data class Intervene(
        val reason: InterventionReason,
        val packageName: String,
        val surface: DistractingSurface?,
        val allowContinue: Boolean,
        val countdownSeconds: Int,
        val graceMillis: Long,
        /** Other rules that also tripped. Recorded for context; does not change the outcome. */
        val alsoTripped: List<InterventionReason> = emptyList(),
    ) : Decision
}

/**
 * Decides whether to interrupt.
 *
 * Deliberately pure: same inputs, same answer, no clock of its own and no Android types. Every
 * temporal fact arrives through [RuleInput], which is what makes midnight, timezone and reboot
 * behaviour testable rather than hopeful.
 */
object BlockingRuleEngine {

    /**
     * Precedence, most-committed first.
     *
     * A schedule or a focus session is a decision made in advance and is respected over a limit
     * that merely accumulated. Launch friction is only a nudge, so anything real outranks it.
     */
    private val PRECEDENCE = listOf(
        InterventionReason.SCHEDULE,
        InterventionReason.FOCUS_MODE,
        InterventionReason.DAILY_LIMIT,
        InterventionReason.SESSION_LIMIT,
        InterventionReason.DEEP_LINK,
        InterventionReason.LAUNCH_FRICTION,
    )

    fun evaluate(input: RuleInput): Decision {
        if (!input.protectionEnabled) return Decision.Allow
        val foreground = input.foreground ?: return Decision.Allow

        val packageName = when (foreground) {
            is ForegroundRef.App -> foreground.packageName
            is ForegroundRef.DeepLink -> foreground.surface.appPackage
        }
        val surface = (foreground as? ForegroundRef.DeepLink)?.surface

        val rules = input.rules[packageName] ?: return Decision.Allow
        if (!rules.enabled) return Decision.Allow

        val tripped = buildList {
            if (rules.schedule.isActiveAt(input.localNow)) add(InterventionReason.SCHEDULE)
            if (input.focusActive && rules.includeInFocus) add(InterventionReason.FOCUS_MODE)

            val usedToday = input.todayUsageMillis[packageName] ?: 0L
            rules.dailyLimitMillis?.let { if (usedToday >= it) add(InterventionReason.DAILY_LIMIT) }
            rules.sessionLimitMillis?.let { if (input.sessionMillis >= it) add(InterventionReason.SESSION_LIMIT) }

            if (foreground is ForegroundRef.DeepLink) add(InterventionReason.DEEP_LINK)
            if (rules.launchFriction && input.sessionMillis == 0L) add(InterventionReason.LAUNCH_FRICTION)
        }

        if (tripped.isEmpty()) return Decision.Allow

        // Bypass is checked only once something has actually tripped, so a grant is never consumed
        // for an app the user was free to open anyway.
        activeGrant(input, packageName, surface)?.let { grant ->
            return Decision.AllowedByBypass(
                grant.effectiveWallExpiry(input.nowUtcMillis, input.nowElapsedMillis, input.bootId),
            )
        }

        val reason = PRECEDENCE.first { it in tripped }
        val tier = input.focusTier

        // Strict never offers a way through. Schedules are also a prior commitment, so continuing
        // past one is only offered when the tier allows it generally.
        val allowContinue = tier.allowContinue

        return Decision.Intervene(
            reason = reason,
            packageName = packageName,
            surface = surface,
            allowContinue = allowContinue,
            countdownSeconds = tier.countdownSeconds,
            graceMillis = tier.graceMinutes * 60_000L,
            alsoTripped = tripped.filterNot { it == reason },
        )
    }

    private fun activeGrant(
        input: RuleInput,
        packageName: String,
        surface: DistractingSurface?,
    ): BypassGrant? {
        val keys = setOfNotNull(packageName, surface?.id)
        return input.grants.firstOrNull { grant ->
            grant.scopeKey in keys &&
                grant.isActive(input.nowUtcMillis, input.nowElapsedMillis, input.bootId)
        }
    }
}
