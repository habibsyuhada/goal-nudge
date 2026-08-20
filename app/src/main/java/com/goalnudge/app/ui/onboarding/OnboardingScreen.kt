package com.goalnudge.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.goalnudge.app.platform.OemAutostartHelper
import com.goalnudge.app.platform.OemBrand
import com.goalnudge.app.platform.PermissionUtils
import com.goalnudge.app.service.NudgeListenerServiceController

/**
 * Onboarding permission jujur (PLAN.md §6 Fase 3): jelaskan kenapa tiap izin dibutuhkan,
 * jangan sembunyikan overlay/battery/autostart di balik dialog sistem tanpa konteks.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }

    var overlayGranted by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }
    var notificationsGranted by remember { mutableStateOf(PermissionUtils.areNotificationsEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = PermissionUtils.canDrawOverlays(context)
                notificationsGranted = PermissionUtils.areNotificationsEnabled(context)
                batteryIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context)
                NudgeListenerServiceController.ensureRunning(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted || PermissionUtils.areNotificationsEnabled(context) }

    val brand = remember { OemAutostartHelper.detectBrand() }
    val steps = remember(brand) {
        buildList {
            add(OnboardingStep.Intro)
            add(OnboardingStep.Overlay)
            add(OnboardingStep.Notifications)
            add(OnboardingStep.Battery)
            if (brand != OemBrand.UNKNOWN) add(OnboardingStep.OemAutostart)
            add(OnboardingStep.Done)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (steps[step]) {
                OnboardingStep.Intro -> IntroStep()
                OnboardingStep.Overlay -> OverlayStep(
                    granted = overlayGranted,
                    onRequest = { context.startActivity(PermissionUtils.overlayPermissionIntent(context)) }
                )
                OnboardingStep.Notifications -> NotificationsStep(
                    granted = notificationsGranted,
                    onRequest = {
                        if (PermissionUtils.needsRuntimeNotificationPermission()) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(PermissionUtils.notificationSettingsIntent(context))
                        }
                    }
                )
                OnboardingStep.Battery -> BatteryStep(
                    ignored = batteryIgnored,
                    onRequest = { context.startActivity(PermissionUtils.ignoreBatteryOptimizationsIntent(context)) }
                )
                OnboardingStep.OemAutostart -> OemAutostartStep(
                    brand = brand,
                    onRequest = { OemAutostartHelper.tryOpenAutostartSettings(context) }
                )
                OnboardingStep.Done -> DoneStep()
            }

            Spacer(modifier = Modifier.height(32.dp))
            StepIndicator(step = step, totalSteps = steps.size)
            Spacer(modifier = Modifier.height(16.dp))

            if (steps[step] == OnboardingStep.Done) {
                Button(
                    onClick = { viewModel.completeOnboarding(onFinished) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mulai pakai Goal Nudge") }
            } else {
                Button(
                    onClick = { if (step < steps.lastIndex) step++ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Lanjut") }
                TextButton(onClick = { if (step < steps.lastIndex) step++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lewati")
                }
            }
        }
    }
}

private enum class OnboardingStep { Intro, Overlay, Notifications, Battery, OemAutostart, Done }

@Composable
private fun StepIndicator(step: Int, totalSteps: Int) {
    Text("Langkah ${step + 1} dari $totalSteps", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun IntroStep() {
    Text("Selamat datang di Goal Nudge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        "Kami akan menampilkan goal & alasanmu sendiri sesaat setelah kamu unlock HP — " +
            "2 sampai 4 kali sehari, bukan tiap kali. Untuk itu kami butuh beberapa izin. " +
            "Kami jelaskan masing-masing sebelum minta."
    )
}

@Composable
private fun OverlayStep(granted: Boolean, onRequest: () -> Unit) {
    Text("1. Tampil di atas app lain", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Izin ini WAJIB — tanpanya overlay nudge tidak bisa muncul sama sekali setelah unlock.")
    Spacer(modifier = Modifier.height(16.dp))
    PermissionStatusButton(granted = granted, onRequest = onRequest)
}

@Composable
private fun NotificationsStep(granted: Boolean, onRequest: () -> Unit) {
    Text("2. Notifikasi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Dipakai sebagai cadangan kalau overlay gagal muncul, dan untuk pengingat khusus deadline.")
    Spacer(modifier = Modifier.height(16.dp))
    PermissionStatusButton(granted = granted, onRequest = onRequest)
}

@Composable
private fun BatteryStep(ignored: Boolean, onRequest: () -> Unit) {
    Text("3. Pengecualian battery optimization", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Opsional, tapi direkomendasikan — mencegah Android mematikan service Goal Nudge di latar belakang.")
    Spacer(modifier = Modifier.height(16.dp))
    PermissionStatusButton(granted = ignored, onRequest = onRequest, activeLabel = "Sudah diizinkan")
}

@Composable
private fun OemAutostartStep(brand: OemBrand, onRequest: () -> Unit) {
    val isSamsung = brand == OemBrand.SAMSUNG
    Text(
        if (isSamsung) "4. Battery management HP Samsung" else "4. Autostart HP ${brand.name}",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        if (isSamsung) {
            "One UI punya \"Sleeping apps\" / battery management sendiri di luar Android biasa. " +
                "Kalau Goal Nudge tidak dikecualikan (jangan taruh di sleeping apps), Samsung bisa " +
                "mematikannya diam-diam setelah beberapa hari dan nudge berhenti muncul."
        } else {
            "HP kamu punya pengelola autostart sendiri di luar Android biasa. Kalau tidak diaktifkan, " +
                "sistem HP bisa mematikan Goal Nudge diam-diam dan nudge berhenti muncul."
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
        Text(if (isSamsung) "Buka pengaturan baterai" else "Buka pengaturan autostart")
    }
}

@Composable
private fun DoneStep() {
    Text("Siap!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Buat goal pertamamu, tulis \"why\"-nya, dan biarkan Goal Nudge mengingatkanmu di saat yang tepat.")
}

@Composable
private fun PermissionStatusButton(granted: Boolean, onRequest: () -> Unit, activeLabel: String = "Sudah aktif") {
    if (granted) {
        Text("✓ $activeLabel", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    } else {
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Aktifkan izin") }
    }
}
