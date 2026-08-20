package com.goalnudge.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.goalnudge.app.notification.NotificationHelper
import com.goalnudge.app.scheduling.NudgeSchedulingSetup
import com.goalnudge.app.service.NudgeListenerServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GoalNudgeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var nudgeSchedulingSetup: NudgeSchedulingSetup

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        nudgeSchedulingSetup.ensureScheduled()
        NudgeListenerServiceController.ensureRunning(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_OVERLAY_SERVICE,
                getString(R.string.notification_channel_overlay_service),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_overlay_service_desc)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_FALLBACK,
                getString(R.string.notification_channel_fallback),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_fallback_desc)
            }
        )
    }
}
