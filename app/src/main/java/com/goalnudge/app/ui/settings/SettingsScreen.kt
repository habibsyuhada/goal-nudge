package com.goalnudge.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.goalnudge.app.domain.model.Tone
import com.goalnudge.app.platform.OemAutostartHelper
import com.goalnudge.app.platform.PermissionUtils
import com.goalnudge.app.service.NudgeListenerServiceController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                NudgeListenerServiceController.ensureRunning(context)
                permissionRefreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Tone nudge")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tone.entries.forEach { tone ->
                    FilterChip(
                        selected = settings.tone == tone,
                        onClick = { viewModel.setTone(tone) },
                        label = { Text(toneLabel(tone)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Rate limiting")
            var maxPerDay by remember(settings.maxNudgesPerDay) { mutableStateOf(settings.maxNudgesPerDay.toFloat()) }
            Text("Maksimal ${maxPerDay.toInt()} nudge/hari")
            Slider(
                value = maxPerDay,
                onValueChange = { maxPerDay = it },
                onValueChangeFinished = { viewModel.setMaxPerDay(maxPerDay.toInt()) },
                valueRange = 1f..8f,
                steps = 6
            )

            var cooldown by remember(settings.cooldownMinutes) { mutableStateOf(settings.cooldownMinutes.toFloat()) }
            Text("Cooldown antar-nudge: ${cooldown.toInt()} menit")
            Slider(
                value = cooldown,
                onValueChange = { cooldown = it },
                onValueChangeFinished = { viewModel.setCooldown(cooldown.toInt()) },
                valueRange = 15f..480f
            )

            Text("Quiet hours: ${settings.quietHoursStartHour}:00 – ${settings.quietHoursEndHour}:00")
            var quietStart by remember(settings.quietHoursStartHour) { mutableStateOf(settings.quietHoursStartHour.toFloat()) }
            var quietEnd by remember(settings.quietHoursEndHour) { mutableStateOf(settings.quietHoursEndHour.toFloat()) }
            Text("Mulai: ${quietStart.toInt()}:00", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = quietStart,
                onValueChange = { quietStart = it },
                onValueChangeFinished = { viewModel.setQuietHours(quietStart.toInt(), quietEnd.toInt()) },
                valueRange = 0f..23f
            )
            Text("Selesai: ${quietEnd.toInt()}:00", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = quietEnd,
                onValueChange = { quietEnd = it },
                onValueChangeFinished = { viewModel.setQuietHours(quietStart.toInt(), quietEnd.toInt()) },
                valueRange = 0f..23f
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Butuh jeda?")
            if (settings.isPaused) {
                Text("Nudge sedang dijeda sampai tanggal tersimpan.")
                OutlinedButton(onClick = { viewModel.clearPause() }) { Text("Lanjutkan nudge sekarang") }
            } else {
                Text("Kesal karena keseringan muncul? Jeda dulu, jangan uninstall.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.pauseThreeDays() }) { Text("Jeda 3 hari") }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Status izin")
            key(permissionRefreshKey) {
                PermissionRow(
                    label = "Tampil di atas app lain (overlay)",
                    granted = PermissionUtils.canDrawOverlays(context),
                    onFix = { context.startActivity(PermissionUtils.overlayPermissionIntent(context)) }
                )
                PermissionRow(
                    label = "Notifikasi",
                    granted = PermissionUtils.areNotificationsEnabled(context),
                    onFix = { context.startActivity(PermissionUtils.notificationSettingsIntent(context)) }
                )
                PermissionRow(
                    label = "Usage access (deteksi app navigasi aktif)",
                    granted = PermissionUtils.hasUsageAccess(context),
                    onFix = { context.startActivity(PermissionUtils.usageAccessIntent()) }
                )
                PermissionRow(
                    label = "Battery optimization diabaikan",
                    granted = PermissionUtils.isIgnoringBatteryOptimizations(context),
                    onFix = { context.startActivity(PermissionUtils.ignoreBatteryOptimizationsIntent(context)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { OemAutostartHelper.tryOpenAutostartSettings(context) }) {
                Text("Buka pengaturan autostart HP (Xiaomi/Oppo/Vivo)")
            }
            Text(
                "Beberapa HP mematikan service background secara agresif. Aktifkan autostart " +
                    "untuk Goal Nudge supaya nudge tetap muncul.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onFix: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label)
                Text(if (granted) "Aktif" else "Belum aktif", style = MaterialTheme.typography.bodySmall)
            }
            if (!granted) {
                OutlinedButton(onClick = onFix) { Text("Aktifkan") }
            }
        }
    }
}

private fun toneLabel(tone: Tone): String = when (tone) {
    Tone.LEMBUT -> "Lembut"
    Tone.NETRAL -> "Netral"
    Tone.KERAS -> "Keras"
}
