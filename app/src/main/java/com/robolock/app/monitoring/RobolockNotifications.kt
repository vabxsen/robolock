package com.robolock.app.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.robolock.app.MainActivity
import com.robolock.app.R

/**
 * Every notification Robolock posts.
 *
 * Kept deliberately few. The ongoing one is required by the platform to run a foreground service
 * and is set to the lowest importance so it sits silently in the shade; the others only appear
 * when something genuinely needs a decision.
 */
object RobolockNotifications {

    const val CHANNEL_MONITOR = "robolock_monitor"
    const val CHANNEL_ALERTS = "robolock_alerts"
    const val CHANNEL_SUMMARY = "robolock_summary"

    const val ID_MONITOR = 1001
    const val ID_DEGRADED = 1002
    const val ID_INTERVENTION_FALLBACK = 1003
    const val ID_SUMMARY = 1004

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                "Protection status",
                // MIN so the required ongoing notification stays out of the way.
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Shows that Robolock is watching the apps you chose." },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Attention needed",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Shown when Robolock cannot protect your apps." },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                context.getString(R.string.summary_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun monitorNotification(context: Context, contentText: String) =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_robolock_mark)
            .setContentTitle("Robolock is on")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openApp(context))
            .build()

    /**
     * Shown when a permission was revoked while running.
     *
     * The app must say so out loud rather than quietly stopping — a wellbeing tool that has
     * silently stopped working is worse than one that was never turned on.
     */
    fun degradedNotification(context: Context, message: String) =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_robolock_mark)
            .setContentTitle("Robolock needs attention")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context))
            .build()

    /**
     * The fallback when an overlay cannot be drawn.
     *
     * The user's tap is what launches our Activity, which is a legitimate way to get to the
     * foreground when a background start would be refused.
     */
    fun interventionFallback(context: Context, appName: String) =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_robolock_mark)
            .setContentTitle("$appName — time to pause")
            .setContentText("You've reached the limit you set. Tap to review.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp(context))
            .build()

    fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted; nothing to do but carry on protecting.
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
