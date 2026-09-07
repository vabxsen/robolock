package com.robolock.app.monitoring

import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.robolock.app.RobolockApplication
import com.robolock.app.blocking.InterventionUiState
import com.robolock.app.blocking.InterventionOverlay
import com.robolock.app.blocking.OverlayController
import com.robolock.app.core.AppContainer
import com.robolock.app.core.log.RLog
import com.robolock.app.permissions.PermissionChecker
import com.robolock.app.ui.theme.RobolockTheme
import com.robolock.rules.ActiveSession
import com.robolock.rules.AppForegroundEvidence
import com.robolock.rules.BlockingRuleEngine
import com.robolock.rules.Decision
import com.robolock.rules.ForegroundRef
import com.robolock.rules.InterventionAction
import com.robolock.rules.SegmenterResult
import com.robolock.rules.SessionEndReason
import com.robolock.rules.SessionSegmenter
import com.robolock.rules.SurfaceCatalog
import com.robolock.rules.AppLevelSurface
import com.robolock.rules.RuleInput
import com.robolock.rules.toIntervention
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The service that actually enforces limits.
 *
 * A foreground service because the work is genuinely continuous while a watched app is open, and
 * because WorkManager's fifteen-minute floor would let a five-minute limit overrun by ten. It is
 * declared `specialUse` rather than `dataSync`, which on API 35 is capped at six cumulative hours
 * a day and would silently stop protecting people in the evening.
 *
 * The service does as little as it can get away with: polling suspends entirely when the screen is
 * off, and it stops itself outright when protection is turned off.
 */
class MonitorService : LifecycleService() {

    private lateinit var container: AppContainer
    private lateinit var permissions: PermissionChecker
    private lateinit var resolver: ForegroundAppResolver
    private lateinit var overlays: OverlayController

    private val segmenter = SessionSegmenter()
    private var session: ActiveSession? = null
    private var loop: Job? = null

    private var screenOn = true
    private var overlayShownFor: String? = null

