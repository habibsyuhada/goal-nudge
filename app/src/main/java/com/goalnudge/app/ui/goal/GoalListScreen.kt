package com.goalnudge.app.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.GoalType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMetrics: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val goals by viewModel.activeGoals.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goal Nudge") },
                actions = {
                    IconButton(onClick = onOpenMetrics) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Metrik")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        },
        floatingActionButton = {
            if (goals.size < Goal.MAX_ACTIVE_GOALS) {
                ExtendedFloatingActionButton(onClick = onAddGoal, text = { Text("Goal baru") }, icon = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                })
            }
        }
    ) { padding ->
        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum ada goal aktif.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Max 3 goal — fokus itu penting.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddGoal) { Text("Buat goal pertama") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onCheckIn = { viewModel.checkIn(goal.id) },
                        onEdit = { onEditGoal(goal.id) },
                        onArchive = { viewModel.archive(goal) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, onCheckIn: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("\"${goal.why}\"", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            when (goal.type) {
                GoalType.PERSEN -> {
                    LinearProgressIndicator(progress = { goal.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${goal.progressPercent}%")
                }
                GoalType.MILESTONE -> {
                    val fraction = if (goal.milestoneTotal > 0) goal.milestoneDone / goal.milestoneTotal.toFloat() else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${goal.milestoneDone}/${goal.milestoneTotal} milestone")
                }
                GoalType.STREAK -> {
                    Text("Streak: ${goal.currentStreak} hari (terpanjang: ${goal.longestStreak})")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sisa ${goal.daysUntilTarget.coerceAtLeast(0)} hari · " +
                    (goal.daysSinceCheckIn?.let { "check-in terakhir $it hari lalu" } ?: "belum pernah check-in"),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCheckIn) { Text("Check-in") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onArchive) { Text("Arsipkan") }
            }
        }
    }
}
