package com.goalnudge.app.domain.model

import java.time.LocalDate

data class Goal(
    val id: Long = 0L,
    val title: String,
    val why: String,
    val targetDate: LocalDate,
    val type: GoalType,
    val photoUri: String? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val progressPercent: Int = 0,
    val milestoneTotal: Int = 0,
    val milestoneDone: Int = 0,
    val lastCheckInAt: LocalDate? = null,
    val isArchived: Boolean = false
) {
    val daysUntilTarget: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate)

    val daysSinceCreated: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDate.now())

    val daysSinceCheckIn: Long?
        get() = lastCheckInAt?.let { java.time.temporal.ChronoUnit.DAYS.between(it, LocalDate.now()) }

    companion object {
        const val MAX_ACTIVE_GOALS = 3
    }
}
