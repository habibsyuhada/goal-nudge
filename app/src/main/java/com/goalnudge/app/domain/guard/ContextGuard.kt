package com.goalnudge.app.domain.guard

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class ContextDecision {
    data object Clear : ContextDecision()
    data class Guarded(val reason: String) : ContextDecision()
}

/**
 * "Deteksi konteks (jangan muncul saat call/camera/maps)" — PLAN.md §6 Fase 3.
 * Best-effort dan fail-open: kalau sinyal tidak tersedia (mis. Usage Access belum
 * diizinkan), guard tidak memblokir nudge sama sekali.
 */
@Singleton
class ContextGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraUsageObserver: CameraUsageObserver
) {
    private val navigationLikePackages = setOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.here.app.maps",
        "org.osmdroid",
        "com.sygic.aura"
    )

    fun evaluate(): ContextDecision {
        if (isPhoneCallActive()) return ContextDecision.Guarded("panggilan telepon aktif")
        if (cameraUsageObserver.isCameraInUse.value) return ContextDecision.Guarded("kamera sedang dipakai")
        foregroundGuardedPackage()?.let { return ContextDecision.Guarded("app navigasi aktif: $it") }
        return ContextDecision.Clear
    }

    private fun isPhoneCallActive(): Boolean = runCatching {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        @Suppress("DEPRECATION")
        telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
    }.getOrDefault(false)

    private fun foregroundGuardedPackage(): String? {
        if (!hasUsageAccess()) return null
        return runCatching {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - USAGE_LOOKBACK_MS
            val events = usageStatsManager.queryEvents(start, end)
            var lastForegroundPackage: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundPackage = event.packageName
                }
            }
            lastForegroundPackage?.takeIf { it in navigationLikePackages }
        }.getOrNull()
    }

    fun hasUsageAccess(): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    companion object {
        private const val USAGE_LOOKBACK_MS = 10_000L
    }
}
