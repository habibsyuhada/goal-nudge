package com.goalnudge.app.data.local

import androidx.room.TypeConverter
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.domain.model.GoalType

class Converters {
    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name

    @TypeConverter
    fun toGoalType(value: String): GoalType = GoalType.valueOf(value)

    @TypeConverter
    fun fromNudgeOutcome(value: NudgeOutcome): String = value.name

    @TypeConverter
    fun toNudgeOutcome(value: String): NudgeOutcome = NudgeOutcome.valueOf(value)
}
