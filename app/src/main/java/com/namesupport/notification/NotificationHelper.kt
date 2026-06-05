package com.namesupport.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.namesupport.R
import com.namesupport.model.ContactItem
import com.namesupport.receiver.ApprovalBroadcastReceiver

object NotificationHelper {

    const val CHANNEL_ID = "namesupport_approval"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showApprovalNotification(context: Context, contact: ContactItem) {
        val notifId = contact.id.toInt()

        val approveIntent = Intent(context, ApprovalBroadcastReceiver::class.java).apply {
            action = ApprovalBroadcastReceiver.ACTION_APPROVE
            putExtra(ApprovalBroadcastReceiver.EXTRA_CONTACT_ID, contact.id)
            putExtra(ApprovalBroadcastReceiver.EXTRA_CONTACT_NAME, contact.displayName)
            putExtra(ApprovalBroadcastReceiver.EXTRA_SUGGESTION, contact.suggestion)
            putExtra(ApprovalBroadcastReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val dismissIntent = Intent(context, ApprovalBroadcastReceiver::class.java).apply {
            action = ApprovalBroadcastReceiver.ACTION_DISMISS
            putExtra(ApprovalBroadcastReceiver.EXTRA_CONTACT_ID, contact.id)
            putExtra(ApprovalBroadcastReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePi = PendingIntent.getBroadcast(
            context, notifId * 2,
            approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPi = PendingIntent.getBroadcast(
            context, notifId * 2 + 1,
            dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title, contact.displayName))
            .setContentText(context.getString(R.string.notification_text, contact.suggestion))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_approve), approvePi)
            .addAction(0, context.getString(R.string.action_dismiss), dismissPi)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
