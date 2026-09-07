package com.robolock.app.core

import android.content.Context
import com.robolock.app.core.time.Clock
import com.robolock.app.core.time.SystemClockSource
import com.robolock.app.data.apps.InstalledAppsRepository
import com.robolock.app.data.bypass.BypassRepository
import com.robolock.app.data.db.RobolockDatabase
import com.robolock.app.data.rules.RulesRepository
import com.robolock.app.data.settings.SettingsRepository
import com.robolock.app.data.stats.StatisticsRepository
import com.robolock.rules.SupportedAppRegistry

/**
 * Manual dependency graph.
 *
 * Constructed once by the Application and read from the UI layer. A DI framework would earn its
 * keep at a larger size than this; here it would only add build time.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val clock: Clock = SystemClockSource()

    /**
     * Which apps Robolock watches.
     *
     * The debug source set overrides this so enforcement can be exercised on an emulator, where
     * Instagram, YouTube and WhatsApp are not installed.
     */
    val supportedApps: SupportedAppRegistry = BuildRegistry.registry

    private val database: RobolockDatabase by lazy { RobolockDatabase.build(appContext) }

    val installedApps: InstalledAppsRepository = InstalledAppsRepository(appContext, supportedApps)

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val rules: RulesRepository by lazy { RulesRepository(database.appRules(), supportedApps) }

    val bypass: BypassRepository by lazy { BypassRepository(database.bypassGrants(), clock) }

    val statistics: StatisticsRepository by lazy {
        StatisticsRepository(
            events = database.interventionEvents(),
            sessions = database.sessions(),
            daily = database.dailyUsage(),
            clock = clock,
        )
    }
}
