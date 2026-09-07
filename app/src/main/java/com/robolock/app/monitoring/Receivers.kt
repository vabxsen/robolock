package com.robolock.app.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.robolock.app.RobolockApplication
import com.robolock.app.core.log.RLog
import com.robolock.app.permissions.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts protection after a reboot or an app update.
 *
 * BOOT_COMPLETED is one of the few contexts where starting a foreground service from the
 * background is still permitted, which is what makes this the right place to do it.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pending = goAsync()
        val app = context.applicationContext as? RobolockApplication ?: run {
            pending.finish(); return
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = app.container
                val settings = container.settings.current()

                // A sitting that was live when the device went down is closed rather than left
                // dangling, so its time is credited instead of silently disappearing.
                container.bypass.pruneExpired()

                if (settings.protectionEnabled && PermissionChecker(context).hasUsageAccess()) {
                    RLog.i("Restarting protection after $action")
                    MonitorService.start(context)
                } else {
                    RLog.i("Not restarting after $action: protection off or usage access missing")
                }
            } catch (e: Exception) {
                RLog.e("Boot restart failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Handles the clock or timezone moving.
 *
 * Both invalidate assumptions the rules depend on: a bypass deadline was written against the old
 * wall clock, and daily budgets roll over at a local midnight that may have just moved.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_TIME_CHANGED && action != Intent.ACTION_TIMEZONE_CHANGED) return

        val pending = goAsync()
        val app = context.applicationContext as? RobolockApplication ?: run {
            pending.finish(); return
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                RLog.i("Clock event: $action")
                // Re-anchor wall-clock deadlines against the monotonic clock, which did not move.
                app.container.bypass.reanchorAfterClockChange()
                app.container.bypass.pruneExpired()
            } catch (e: Exception) {
                RLog.e("Clock change handling failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
