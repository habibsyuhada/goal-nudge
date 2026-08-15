package com.goalnudge.app.widget

import com.goalnudge.app.data.repository.GoalRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidget bukan komponen Android biasa, jadi tidak bisa di-@AndroidEntryPoint —
 * ambil dependency lewat Hilt EntryPoint ini.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun goalRepository(): GoalRepository
}
