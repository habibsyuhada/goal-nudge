package com.goalnudge.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.goalnudge.app.data.local.entity.NudgeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NudgeEventDao {

    @Insert
    suspend fun insert(event: NudgeEventEntity): Long

    @Query("SELECT COUNT(*) FROM nudge_events WHERE outcome = 'SHOWN' AND timestampMillis >= :sinceMillis")
    suspend fun countShownSince(sinceMillis: Long): Int

    @Query("SELECT * FROM nudge_events WHERE outcome = 'SHOWN' ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun lastShown(): NudgeEventEntity?

    @Query("SELECT * FROM nudge_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<NudgeEventEntity>>

    @Query(
        "SELECT COUNT(*) FROM nudge_events WHERE window = :window AND timestampMillis >= :sinceMillis " +
            "AND outcome IN ('SHOWN','TAPPED_CHECKIN','TAPPED_LATER','SWIPED_AWAY','TIMED_OUT','FALLBACK_NOTIFICATION')"
    )
    suspend fun countForWindowSince(window: String, sinceMillis: Long): Int
}
