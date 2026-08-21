package com.goalnudge.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Titik masuk tunggal untuk (re)start [NudgeListenerService]. Start di-best-effort: dipanggil
 * juga dari [com.goalnudge.app.GoalNudgeApp.onCreate], yang bisa terjadi di background (mis.
 * proses dibangunkan WorkManager). Android 12+ boleh menolak start foreground service dari
 * background dengan `ForegroundServiceStartNotAllowedException` — kalau itu terjadi, biarkan;
 * percobaan berikutnya (app dibuka / reboot) akan berhasil. Kalaupun listener ini tidak
 * berhasil jalan sama sekali, [com.goalnudge.app.scheduling.NudgeScheduleWorker] tetap
 * memberikan nudge lewat notifikasi full-screen — bukan lagi bergantung pada izin overlay.
 */
object NudgeListenerServiceController {

    fun ensureRunning(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, NudgeListenerService::class.java))
        }
    }
}
