package com.goalnudge.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.di.ApplicationScope
import com.goalnudge.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service persisten yang menjadi trigger utama nudge (PLAN.md §1/§3): sejak
 * Android 8.0, broadcast implisit `ACTION_SCREEN_ON` TIDAK dikirim ke receiver yang
 * didaftarkan lewat AndroidManifest — hanya ke receiver yang didaftarkan runtime lewat
 * [Context.registerReceiver]. Service ini menjaga receiver itu tetap terdaftar selama app
 * hidup di background. Diminta jalan dari [com.goalnudge.app.GoalNudgeApp.onCreate] dan
 * [BootCompletedReceiver].
 *
 * Trigger-nya `ACTION_SCREEN_ON`, BUKAN `ACTION_USER_PRESENT` (setelah unlock) — sengaja.
 * `NotificationCompat.Builder.setFullScreenIntent` di [NotificationHelper.showUrgentNudge]
 * hanya auto-membuka activity kalau notifikasi di-post SAAT layar mati/masih terkunci
 * (pembatasan resmi Android, sama seperti pola app telepon/alarm); begitu `ACTION_USER_PRESENT`
 * terpicu, user sudah selesai unlock dan Android cuma menampilkannya sebagai notifikasi biasa
 * (persis gejala yang dilaporkan: "cuma muncul sebagai push notification"). Dengan
 * `ACTION_SCREEN_ON`, nudge diposting begitu layar menyala — kalau HP masih terkunci saat itu,
 * kartunya auto-muncul di atas lock screen; kalau tidak, tetap jadi notifikasi yang bisa ditap.
 * Kalau service ini sendiri dibunuh OS/OEM sebelum layar menyala berikutnya,
 * [com.goalnudge.app.scheduling.NudgeScheduleWorker] jadi jaring pengaman.
 */
@AndroidEntryPoint
class NudgeListenerService : Service() {

    @Inject
    lateinit var nudgeSelector: NudgeSelector

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var nudgeEventRepository: NudgeEventRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private var screenOnReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_SCREEN_ON) return
                applicationScope.launch { handleScreenOn() }
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenOnReceiver = receiver
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.LISTENER_SERVICE_NOTIFICATION_ID,
            notificationHelper.buildListenerServiceNotification()
        )
        return START_STICKY
    }

    override fun onDestroy() {
        screenOnReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenOnReceiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private suspend fun handleScreenOn() {
        when (val attempt = nudgeSelector.selectForUnlock()) {
            is NudgeAttempt.Show -> {
                val tone = settingsDataStore.settings.first().tone
                notificationHelper.showUrgentNudge(attempt.content, attempt.window, tone)
                nudgeEventRepository.record(
                    attempt.content.goalId,
                    attempt.window,
                    NudgeOutcome.FALLBACK_NOTIFICATION,
                    System.currentTimeMillis()
                )
            }
            is NudgeAttempt.Blocked, NudgeAttempt.NoActiveGoals -> Unit
        }
    }
}
