package com.goalnudge.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NudgeOutcome {
    SHOWN,
    TAPPED_CHECKIN,
    TAPPED_LATER,
    SWIPED_AWAY,
    TIMED_OUT,
    SKIPPED_GUARD,
    SKIPPED_RATE_LIMIT,
    FALLBACK_NOTIFICATION
}

@Entity(tableName = "nudge_events")
data class NudgeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long?,
    val timestampMillis: Long,
    val window: String?,
    val outcome: NudgeOutcome
)
