package com.robolock.app.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.robolock.app.RobolockApplication
import com.robolock.app.blocking.LinkInterceptActivity
import com.robolock.app.core.AppContainer
import com.robolock.app.data.apps.InstalledAppState
import com.robolock.app.data.settings.RobolockSettings
import com.robolock.app.data.stats.StatsSnapshot
import com.robolock.app.monitoring.MonitorService
import com.robolock.app.permissions.PermissionState
import com.robolock.app.permissions.PermissionChecker
import com.robolock.rules.AppRules
import com.robolock.rules.DetectionCapability
import com.robolock.rules.DistractingSurface
import com.robolock.rules.FocusSchedule
import com.robolock.rules.FocusTier
import com.robolock.rules.Schedule
import com.robolock.rules.SurfaceCatalog
import com.robolock.rules.ThemePreference
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun Application.container(): AppContainer = (this as RobolockApplication).container

/** Shared base so every screen model reaches the same graph the same way. */
abstract class RobolockViewModel(app: Application) : AndroidViewModel(app) {
    protected val container: AppContainer = app.container()
}

// --- Home --------------------------------------------------------------------------------------

data class HomeUiState(
    val settings: RobolockSettings = RobolockSettings(),
    val today: StatsSnapshot = StatsSnapshot(),
    val week: StatsSnapshot = StatsSnapshot(),
    val apps: List<InstalledAppState> = emptyList(),
    val permissions: PermissionState = PermissionState(),
    val loading: Boolean = true,
) {
    /**
     * Whether the app is genuinely protecting anything right now.
     *
     * Both halves matter: a switch turned on without Usage Access protects nothing, and saying
     * otherwise would be the exact dishonesty this app is built to avoid.
     */
    val actuallyProtecting: Boolean
        get() = settings.protectionEnabled && permissions.canMonitor

    val needsAttention: Boolean
        get() = settings.protectionEnabled && !permissions.canMonitor
}

class HomeViewModel(app: Application) : RobolockViewModel(app) {

    private val permissionChecker = PermissionChecker(app)
    private val permissions = MutableStateFlow(permissionChecker.state())
    private val apps = MutableStateFlow(emptyList<InstalledAppState>())

    val state: StateFlow<HomeUiState> = combine(
        container.settings.settings,
        container.statistics.observeToday(),
        container.statistics.observeWeek(),
        apps,
        permissions,
    ) { settings, today, week, appList, perms ->
        HomeUiState(settings, today, week, appList, perms, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    /** Called whenever the screen is resumed, so returning from Settings shows the truth. */
    fun refresh() {
        permissions.value = permissionChecker.state()
        viewModelScope.launch { apps.value = container.installedApps.states() }
    }

    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settings.setProtectionEnabled(enabled)
            if (enabled) MonitorService.start(getApplication()) else MonitorService.stop(getApplication())
        }
    }
}

// --- Stats -------------------------------------------------------------------------------------

enum class StatsPeriod(val label: String, val days: Long) {
    DAY("Day", 1),
    WEEK("Week", 7),
    MONTH("Month", 30),
    ALL("All", 3650),
}

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val snapshot: StatsSnapshot = StatsSnapshot(),
    val apps: List<InstalledAppState> = emptyList(),
    val rangeLabel: String = "",
)

class StatsViewModel(app: Application) : RobolockViewModel(app) {

    private val period = MutableStateFlow(StatsPeriod.WEEK)
    private val apps = MutableStateFlow(emptyList<InstalledAppState>())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val snapshot = period.flatMapLatest { p ->
        val today = LocalDate.now()
        container.statistics.observeRange(today.minusDays(p.days - 1), today)
    }

    val state: StateFlow<StatsUiState> = combine(period, snapshot, apps) { p, snap, appList ->
        StatsUiState(p, snap, appList, rangeLabel = rangeLabel(p))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    init {
        viewModelScope.launch { apps.value = container.installedApps.states() }
    }

    fun setPeriod(next: StatsPeriod) {
        period.value = next
    }

    private fun rangeLabel(p: StatsPeriod): String {
        val today = LocalDate.now()
        return when (p) {
            StatsPeriod.DAY -> "Today"
            StatsPeriod.ALL -> "All time"
            else -> {
                val from = today.minusDays(p.days - 1)
                "${from.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${from.dayOfMonth} – " +
                    "${today.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${today.dayOfMonth}"
            }
        }
    }
}

// --- Focus -------------------------------------------------------------------------------------

data class FocusUiState(
    val tier: FocusTier = FocusTier.DEFAULT,
    val schedule: FocusSchedule = FocusSchedule.DEFAULT,
    val windows: Schedule = Schedule(),
    val saved: Boolean = false,
)

class FocusViewModel(app: Application) : RobolockViewModel(app) {

    private val pendingTier = MutableStateFlow<FocusTier?>(null)
    private val pendingSchedule = MutableStateFlow<FocusSchedule?>(null)
    private val saved = MutableStateFlow(false)

