package com.goalnudge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.GoalType
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val why: String,
    val targetDateEpochDay: Long,
    val type: GoalType,
    val photoUri: String?,
    val createdAtEpochDay: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val progressPercent: Int,
    val milestoneTotal: Int,
    val milestoneDone: Int,
    val lastCheckInEpochDay: Long?,
    val isArchived: Boolean
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    title = title,
    why = why,
    targetDate = LocalDate.ofEpochDay(targetDateEpochDay),
    type = type,
    photoUri = photoUri,
    createdAt = LocalDate.ofEpochDay(createdAtEpochDay),
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    progressPercent = progressPercent,
    milestoneTotal = milestoneTotal,
    milestoneDone = milestoneDone,
    lastCheckInAt = lastCheckInEpochDay?.let(LocalDate::ofEpochDay),
    isArchived = isArchived
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    why = why,
    targetDateEpochDay = targetDate.toEpochDay(),
    type = type,
    photoUri = photoUri,
    createdAtEpochDay = createdAt.toEpochDay(),
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    progressPercent = progressPercent,
    milestoneTotal = milestoneTotal,
    milestoneDone = milestoneDone,
    lastCheckInEpochDay = lastCheckInAt?.toEpochDay(),
    isArchived = isArchived
)
