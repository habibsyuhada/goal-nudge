package com.goalnudge.app.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.goalnudge.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service ringan yang hanya menandai bahwa overlay sedang aktif di layar.
 * Overlay window itu sendiri dikelola langsung oleh [com.goalnudge.app.overlay.OverlayManager]
 * lewat WindowManager — service ini hanya menjaga proses tetap hidup selama overlay tampil,
 * lalu berhenti sendiri saat overlay ditutup (lihat OverlayManager.dismiss()).
 */
@AndroidEntryPoint
class UnlockOverlayService : LifecycleService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notificationHelper.buildServiceNotification())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
