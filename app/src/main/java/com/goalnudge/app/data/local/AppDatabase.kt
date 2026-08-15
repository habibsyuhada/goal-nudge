package com.goalnudge.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.goalnudge.app.data.local.dao.CheckInDao
import com.goalnudge.app.data.local.dao.GoalDao
import com.goalnudge.app.data.local.dao.NudgeEventDao
import com.goalnudge.app.data.local.entity.CheckInEntity
import com.goalnudge.app.data.local.entity.GoalEntity
import com.goalnudge.app.data.local.entity.NudgeEventEntity

@Database(
    entities = [GoalEntity::class, CheckInEntity::class, NudgeEventEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun checkInDao(): CheckInDao
    abstract fun nudgeEventDao(): NudgeEventDao

    companion object {
        const val NAME = "goal_nudge.db"
    }
}
