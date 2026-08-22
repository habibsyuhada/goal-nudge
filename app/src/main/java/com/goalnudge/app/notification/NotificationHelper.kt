package com.goalnudge.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.goalnudge.app.R
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.NudgeWindow
import com.goalnudge.app.domain.model.Tone
import com.goalnudge.app.ui.MainActivity
import com.goalnudge.app.ui.nudge.NudgeFullScreenActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Notifikasi silent untuk [com.goalnudge.app.service.NudgeListenerService] — foreground
     * service persisten yang menjaga receiver `ACTION_SCREEN_ON` tetap terdaftar.
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

    /**
     * Jalur utama nudge sekarang: notifikasi dengan [NotificationCompat.Builder.setFullScreenIntent]
     * yang membuka [NudgeFullScreenActivity] penuh layar (bahkan di atas lock screen), dipanggil
     * baik dari layar-menyala realtime ([com.goalnudge.app.service.NudgeListenerService]) maupun
     * dari jaring pengaman terjadwal ([com.goalnudge.app.scheduling.NudgeScheduleWorker]) —
     * keduanya memberi pengalaman yang sama. PENTING: `setFullScreenIntent` cuma auto-membuka
     * activity kalau notifikasi ini di-post SAAT layar mati/masih terkunci (pembatasan resmi
     * Android sejak API 29) — makanya pemanggilnya harus di momen layar baru menyala, BUKAN
     * setelah user selesai unlock (lihat catatan di NudgeListenerService). Kalau HP sudah
     * kadung unlocked, atau OS mendemosikannya (Android 14+ tanpa izin USE_FULL_SCREEN_INTENT —
     * lihat OnboardingScreen), tap notifikasi tetap membuka activity yang sama.
     */
    fun showUrgentNudge(content: NudgeContent, window: NudgeWindow, tone: Tone) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notificationId = URGENT_NOTIFICATION_ID_BASE + content.goalId.toInt()
        val activityIntent = Intent(context, NudgeFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NudgeFullScreenActivity.EXTRA_GOAL_ID, content.goalId)
            putExtra(NudgeFullScreenActivity.EXTRA_TITLE, content.title)
            putExtra(NudgeFullScreenActivity.EXTRA_BODY, content.body)
            putExtra(NudgeFullScreenActivity.EXTRA_WHY, content.why)
            putExtra(NudgeFullScreenActivity.EXTRA_CHECKIN_LINE, content.checkInLine)
            putExtra(NudgeFullScreenActivity.EXTRA_WINDOW, window.name)
            putExtra(NudgeFullScreenActivity.EXTRA_TONE, tone.name)
            putExtra(NudgeFullScreenActivity.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context, notificationId, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FALLBACK)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${content.body}\n\n${content.checkInLine}"))
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(activityPendingIntent)
            .setFullScreenIntent(activityPendingIntent, true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY_SERVICE = "overlay_service"
        const val CHANNEL_FALLBACK = "nudge_fallback"
        const val LISTENER_SERVICE_NOTIFICATION_ID = 1003
        private const val URGENT_NOTIFICATION_ID_BASE = 2000
    }
}
