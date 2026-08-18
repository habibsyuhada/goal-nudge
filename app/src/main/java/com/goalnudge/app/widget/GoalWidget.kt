package com.goalnudge.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.goalnudge.app.domain.model.Goal
import dagger.hilt.android.EntryPointAccessors

/**
 * Widget home screen — lapis kedua kehadiran (PLAN.md §4): pasif, selalu terlihat.
 * Goal aktif + sisa hari + streak, tanpa perlu buka app.
 */
class GoalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).goalRepository()
        val goals = repository.getActiveGoalsOnce()
        val mostUrgent = goals.minByOrNull { it.daysUntilTarget }

        provideContent {
            WidgetContent(goal = mostUrgent)
        }
    }
}

@Composable
private fun WidgetContent(goal: Goal?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1B1F3B)))
            .padding(12.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (goal == null) {
            Text(
                text = "Belum ada goal aktif",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
            )
        } else {
            Text(
                text = goal.title,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Sisa ${goal.daysUntilTarget.coerceAtLeast(0)} hari",
                style = TextStyle(color = ColorProvider(Color(0xFFB0BEC5)), fontSize = 13.sp)
            )
            Text(
                text = "Streak: ${goal.currentStreak} hari",
                style = TextStyle(color = ColorProvider(Color(0xFFB0BEC5)), fontSize = 13.sp)
            )
        }
    }
}
