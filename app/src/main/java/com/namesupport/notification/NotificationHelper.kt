package com.namesupport.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.namesupport.MainActivity
import com.namesupport.R

object NotificationHelper {

    const val FOREGROUND_NOTIFICATION_ID = 1
    const val SYNC_NOTIFICATION_ID = 2

    private const val CHANNEL_MONITOR = "channel_monitor"
    private const val CHANNEL_SYNC = "channel_sync"

    /** Must be called once at app startup (e.g. in MainActivity.onCreate). */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.notif_channel_monitor_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_monitor_desc)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.notif_channel_sync_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_sync_desc)
            }
        )
    }

    /** Persistent notification shown while the foreground service is alive. */
    fun buildForegroundNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(context.getString(R.string.notif_monitor_title))
            .setContentText(context.getString(R.string.notif_monitor_text))
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** One-shot notification shown when new contacts were auto-transliterated. */
    fun showContactsUpdatedNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = context.resources.getQuantityString(
            R.plurals.notif_sync_text, count, count
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setContentTitle(context.getString(R.string.notif_sync_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(SYNC_NOTIFICATION_ID, notification)
    }
}
