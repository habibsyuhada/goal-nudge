package com.goalnudge.app.scheduling

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.notification.NotificationHelper
import com.goalnudge.app.service.NudgeAttempt
import com.goalnudge.app.service.NudgeSelector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Jaring pengaman berkala: kalau user tidak unlock HP selama jendela nudge (atau overlay
 * gagal ditambahkan), kirim notifikasi biasa sebagai fallback — PLAN.md §6 Fase 3.
 * Sengaja TIDAK pernah menampilkan overlay dari sini — overlay hanya untuk momen unlock asli.
 */
@HiltWorker
class NudgeScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val nudgeSelector: NudgeSelector,
    private val notificationHelper: NotificationHelper,
    private val nudgeEventRepository: NudgeEventRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        when (val attempt = nudgeSelector.selectForUnlock()) {
            is NudgeAttempt.Show -> {
                notificationHelper.showFallbackNudge(attempt.content)
                nudgeEventRepository.record(
                    attempt.content.goalId,
                    attempt.window,
                    NudgeOutcome.FALLBACK_NOTIFICATION,
                    System.currentTimeMillis()
                )
            }
            is NudgeAttempt.Blocked, NudgeAttempt.NoActiveGoals -> Unit
        }
        return Result.success()
    }
}
