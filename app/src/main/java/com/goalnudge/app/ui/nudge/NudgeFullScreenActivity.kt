package com.goalnudge.app.ui.nudge

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.GoalRepository
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.di.ApplicationScope
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.NudgeWindow
import com.goalnudge.app.domain.model.Tone
import com.goalnudge.app.overlay.NudgeOverlayCard
import com.goalnudge.app.ui.theme.GoalNudgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Kartu nudge penuh layar, dibuka lewat [android.app.Notification.Builder.setFullScreenIntent]
 * dari [com.goalnudge.app.notification.NotificationHelper.showUrgentNudge] — jalur pengganti
 * overlay `TYPE_APPLICATION_OVERLAY` lama, yang di banyak HP (terutama OEM Cina) gagal muncul
 * karena background service-nya dibunuh sebelum sempat menambahkan window. Full-screen intent
 * notification tidak butuh [android.provider.Settings.canDrawOverlays] dan tetap membuka
 * activity ini walau OS mendemosikannya jadi notifikasi biasa (user tinggal tap).
 */
@AndroidEntryPoint
class NudgeFullScreenActivity : ComponentActivity() {

    @Inject
    lateinit var goalRepository: GoalRepository

    @Inject
    lateinit var nudgeEventRepository: NudgeEventRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }

        val content = NudgeContent(
            goalId = intent.getLongExtra(EXTRA_GOAL_ID, 0L),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            body = intent.getStringExtra(EXTRA_BODY).orEmpty(),
            why = intent.getStringExtra(EXTRA_WHY).orEmpty(),
            checkInLine = intent.getStringExtra(EXTRA_CHECKIN_LINE).orEmpty()
        )
        val window = intent.getStringExtra(EXTRA_WINDOW)
            ?.let { runCatching { NudgeWindow.valueOf(it) }.getOrNull() }
        val tone = intent.getStringExtra(EXTRA_TONE)
            ?.let { runCatching { Tone.valueOf(it) }.getOrNull() } ?: Tone.NETRAL

        applicationScope.launch {
            nudgeEventRepository.record(content.goalId, window, NudgeOutcome.SHOWN, System.currentTimeMillis())
        }

        setContent {
            GoalNudgeTheme {
                NudgeOverlayCard(
                    content = content,
                    tone = tone,
                    autoDismissMillis = AUTO_DISMISS_MILLIS,
                    onCheckIn = {
                        applicationScope.launch { goalRepository.checkIn(content.goalId) }
                        recordAndFinish(content, window, NudgeOutcome.TAPPED_CHECKIN)
                    },
                    onLater = { recordAndFinish(content, window, NudgeOutcome.TAPPED_LATER) },
                    onSwipedAway = { recordAndFinish(content, window, NudgeOutcome.SWIPED_AWAY) },
                    onTimeout = { recordAndFinish(content, window, NudgeOutcome.TIMED_OUT) }
                )
            }
        }
    }

    private fun recordAndFinish(content: NudgeContent, window: NudgeWindow?, outcome: NudgeOutcome) {
        applicationScope.launch {
            nudgeEventRepository.record(content.goalId, window, outcome, System.currentTimeMillis())
        }
        finish()
    }

    companion object {
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_WHY = "why"
        const val EXTRA_CHECKIN_LINE = "checkin_line"
        const val EXTRA_WINDOW = "window"
        const val EXTRA_TONE = "tone"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val AUTO_DISMISS_MILLIS = 6000L
    }
}
