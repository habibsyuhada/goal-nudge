package com.goalnudge.app.domain.guard

import com.goalnudge.app.data.datastore.UserSettings
import com.goalnudge.app.data.repository.NudgeEventRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed class RateLimitDecision {
    data object Allowed : RateLimitDecision()
    data class Blocked(val reason: String) : RateLimitDecision()
}

/**
 * Rate limiting sejak MVP — PLAN.md §4: max N/hari, cooldown antar-nudge, quiet hours.
 * Kelangkaan adalah fitur, bukan bug: muncul tiap unlock = uninstall.
 */
@Singleton
class RateLimiter @Inject constructor(private val nudgeEventRepository: NudgeEventRepository) {

    suspend fun evaluate(settings: UserSettings, nowMillis: Long): RateLimitDecision {
        if (settings.isPaused) return RateLimitDecision.Blocked("user sedang jeda")

        if (isQuietHour(settings, nowMillis)) return RateLimitDecision.Blocked("quiet hours")

        val startOfDay = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val shownToday = nudgeEventRepository.countShownSince(startOfDay)
        if (shownToday >= settings.maxNudgesPerDay) {
            return RateLimitDecision.Blocked("sudah mencapai max ${settings.maxNudgesPerDay}/hari")
        }

        val lastShown = nudgeEventRepository.lastShownAtMillis()
        if (lastShown != null) {
            val minutesSinceLast = (nowMillis - lastShown) / 60_000
            if (minutesSinceLast < settings.cooldownMinutes) {
                return RateLimitDecision.Blocked("cooldown, baru $minutesSinceLast menit lalu")
            }
        }

        return RateLimitDecision.Allowed
    }

    private fun isQuietHour(settings: UserSettings, nowMillis: Long): Boolean {
        val hour = java.time.Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
        val start = settings.quietHoursStartHour
        val end = settings.quietHoursEndHour
        return if (start == end) {
            false
        } else if (start < end) {
            hour in start until end
        } else {
            // rentang melewati tengah malam, mis. 23 -> 6
            hour >= start || hour < end
        }
    }
}
