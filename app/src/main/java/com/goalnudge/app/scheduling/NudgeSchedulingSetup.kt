package com.goalnudge.app.scheduling

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifikasi terjadwal sebagai lapis ke-3 (PLAN.md §4): jaring pengaman kalau user tidak
 * unlock HP selama jendela pagi/siang/malam, atau overlay gagal muncul.
 */
@Singleton
class NudgeSchedulingSetup @Inject constructor(@ApplicationContext private val context: Context) {

    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<NudgeScheduleWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "nudge_schedule_check"
    }
}
