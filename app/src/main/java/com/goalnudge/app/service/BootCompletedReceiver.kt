package com.goalnudge.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goalnudge.app.scheduling.NudgeSchedulingSetup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Memastikan jadwal WorkManager (fallback notif + jendela nudge) tetap hidup setelah
 * reboot — PLAN.md §6 Fase 1 "uji: service survive setelah reboot".
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var nudgeSchedulingSetup: NudgeSchedulingSetup

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        nudgeSchedulingSetup.ensureScheduled()
        NudgeListenerServiceController.ensureRunning(context)
    }
}
