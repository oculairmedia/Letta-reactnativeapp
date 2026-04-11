package com.letta.mobile.channel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.letta.mobile.NotificationNavigationTarget
import com.letta.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ChannelNotification(
    val agentId: String,
    val agentName: String,
    val conversationId: String,
    val conversationSummary: String?,
    val messageId: String,
    val messagePreview: String,
)

@Singleton
class ChannelNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_notifications_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_notifications_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun publish(notification: ChannelNotification) {
        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val target = NotificationNavigationTarget(
            agentId = notification.agentId,
            conversationId = notification.conversationId,
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.conversationId.hashCode(),
            target.createIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = notification.agentName.ifBlank {
            context.getString(R.string.channel_notifications_fallback_title)
        }
        val messageText = notification.messagePreview.ifBlank {
            context.getString(R.string.channel_notifications_fallback_text)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setSubText(notification.conversationSummary)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context).notify(notification.conversationId.hashCode(), builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "letta-agent-updates"
    }
}
