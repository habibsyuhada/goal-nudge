package com.goalnudge.app.di

import android.content.Context
import androidx.room.Room
import com.goalnudge.app.data.local.AppDatabase
import com.goalnudge.app.data.local.dao.CheckInDao
import com.goalnudge.app.data.local.dao.GoalDao
import com.goalnudge.app.data.local.dao.NudgeEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideCheckInDao(db: AppDatabase): CheckInDao = db.checkInDao()

    @Provides
    fun provideNudgeEventDao(db: AppDatabase): NudgeEventDao = db.nudgeEventDao()
}