    /** Set when a permission disappears underneath us, so the UI can say so instead of lying. */
    @Volatile
    private var degradedMessage: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    session?.let { session = (segmenter.onScreenOff(it, now()) as? SegmenterResult.Continuing)?.session }
                    overlays.hide()
                    overlayShownFor = null
                }
                Intent.ACTION_SCREEN_ON -> screenOn = true
            }
        }
    }

    // Held so they can be unregistered by identity in onDestroy.
    private var usageOpListener: AppOpsManager.OnOpChangedListener? = null
    private var overlayOpListener: AppOpsManager.OnOpChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as RobolockApplication).container
        permissions = PermissionChecker(this)
        resolver = ForegroundAppResolver(AndroidUsageEventSource(this), packageName)
        overlays = OverlayController(this)

        RobolockNotifications.ensureChannels(this)
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
        watchPermissionRevocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!startForegroundSafely()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (loop == null) loop = lifecycleScope.launch { monitorLoop() }
        return START_STICKY
    }

    /**
     * Promotes to foreground, or gives up cleanly.
     *
     * On API 31+ starting a foreground service from the background throws, so a start that arrives
     * after a background kill has to be handled rather than crashing the process.
     */
    private fun startForegroundSafely(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            RobolockNotifications.ID_MONITOR,
            RobolockNotifications.monitorNotification(this, "Watching the apps you chose."),
            type,
        )
        true
    } catch (e: Exception) {
        RLog.e("Could not start in the foreground", e)
        RobolockNotifications.post(
            this,
            RobolockNotifications.ID_DEGRADED,
            RobolockNotifications.degradedNotification(
                this,
                "Robolock stopped running in the background. Open the app to turn protection back on.",
            ),
        )
        false
    }

    private suspend fun monitorLoop() {
        while (lifecycleScope.isActive) {
            val settings = container.settings.current()

            if (!settings.protectionEnabled) {
                RLog.i("Protection disabled; stopping monitor")
                closeSession(SessionEndReason.PROTECTION_OFF)
                stopSelf()
                return
            }

            if (!permissions.hasUsageAccess()) {
                reportDegraded("Usage Access was turned off, so Robolock can't see which apps you're using.")
                delay(MonitorCadence.IDLE.intervalMillis)
                continue
            }
            clearDegraded()

            val cadence = tick(settings.protectionEnabled)
            if (cadence == MonitorCadence.SUSPENDED) {
                // Screen is off. Wait on the receiver rather than polling.
                delay(SUSPENDED_RECHECK_MILLIS)
            } else {
                delay(cadence.intervalMillis)
            }
        }
    }

    /** One observation. Returns the cadence to use before the next one. */
    private suspend fun tick(protectionEnabled: Boolean): MonitorCadence {
        if (!screenOn) {
            session?.let { current ->
                when (val result = segmenter.onTick(current, now())) {
                    is SegmenterResult.Closed -> {
                        persist(result.closed)
                        session = result.next
                    }
                    is SegmenterResult.Continuing -> session = result.session
                    SegmenterResult.Idle -> session = null
                }
            }
            return MonitorCadence.SUSPENDED
        }

        val foreground = resolver.poll(now())
        val watched = container.supportedApps.packages
        val isWatched = foreground != null && foreground in watched

        // Advance the session model.
        session = if (foreground != null) {
            when (val result = segmenter.onForeground(session, foreground, now())) {
                is SegmenterResult.Closed -> {
                    persist(result.closed)
                    result.next
                }
                is SegmenterResult.Continuing -> result.session
                SegmenterResult.Idle -> null
            }
        } else {
            (segmenter.onTick(session, now()) as? SegmenterResult.Continuing)?.session ?: session
        }

        if (!isWatched || foreground == null) {
            if (overlayShownFor != null) {
                overlays.hide()
                overlayShownFor = null
            }
            return MonitorCadence.of(screenOn, false, protectionEnabled)
        }

        evaluate(foreground)
        return MonitorCadence.of(screenOn, true, protectionEnabled)
    }

    private suspend fun evaluate(packageName: String) {
        val settings = container.settings.current()
        val clock = container.clock
        val current = session

        val input = RuleInput(
            foreground = ForegroundRef.App(packageName),
            nowUtcMillis = clock.nowUtcMillis(),
            nowElapsedMillis = clock.elapsedRealtime(),
            bootId = clock.bootId(),
            localNow = clock.localNow(),
            sessionMillis = current?.foregroundMillis ?: 0L,
            todayUsageMillis = todayUsage(packageName),
            grants = container.bypass.active(),
            rules = container.rules.current(),
            focusTier = settings.focusTier,
            focusActive = focusActive(settings.focusSchedule, settings.focusWindows, clock.localNow()),
            protectionEnabled = settings.protectionEnabled,
        )

        when (val decision = BlockingRuleEngine.evaluate(input)) {
            is Decision.Allow, is Decision.AllowedByBypass -> {
                if (overlayShownFor == packageName) {
                    overlays.hide()
                    overlayShownFor = null
                }
            }
            is Decision.Intervene -> showIntervention(packageName, decision)
        }
    }

    private suspend fun showIntervention(packageName: String, decision: Decision.Intervene) {
        if (overlayShownFor == packageName) return

        val app = container.supportedApps.byPackage(packageName)
        val state = InterventionUiState(
            packageName = packageName,
            appName = app?.displayName ?: packageName,
            icon = container.installedApps.states().firstOrNull { it.app.packageName == packageName }?.icon,
            reason = decision.reason,
            sessionMillis = session?.foregroundMillis ?: 0L,
            allowContinue = decision.allowContinue,
            countdownSeconds = decision.countdownSeconds,
        )

        val shown = overlays.show(
            overPackage = packageName,
            onBackPressed = { onGoBack(packageName, decision) },
            content = {
                RobolockTheme {
                    InterventionOverlay(
                        state = state,
                        onGoBack = { onGoBack(packageName, decision) },
                        onContinue = { onContinue(packageName, decision) },
                        onAdjustLimits = { onAdjustLimits() },
                    )
                }
            },
        )

        if (shown) {
            overlayShownFor = packageName
        } else {
            // Could not draw over other apps — say something rather than failing silently.
            RobolockNotifications.post(
                this,
                RobolockNotifications.ID_INTERVENTION_FALLBACK,
                RobolockNotifications.interventionFallback(this, state.appName),
            )
            record(packageName, decision, InterventionAction.TIMED_OUT)
        }
    }

    private fun onGoBack(packageName: String, decision: Decision.Intervene) {
        overlays.hide()
        overlayShownFor = null
        goHome()
        lifecycleScope.launch { record(packageName, decision, InterventionAction.EXITED) }
    }

    private fun onContinue(packageName: String, decision: Decision.Intervene) {
        overlays.hide()
        overlayShownFor = null
        lifecycleScope.launch {
            // The grace period is what stops the overlay reappearing the instant it is dismissed.
            container.bypass.grant(packageName, decision.graceMillis)
            record(packageName, decision, InterventionAction.CONTINUED)
        }
    }

    private fun onAdjustLimits() {
        overlays.hide()
        overlayShownFor = null
        startActivity(
            packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Records what happened.
     *
     * Note the route: a package name becomes an [AppLevelSurface] from the catalog, which becomes
     * [AppForegroundEvidence], which becomes the stored event. There is no way to write a record
     * for a surface Robolock did not observe, because there is no constructor that accepts one.
     */
    private suspend fun record(packageName: String, decision: Decision.Intervene, action: InterventionAction) {
        val surface = SurfaceCatalog.appSurfaceFor(
            packageName = packageName,
            displayName = container.supportedApps.byPackage(packageName)?.displayName ?: packageName,
        )

        val evidence = AppForegroundEvidence(
            surface = surface,
            atUtcMillis = container.clock.nowUtcMillis(),
            sessionMillisBefore = session?.foregroundMillis ?: 0L,
        )
        container.statistics.record(
            evidence.toIntervention(
                reason = decision.reason,
                action = action,
                bypassMillis = if (action == InterventionAction.CONTINUED) decision.graceMillis else null,
            ),
        )
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private suspend fun todayUsage(packageName: String): Map<String, Long> = mapOf(
        packageName to container.statistics.foregroundMillisToday(packageName),
    )

    private fun focusActive(
        schedule: com.robolock.rules.FocusSchedule,
        windows: com.robolock.rules.Schedule,
        localNow: java.time.LocalDateTime,
    ): Boolean = when (schedule) {
        com.robolock.rules.FocusSchedule.ALWAYS -> true
        com.robolock.rules.FocusSchedule.SELECTED_HOURS,
        com.robolock.rules.FocusSchedule.CUSTOM,
        -> windows.isActiveAt(localNow)
    }

    private suspend fun persist(closed: com.robolock.rules.ClosedSession) {
        container.statistics.recordSession(closed)
    }

    private suspend fun closeSession(reason: SessionEndReason) {
        session?.let { persist(segmenter.forceClose(it, now(), reason)) }
        session = null
    }

    /** Immediate callbacks when the user revokes a permission in Settings. */
    private fun watchPermissionRevocation() {
        val appOps = getSystemService(AppOpsManager::class.java) ?: return
        runCatching {
            val usage = AppOpsManager.OnOpChangedListener { _, _ ->
                if (!permissions.hasUsageAccess()) {
                    reportDegraded("Usage Access was turned off, so Robolock can't see which apps you're using.")
                }
            }
            val overlay = AppOpsManager.OnOpChangedListener { _, _ ->
                if (!permissions.hasOverlay()) {
                    overlays.hide()
                    overlayShownFor = null
                }
            }
            appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS, packageName, usage)
            appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, overlay)
            usageOpListener = usage
            overlayOpListener = overlay
        }
    }

    private fun reportDegraded(message: String) {
        if (degradedMessage == message) return
        degradedMessage = message
        RLog.w("Degraded: $message")
        RobolockNotifications.post(
            this,
            RobolockNotifications.ID_DEGRADED,
            RobolockNotifications.degradedNotification(this, message),
        )
    }

    private fun clearDegraded() {
        if (degradedMessage == null) return
        degradedMessage = null
        RobolockNotifications.cancel(this, RobolockNotifications.ID_DEGRADED)
    }

    private fun now() = container.clock.nowUtcMillis()

    override fun onDestroy() {
        loop?.cancel()
        loop = null
        overlays.hide()
        runCatching { unregisterReceiver(screenReceiver) }
        getSystemService(AppOpsManager::class.java)?.let { appOps ->
            usageOpListener?.let { runCatching { appOps.stopWatchingMode(it) } }
            overlayOpListener?.let { runCatching { appOps.stopWatchingMode(it) } }
        }
        usageOpListener = null
        overlayOpListener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private const val SUSPENDED_RECHECK_MILLIS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                RLog.e("Could not start monitor service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
