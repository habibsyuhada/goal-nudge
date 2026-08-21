package com.goalnudge.app.scheduling

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.notification.NotificationHelper
import com.goalnudge.app.service.NudgeAttempt
import com.goalnudge.app.service.NudgeSelector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Jaring pengaman berkala: kalau [com.goalnudge.app.service.NudgeListenerService] tidak sempat
 * menangkap unlock selama jendela nudge (mis. dibunuh OS/OEM di background), worker ini
 * memicu nudge yang sama lewat [NotificationHelper.showUrgentNudge] — PLAN.md §6 Fase 3.
 */
@HiltWorker
class NudgeScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val nudgeSelector: NudgeSelector,
    private val notificationHelper: NotificationHelper,
    private val nudgeEventRepository: NudgeEventRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
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
        return Result.success()
    }
}