    val state: StateFlow<FocusUiState> = combine(
        container.settings.settings,
        pendingTier,
        pendingSchedule,
        saved,
    ) { settings, tier, schedule, wasSaved ->
        FocusUiState(
            tier = tier ?: settings.focusTier,
            schedule = schedule ?: settings.focusSchedule,
            windows = settings.focusWindows,
            saved = wasSaved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun selectTier(tier: FocusTier) {
        pendingTier.value = tier
        saved.value = false
    }

    fun selectSchedule(schedule: FocusSchedule) {
        pendingSchedule.value = schedule
        saved.value = false
    }

    fun save() {
        viewModelScope.launch {
            pendingTier.value?.let { container.settings.setFocusTier(it) }
            pendingSchedule.value?.let { container.settings.setFocusSchedule(it) }
            pendingTier.value = null
            pendingSchedule.value = null
            saved.value = true
        }
    }
}

// --- App controls ------------------------------------------------------------------------------

data class AppControlsUiState(
    val apps: List<InstalledAppState> = emptyList(),
    val rules: Map<String, AppRules> = emptyMap(),
)

class AppControlsViewModel(app: Application) : RobolockViewModel(app) {

    private val apps = MutableStateFlow(emptyList<InstalledAppState>())

    val state: StateFlow<AppControlsUiState> =
        combine(apps, container.rules.observeAll()) { appList, rules ->
            AppControlsUiState(appList, rules)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppControlsUiState())

    init {
        viewModelScope.launch { apps.value = container.installedApps.states() }
    }

    fun setEnabled(packageName: String, enabled: Boolean) = update(packageName) { it.copy(enabled = enabled) }

    fun setSessionLimit(packageName: String, millis: Long?) =
        update(packageName) { it.copy(sessionLimitMillis = millis) }

    fun setDailyLimit(packageName: String, millis: Long?) =
        update(packageName) { it.copy(dailyLimitMillis = millis) }

    fun setLaunchFriction(packageName: String, enabled: Boolean) =
        update(packageName) { it.copy(launchFriction = enabled) }

    fun setSchedule(packageName: String, schedule: Schedule) =
        update(packageName) { it.copy(schedule = schedule) }

    private fun update(packageName: String, transform: (AppRules) -> AppRules) {
        viewModelScope.launch { container.rules.update(packageName, transform) }
    }
}

// --- Permissions -------------------------------------------------------------------------------

class PermissionsViewModel(app: Application) : RobolockViewModel(app) {

    private val checker = PermissionChecker(app)
    private val _state = MutableStateFlow(checker.state())
    val state: StateFlow<PermissionState> = _state

    /** Re-read from the system. Called on resume, so returning from Settings updates the screen. */
    fun refresh() {
        _state.value = checker.state()
    }

    fun intentFor(permission: com.robolock.app.permissions.RobolockPermission) =
        checker.intentFor(permission)

    /**
     * Finishes the welcome flow.
     *
     * Protection is only switched on when Usage Access is actually granted, so the app never
     * shows itself as protecting while it has no way to see anything. If the permission is
     * missing the user still reaches the dashboard, where the banner explains why.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            container.settings.setOnboardingComplete(true)
            if (checker.hasUsageAccess()) {
                container.settings.setProtectionEnabled(true)
                MonitorService.start(getApplication())
            }
        }
    }
}

// --- Settings and distractions -----------------------------------------------------------------

data class SurfacePreference(
    val surface: DistractingSurface,
    val selected: Boolean,
) {
    val detectable: Boolean get() = surface.capability != DetectionCapability.UNSUPPORTED
}

data class SettingsUiState(
    val settings: RobolockSettings = RobolockSettings(),
    val surfaces: List<SurfacePreference> = emptyList(),
)

class SettingsViewModel(app: Application) : RobolockViewModel(app) {

    val state: StateFlow<SettingsUiState> = container.settings.settings
        .let { flow ->
            combine(flow, MutableStateFlow(Unit)) { settings, _ ->
                SettingsUiState(
                    settings = settings,
                    surfaces = SurfaceCatalog.selectable.map { surface ->
                        SurfacePreference(surface, surface.id in settings.avoidedSurfaceIds)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(theme: ThemePreference) = viewModelScope.launch { container.settings.setTheme(theme) }

    fun setDailySummary(enabled: Boolean) =
        viewModelScope.launch { container.settings.setDailySummaryEnabled(enabled) }

    /**
     * Opts in or out of link interception.
     *
     * The preference alone would do nothing, so this also enables the manifest alias — that
     * component is what actually makes Robolock appear as a handler for Shorts and Reels links.
     * Turning it off removes Robolock from the chooser entirely.
     */
    fun setInterceptLinks(enabled: Boolean) {
        viewModelScope.launch {
            container.settings.setInterceptLinks(enabled)

            val app = getApplication<Application>()
            app.packageManager.setComponentEnabledSetting(
                ComponentName(app, LinkInterceptActivity.ALIAS),
                if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    fun toggleSurface(surface: DistractingSurface, selected: Boolean) {
        viewModelScope.launch {
            val current = container.settings.current().avoidedSurfaceIds
            val next = if (selected) current + surface.id else current - surface.id
            container.settings.setAvoidedSurfaces(next)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            container.statistics.clearAll()
            container.bypass.clear()
        }
    }
}

/** One factory for every screen model, so the Activity does not need a DI framework. */
object RobolockViewModelFactory {
    fun create(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(app)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(app)
            modelClass.isAssignableFrom(FocusViewModel::class.java) -> FocusViewModel(app)
            modelClass.isAssignableFrom(AppControlsViewModel::class.java) -> AppControlsViewModel(app)
            modelClass.isAssignableFrom(PermissionsViewModel::class.java) -> PermissionsViewModel(app)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(app)
            else -> error("Unknown ViewModel ${modelClass.name}")
        } as T
    }
}
