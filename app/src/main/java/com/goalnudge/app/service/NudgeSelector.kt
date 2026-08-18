package com.goalnudge.app.service

import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.GoalRepository
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.domain.guard.ContextDecision
import com.goalnudge.app.domain.guard.ContextGuard
import com.goalnudge.app.domain.guard.RateLimitDecision
import com.goalnudge.app.domain.guard.RateLimiter
import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.NudgeWindow
import com.goalnudge.app.domain.template.NudgeTemplateEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed class NudgeAttempt {
    data class Show(val content: NudgeContent, val window: NudgeWindow) : NudgeAttempt()
    data class Blocked(val reason: String) : NudgeAttempt()
    data object NoActiveGoals : NudgeAttempt()
}

/**
 * Menentukan apakah & goal mana yang tampil saat unlock — menyatukan rate limiting,
 * context guard, dan jadwal jendela (pagi/siang/malam) dari PLAN.md §4 & §6.
 */
@Singleton
class NudgeSelector @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsDataStore: SettingsDataStore,
    private val nudgeEventRepository: NudgeEventRepository,
    private val rateLimiter: RateLimiter,
    private val contextGuard: ContextGuard,
    private val templateEngine: NudgeTemplateEngine
) {
    suspend fun selectForUnlock(nowMillis: Long = System.currentTimeMillis()): NudgeAttempt {
        val goals = goalRepository.getActiveGoalsOnce()
        if (goals.isEmpty()) return NudgeAttempt.NoActiveGoals

        val hour = java.time.Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
        val window = NudgeWindow.current(hour)
            ?: return NudgeAttempt.Blocked("di luar jendela nudge (pagi/siang/malam)")

        val startOfDay = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (nudgeEventRepository.wasWindowAlreadyUsed(window, startOfDay)) {
            return NudgeAttempt.Blocked("jendela $window sudah dipakai hari ini")
        }

        val settings = settingsDataStore.settings.first()

        when (val rate = rateLimiter.evaluate(settings, nowMillis)) {
            is RateLimitDecision.Blocked -> {
                nudgeEventRepository.record(null, window, NudgeOutcome.SKIPPED_RATE_LIMIT, nowMillis)
                return NudgeAttempt.Blocked(rate.reason)
            }
            RateLimitDecision.Allowed -> Unit
        }

        when (val guard = contextGuard.evaluate()) {
            is ContextDecision.Guarded -> {
                nudgeEventRepository.record(null, window, NudgeOutcome.SKIPPED_GUARD, nowMillis)
                return NudgeAttempt.Blocked(guard.reason)
            }
            ContextDecision.Clear -> Unit
        }

        val mostNeglectedGoal = goals
            .sortedWith(
                compareByDescending<Goal> { it.daysSinceCheckIn ?: Long.MAX_VALUE }
                    .thenBy { it.daysUntilTarget }
            )
            .first()

        val content = templateEngine.render(mostNeglectedGoal, settings.tone)
        return NudgeAttempt.Show(content, window)
    }
}
