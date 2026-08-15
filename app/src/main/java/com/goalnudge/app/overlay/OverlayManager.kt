package com.goalnudge.app.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.GoalRepository
import com.goalnudge.app.data.repository.NudgeEventRepository
import com.goalnudge.app.di.ApplicationScope
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.NudgeWindow
import com.goalnudge.app.domain.model.Tone
import com.goalnudge.app.notification.NotificationHelper
import com.goalnudge.app.service.UnlockOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menambahkan/melepas overlay window secara langsung lewat WindowManager. Overlay window
 * ditambahkan DULU baru foreground service di-start — urutan ini disengaja, lihat catatan
 * Android 15 di PLAN.md §3 (ForegroundServiceStartNotAllowedException kalau kebalik).
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nudgeEventRepository: NudgeEventRepository,
    private val goalRepository: GoalRepository,
    private val notificationHelper: NotificationHelper,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun isShowing(): Boolean = overlayView != null

    fun showOverlay(content: NudgeContent, window: NudgeWindow, tone: Tone) {
        if (isShowing()) return

        if (!hasOverlayPermission()) {
            fallbackToNotification(content, window)
            return
        }

        runCatching { addOverlayView(content, window, tone) }
            .onFailure { fallbackToNotification(content, window) }
    }

    private fun addOverlayView(content: NudgeContent, window: NudgeWindow, tone: Tone) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val owner = OverlayLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MaterialTheme {
                    Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                        NudgeOverlayCard(
                            content = content,
                            tone = tone,
                            autoDismissMillis = AUTO_DISMISS_MILLIS,
                            onCheckIn = {
                                applicationScope.launch { goalRepository.checkIn(content.goalId) }
                                recordAndDismiss(content, window, NudgeOutcome.TAPPED_CHECKIN)
                            },
                            onLater = { recordAndDismiss(content, window, NudgeOutcome.TAPPED_LATER) },
                            onSwipedAway = { recordAndDismiss(content, window, NudgeOutcome.SWIPED_AWAY) },
                            onTimeout = { recordAndDismiss(content, window, NudgeOutcome.TIMED_OUT) }
                        )
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        wm.addView(composeView, params)
        windowManager = wm
        overlayView = composeView
        lifecycleOwner = owner

        // Overlay sudah visible di layar — sekarang aman untuk start foreground service.
        ContextCompat.startForegroundService(context, Intent(context, UnlockOverlayService::class.java))

        applicationScope.launch {
            nudgeEventRepository.record(content.goalId, window, NudgeOutcome.SHOWN, System.currentTimeMillis())
        }
    }

    private fun fallbackToNotification(content: NudgeContent, window: NudgeWindow) {
        notificationHelper.showFallbackNudge(content)
        applicationScope.launch {
            nudgeEventRepository.record(content.goalId, window, NudgeOutcome.FALLBACK_NOTIFICATION, System.currentTimeMillis())
        }
    }

    private fun recordAndDismiss(content: NudgeContent, window: NudgeWindow, outcome: NudgeOutcome) {
        applicationScope.launch {
            nudgeEventRepository.record(content.goalId, window, outcome, System.currentTimeMillis())
        }
        dismiss()
    }

    fun dismiss() {
        val wm = windowManager
        val view = overlayView
        lifecycleOwner?.apply {
            handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        if (wm != null && view != null) {
            runCatching { wm.removeView(view) }
        }
        windowManager = null
        overlayView = null
        lifecycleOwner = null
        context.stopService(Intent(context, UnlockOverlayService::class.java))
    }

    companion object {
        private const val AUTO_DISMISS_MILLIS = 6000L
    }
}
