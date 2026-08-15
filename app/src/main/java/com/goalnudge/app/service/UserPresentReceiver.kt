package com.goalnudge.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.di.ApplicationScope
import com.goalnudge.app.overlay.OverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Trigger utama nudge: momen setelah user unlock HP (PLAN.md §1). BUKAN setiap unlock —
 * seleksi & rate limiting terjadi di [NudgeSelector] sebelum overlay ditampilkan.
 */
@AndroidEntryPoint
class UserPresentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var nudgeSelector: NudgeSelector

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                handleUnlock()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleUnlock() {
        when (val attempt = nudgeSelector.selectForUnlock()) {
            is NudgeAttempt.Show -> {
                val tone = settingsDataStore.settings.first().tone
                withContext(Dispatchers.Main) {
                    overlayManager.showOverlay(attempt.content, attempt.window, tone)
                }
            }
            is NudgeAttempt.Blocked, NudgeAttempt.NoActiveGoals -> Unit
        }
    }
}
