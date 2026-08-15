package com.goalnudge.app.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalnudge.app.data.local.entity.NudgeOutcome
import com.goalnudge.app.data.repository.NudgeEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MetricsSummary(
    val totalShown: Int = 0,
    val tappedCheckIn: Int = 0,
    val tappedLater: Int = 0,
    val swipedAway: Int = 0,
    val timedOut: Int = 0,
    val fallbackNotifications: Int = 0
) {
    /** Satu metrik utama produk (PLAN.md §6 Fase 4): % nudge di-tap vs di-swipe. */
    private val totalResolved: Int
        get() = tappedCheckIn + tappedLater + swipedAway + timedOut

    val tapRate: Float
        get() = if (totalResolved == 0) 0f else (tappedCheckIn + tappedLater) / totalResolved.toFloat()

    val swipeRate: Float
        get() = if (totalResolved == 0) 0f else swipedAway / totalResolved.toFloat()
}

@HiltViewModel
class MetricsViewModel @Inject constructor(
    nudgeEventRepository: NudgeEventRepository
) : ViewModel() {

    val summary: StateFlow<MetricsSummary> = nudgeEventRepository.observeAll()
        .map { events ->
            MetricsSummary(
                totalShown = events.count { it.outcome == NudgeOutcome.SHOWN },
                tappedCheckIn = events.count { it.outcome == NudgeOutcome.TAPPED_CHECKIN },
                tappedLater = events.count { it.outcome == NudgeOutcome.TAPPED_LATER },
                swipedAway = events.count { it.outcome == NudgeOutcome.SWIPED_AWAY },
                timedOut = events.count { it.outcome == NudgeOutcome.TIMED_OUT },
                fallbackNotifications = events.count { it.outcome == NudgeOutcome.FALLBACK_NOTIFICATION }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetricsSummary())
}
