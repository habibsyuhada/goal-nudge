package com.goalnudge.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.goalnudge.app.R
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun buildServiceNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_OVERLAY_SERVICE)
            .setContentTitle(context.getString(R.string.notification_channel_overlay_service))
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    /**
     * Notifikasi silent untuk [com.goalnudge.app.service.NudgeListenerService]. ID-nya sengaja
     * berbeda dari [SERVICE_NOTIFICATION_ID] — kedua foreground service ini bisa jalan
     * bersamaan (listener persisten + overlay service sesaat), dan berbagi ID notifikasi bikin
     * salah satu ke-cancel saat yang lain berhenti.
     */
    fun buildListenerServiceNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_OVERLAY_SERVICE)
            .setContentTitle(context.getString(R.string.notification_channel_overlay_service))
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    /** Fallback saat overlay tidak bisa muncul (izin dicabut / gagal ditambahkan ke WindowManager). */
    fun showFallbackNudge(content: NudgeContent) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val openAppIntent = PendingIntent.getActivity(
            context, content.goalId.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FALLBACK)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${content.body}\n\n${content.checkInLine}"))
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(FALLBACK_NOTIFICATION_ID_BASE + content.goalId.toInt(), notification)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY_SERVICE = "overlay_service"
        const val CHANNEL_FALLBACK = "nudge_fallback"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val LISTENER_SERVICE_NOTIFICATION_ID = 1003
        private const val FALLBACK_NOTIFICATION_ID_BASE = 2000
    }
}
