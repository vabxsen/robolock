package com.robolock.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.robolock.rules.FocusSchedule
import com.robolock.rules.FocusTier
import com.robolock.rules.Schedule
import com.robolock.rules.ScheduleCodec
import com.robolock.rules.SurfaceCatalog
import com.robolock.rules.ThemePreference
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * App-wide scalar preferences.
 *
 * Only settings live here. Anything written on the enforcement hot path, or needing a transaction,
 * belongs in Room instead.
 */
data class RobolockSettings(
    val protectionEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
    val theme: ThemePreference = ThemePreference.DEFAULT,
    val focusTier: FocusTier = FocusTier.DEFAULT,
    val focusSchedule: FocusSchedule = FocusSchedule.DEFAULT,
    val focusWindows: Schedule = Schedule(),
    /**
     * Surfaces the user says they want less of.
     *
     * A preference, not a detection claim. Selecting an undetectable surface shapes the copy and
     * the intentional-use prompts; it can never produce a statistic. See DistractionsRepository.
     */
    val avoidedSurfaceIds: Set<String> = emptySet(),
    val dailySummaryEnabled: Boolean = true,
    /** Opt-in. Enables the disabled-by-default link-catching alias. */
    val interceptLinks: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("robolock_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val protectionEnabled = booleanPreferencesKey("protection_enabled")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val theme = stringPreferencesKey("theme")
        val focusTier = stringPreferencesKey("focus_tier")
        val focusSchedule = stringPreferencesKey("focus_schedule")
        val focusWindows = stringPreferencesKey("focus_windows")
        val avoidedSurfaces = stringSetPreferencesKey("avoided_surfaces")
        val dailySummary = booleanPreferencesKey("daily_summary")
        val interceptLinks = booleanPreferencesKey("intercept_links")
    }

    val settings: Flow<RobolockSettings> = store.data
        // A corrupt preferences file must not take the app down with it.
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { prefs ->
            RobolockSettings(
                protectionEnabled = prefs[Keys.protectionEnabled] ?: false,
                onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
                theme = ThemePreference.fromId(prefs[Keys.theme]),
                focusTier = FocusTier.fromId(prefs[Keys.focusTier]),
                focusSchedule = FocusSchedule.fromId(prefs[Keys.focusSchedule]),
                focusWindows = ScheduleCodec.decode(prefs[Keys.focusWindows].orEmpty()),
                // Drop ids that no longer exist, so removing a surface cannot resurrect a ghost.
                avoidedSurfaceIds = (prefs[Keys.avoidedSurfaces] ?: defaultAvoided())
                    .filter { SurfaceCatalog.byId(it) != null }
                    .toSet(),
                dailySummaryEnabled = prefs[Keys.dailySummary] ?: true,
                interceptLinks = prefs[Keys.interceptLinks] ?: false,
            )
        }

    suspend fun current(): RobolockSettings = settings.first()

    suspend fun setProtectionEnabled(enabled: Boolean) =
        put(Keys.protectionEnabled, enabled)

    suspend fun setOnboardingComplete(complete: Boolean) =
        put(Keys.onboardingComplete, complete)

    suspend fun setTheme(theme: ThemePreference) = put(Keys.theme, theme.id)

    suspend fun setFocusTier(tier: FocusTier) = put(Keys.focusTier, tier.id)

    suspend fun setFocusSchedule(schedule: FocusSchedule) = put(Keys.focusSchedule, schedule.id)

    suspend fun setFocusWindows(windows: Schedule) =
        put(Keys.focusWindows, ScheduleCodec.encode(windows))

    suspend fun setAvoidedSurfaces(ids: Set<String>) = put(Keys.avoidedSurfaces, ids)

    suspend fun setDailySummaryEnabled(enabled: Boolean) = put(Keys.dailySummary, enabled)

    suspend fun setInterceptLinks(enabled: Boolean) = put(Keys.interceptLinks, enabled)

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private fun defaultAvoided(): Set<String> = SurfaceCatalog.selectable.map { it.id }.toSet()
}
