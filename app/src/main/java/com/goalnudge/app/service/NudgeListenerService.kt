package com.goalnudge.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.di.ApplicationScope
import com.goalnudge.app.notification.NotificationHelper
import com.goalnudge.app.overlay.OverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Foreground service persisten yang menjadi trigger utama nudge (PLAN.md §1/§3): sejak
 * Android 8.0, broadcast implisit `ACTION_USER_PRESENT` TIDAK dikirim ke receiver yang
 * didaftarkan lewat AndroidManifest — hanya ke receiver yang didaftarkan runtime lewat
 * [Context.registerReceiver]. Service ini menjaga receiver itu tetap terdaftar selama app
 * hidup di background. Diminta jalan dari [com.goalnudge.app.GoalNudgeApp.onCreate] dan
 * [BootCompletedReceiver] — hanya kalau izin overlay sudah diberikan.
 */
@AndroidEntryPoint
class NudgeListenerService : Service() {

    @Inject
    lateinit var nudgeSelector: NudgeSelector

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private var userPresentReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_USER_PRESENT) return
                applicationScope.launch { handleUnlock() }
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        userPresentReceiver = receiver
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.LISTENER_SERVICE_NOTIFICATION_ID,
            notificationHelper.buildListenerServiceNotification()
        )
        return START_STICKY
    }

    override fun onDestroy() {
        userPresentReceiver?.let { runCatching { unregisterReceiver(it) } }
        userPresentReceiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private suspend fun handleUnlock() {
        when (val attempt = nudgeSelector.selectForUnlock()) {
            is NudgeAttempt.Show -> {
                val tone = settingsDataStore.settings.first().tone
                withContext(Dispatchers.Main) {
                    overlayManager.showOverlay(attempt.content, attempt.window, tone)
                }
            }
            is NudgeAttempt.Blocked, NudgeAttempt.NoActiveGoals -> Unit
        }
    }
}
