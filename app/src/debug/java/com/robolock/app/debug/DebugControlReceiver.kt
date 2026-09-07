package com.robolock.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.robolock.app.RobolockApplication
import com.robolock.app.core.log.RLog
import com.robolock.app.monitoring.MonitorService
import com.robolock.rules.AppRules
import com.robolock.rules.FocusTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only control surface, driven over adb.
 *
 * Exists so the enforcement pipeline can be exercised before the settings UI is built, and so
 * limits can be set to a few seconds instead of waiting fifteen real minutes to see an overlay.
 * It drives the same repositories the UI does — nothing here is a shortcut around the rule engine,
 * and none of it exists in a release build.
 *
 *   adb shell am broadcast -a com.robolock.app.debug.SEED \
 *     -e pkg com.android.chrome --ei sessionSeconds 10 -n com.robolock.app.debug/com.robolock.app.debug.DebugControlReceiver
 */
class DebugControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RobolockApplication ?: return
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_SEED -> {
                        val pkg = intent.getStringExtra("pkg") ?: "com.android.chrome"
                        val sessionSeconds = intent.getIntExtra("sessionSeconds", 10)
                        val friction = intent.getBooleanExtra("friction", false)
                        val tier = FocusTier.fromId(intent.getStringExtra("tier"))

                        app.container.settings.setProtectionEnabled(true)
                        app.container.settings.setOnboardingComplete(true)
                        app.container.settings.setFocusTier(tier)
                        app.container.rules.save(
                            AppRules(
                                packageName = pkg,
                                enabled = true,
                                sessionLimitMillis = sessionSeconds * 1000L,
                                launchFriction = friction,
                            ),
                        )
                        RLog.i("DEBUG seeded: $pkg session=${sessionSeconds}s friction=$friction tier=${tier.id}")
                        MonitorService.start(context)
                    }

                    ACTION_STOP -> {
                        app.container.settings.setProtectionEnabled(false)
                        MonitorService.stop(context)
                        RLog.i("DEBUG protection stopped")
                    }

                    ACTION_RESET -> {
                        app.container.statistics.clearAll()
                        app.container.bypass.clear()
                        app.container.rules.clear()
                        app.container.settings.clear()
                        RLog.i("DEBUG state cleared")
                    }

                    ACTION_DUMP -> {
                        val settings = app.container.settings.current()
                        val rules = app.container.rules.current()
                        val grants = app.container.bypass.active()
                        RLog.i("DEBUG settings=$settings")
                        RLog.i("DEBUG rules=${rules.values.filter { it.enabled }}")
                        RLog.i("DEBUG grants=$grants")
                    }
                }
            } catch (e: Exception) {
                RLog.e("DEBUG command failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SEED = "com.robolock.app.debug.SEED"
        const val ACTION_STOP = "com.robolock.app.debug.STOP"
        const val ACTION_RESET = "com.robolock.app.debug.RESET"
        const val ACTION_DUMP = "com.robolock.app.debug.DUMP"
    }
}
