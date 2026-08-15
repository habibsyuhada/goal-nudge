package com.goalnudge.app.ui.metrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

/**
 * Fase 4: "Satu metrik utama: % nudge di-tap vs di-swipe (ini penentu hidup-mati produk)"
 * — PLAN.md §6. Semua dihitung lokal dari nudge_events, tanpa backend/analytics pihak ketiga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(onBack: () -> Unit, viewModel: MetricsViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metrik") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Tap vs Swipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Metrik penentu: apakah nudge ini bikin orang bertindak, atau cuma ganggu?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            MetricCard("Ditap (kerjakan / nanti)", summary.tapRate)
            MetricCard("Di-swipe", summary.swipeRate)

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rincian mentah", fontWeight = FontWeight.Bold)
                    Text("Ditampilkan (overlay): ${summary.totalShown}")
                    Text("Tap 'Kerjakan sekarang': ${summary.tappedCheckIn}")
                    Text("Tap 'Nanti': ${summary.tappedLater}")
                    Text("Di-swipe: ${summary.swipedAway}")
                    Text("Timeout tanpa interaksi: ${summary.timedOut}")
                    Text("Fallback notifikasi: ${summary.fallbackNotifications}")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, rate: Float) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("${(rate * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { rate }, modifier = Modifier.fillMaxWidth())
        }
    }
}
