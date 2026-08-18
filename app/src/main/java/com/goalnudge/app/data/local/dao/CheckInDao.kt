package com.goalnudge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.goalnudge.app.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    @Insert
    suspend fun insert(checkIn: CheckInEntity): Long

    @Query("SELECT * FROM check_ins WHERE goalId = :goalId ORDER BY checkInEpochDay DESC")
    fun observeForGoal(goalId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT COUNT(*) FROM check_ins WHERE goalId = :goalId AND checkInEpochDay = :epochDay")
    suspend fun countOnDay(goalId: Long, epochDay: Long): Int
}
