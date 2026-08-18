package com.goalnudge.app.data.repository

import com.goalnudge.app.data.local.dao.CheckInDao
import com.goalnudge.app.data.local.dao.GoalDao
import com.goalnudge.app.data.local.entity.CheckInEntity
import com.goalnudge.app.data.local.entity.toDomain
import com.goalnudge.app.data.local.entity.toEntity
import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.GoalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class SaveGoalResult {
    data class Success(val goalId: Long) : SaveGoalResult()
    data object TooManyActiveGoals : SaveGoalResult()
}

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao
) {
    fun observeActiveGoals(): Flow<List<Goal>> =
        goalDao.observeActiveGoals().map { list -> list.map { it.toDomain() } }

    fun observeGoal(id: Long): Flow<Goal?> = goalDao.observeById(id).map { it?.toDomain() }

    suspend fun getGoalOnce(id: Long): Goal? = goalDao.getById(id)?.toDomain()

    suspend fun getActiveGoalsOnce(): List<Goal> = goalDao.getActiveGoalsOnce().map { it.toDomain() }

    /** Wajib: max 3 goal aktif (PLAN.md §4). Goal baru (id == 0) dicek terhadap batas ini. */
    suspend fun saveGoal(goal: Goal): SaveGoalResult {
        if (goal.id == 0L) {
            val activeCount = goalDao.countActive()
            if (activeCount >= Goal.MAX_ACTIVE_GOALS) {
                return SaveGoalResult.TooManyActiveGoals
            }
        }
        val id = goalDao.upsert(goal.toEntity())
        return SaveGoalResult.Success(if (goal.id != 0L) goal.id else id)
    }

    suspend fun archiveGoal(goal: Goal) {
        goalDao.update(goal.toEntity().copy(isArchived = true))
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.delete(goal.toEntity())
    }

    /** Check-in satu tombol: update streak/progress dan catat riwayat. Idempoten per hari. */
    suspend fun checkIn(goalId: Long, note: String? = null): Goal? {
        val entity = goalDao.getById(goalId) ?: return null
        val today = LocalDate.now()
        val alreadyToday = checkInDao.countOnDay(goalId, today.toEpochDay()) > 0
        if (alreadyToday) return entity.toDomain()

        checkInDao.insert(CheckInEntity(goalId = goalId, checkInEpochDay = today.toEpochDay(), note = note))

        val yesterday = today.minusDays(1)
        val wasStreakAlive = entity.lastCheckInEpochDay == yesterday.toEpochDay()
        val newStreak = if (wasStreakAlive) entity.currentStreak + 1 else 1
        val newLongest = maxOf(entity.longestStreak, newStreak)
        val newProgress = when (entity.type) {
            GoalType.PERSEN -> (entity.progressPercent + 5).coerceAtMost(100)
            else -> entity.progressPercent
        }
        val newMilestoneDone = when (entity.type) {
            GoalType.MILESTONE -> (entity.milestoneDone + 1).coerceAtMost(entity.milestoneTotal)
            else -> entity.milestoneDone
        }

        val updated = entity.copy(
            currentStreak = newStreak,
            longestStreak = newLongest,
            progressPercent = newProgress,
            milestoneDone = newMilestoneDone,
            lastCheckInEpochDay = today.toEpochDay()
        )
        goalDao.update(updated)
        return updated.toDomain()
    }
}
