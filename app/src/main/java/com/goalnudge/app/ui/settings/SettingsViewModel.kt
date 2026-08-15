package com.goalnudge.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goalnudge.app.data.datastore.SettingsDataStore
import com.goalnudge.app.data.datastore.UserSettings
import com.goalnudge.app.domain.model.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setTone(tone: Tone) = viewModelScope.launch { settingsDataStore.setTone(tone) }

    fun setMaxPerDay(value: Int) = viewModelScope.launch { settingsDataStore.setMaxNudgesPerDay(value) }

    fun setCooldown(minutes: Int) = viewModelScope.launch { settingsDataStore.setCooldownMinutes(minutes) }

    fun setQuietHours(start: Int, end: Int) = viewModelScope.launch { settingsDataStore.setQuietHours(start, end) }

    /** Tombol "jeda 3 hari" — mencegah uninstall karena kesal (PLAN.md §4). */
    fun pauseThreeDays() = viewModelScope.launch { settingsDataStore.pauseFor(3) }

    fun clearPause() = viewModelScope.launch { settingsDataStore.clearPause() }
}
