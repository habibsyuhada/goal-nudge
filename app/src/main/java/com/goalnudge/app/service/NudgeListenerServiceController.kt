package com.goalnudge.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.goalnudge.app.platform.PermissionUtils

/**
 * Titik masuk tunggal untuk (re)start [NudgeListenerService]. Tanpa izin overlay, service ini
 * tidak berguna (nudge akan selalu fallback ke notifikasi via WorkManager) jadi sengaja tidak
 * dijalankan — dipanggil ulang tiap kali izin overlay mungkin baru saja diberikan.
 *
 * Start di-best-effort: dipanggil juga dari [com.goalnudge.app.GoalNudgeApp.onCreate], yang
 * bisa terjadi di background (mis. proses dibangunkan WorkManager). Android 12+ boleh menolak
 * start foreground service dari background dengan [ForegroundServiceStartNotAllowedException] —
 * kalau itu terjadi, biarkan; percobaan berikutnya (app dibuka / reboot) akan berhasil.
 */
object NudgeListenerServiceController {

    fun ensureRunning(context: Context) {
        if (!PermissionUtils.canDrawOverlays(context)) return
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, NudgeListenerService::class.java))
        }
    }
}
