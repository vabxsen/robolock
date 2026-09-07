package com.robolock.rules

enum class ThemePreference(val id: String, val displayName: String, val description: String) {
    DARK("dark", "Dark", "Robolock's tuned dark palette. Recommended."),
    LIGHT("light", "Light", "A light palette using the same layout."),
    SYSTEM("system", "System", "Follow the device setting.");

    companion object {
        val DEFAULT = DARK
        fun fromId(id: String?): ThemePreference = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * How firmly Robolock pushes back, shown on the Focus Mode screen.
 *
 * These are three settings of the same mechanism rather than three code paths: each tier just
 * changes whether continuing is offered, how long the countdown runs, and how long the grace
 * period lasts afterwards.
 */
enum class FocusTier(
    val id: String,
    val displayName: String,
    val description: String,
    val allowContinue: Boolean,
    val countdownSeconds: Int,
    val graceMinutes: Int,
) {
    BALANCED(
        id = "balanced",
        displayName = "Balanced",
        description = "Reduce distractions without completely blocking apps.",
        allowContinue = true,
        countdownSeconds = 5,
        graceMinutes = 15,
    ),
    FRICTION(
        id = "friction",
        displayName = "Friction",
        description = "Pause before opening selected apps.",
        allowContinue = true,
        countdownSeconds = 20,
        graceMinutes = 5,
    ),
    STRICT(
        id = "strict",
        displayName = "Strict",
        description = "Block selected apps during Focus Mode.",
        allowContinue = false,
        countdownSeconds = 0,
        graceMinutes = 0,
    );

    companion object {
        val DEFAULT = BALANCED
        fun fromId(id: String?): FocusTier = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** When Focus Mode applies. */
enum class FocusSchedule(val id: String, val displayName: String) {
    ALWAYS("always", "Always"),
    SELECTED_HOURS("selected_hours", "During selected hours"),
    CUSTOM("custom", "Custom");

    companion object {
        val DEFAULT = SELECTED_HOURS
        fun fromId(id: String?): FocusSchedule = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
