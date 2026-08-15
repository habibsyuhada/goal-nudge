package com.goalnudge.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goalnudge.app.domain.model.Tone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "goal_nudge_settings")

data class UserSettings(
    val tone: Tone = Tone.NETRAL,
    val maxNudgesPerDay: Int = 4,
    val cooldownMinutes: Int = 90,
    val quietHoursStartHour: Int = 23,
    val quietHoursEndHour: Int = 6,
    val pauseUntilEpochDay: Long? = null,
    val onboardingCompleted: Boolean = false
) {
    val isPaused: Boolean
        get() = pauseUntilEpochDay?.let { LocalDate.now().toEpochDay() < it } ?: false
}

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val TONE = stringPreferencesKey("tone")
        val MAX_PER_DAY = intPreferencesKey("max_nudges_per_day")
        val COOLDOWN_MIN = intPreferencesKey("cooldown_minutes")
        val QUIET_START = intPreferencesKey("quiet_hours_start")
        val QUIET_END = intPreferencesKey("quiet_hours_end")
        val PAUSE_UNTIL = longPreferencesKey("pause_until_epoch_day")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            tone = prefs[Keys.TONE]?.let { runCatching { Tone.valueOf(it) }.getOrNull() } ?: Tone.NETRAL,
            maxNudgesPerDay = prefs[Keys.MAX_PER_DAY] ?: 4,
            cooldownMinutes = prefs[Keys.COOLDOWN_MIN] ?: 90,
            quietHoursStartHour = prefs[Keys.QUIET_START] ?: 23,
            quietHoursEndHour = prefs[Keys.QUIET_END] ?: 6,
            pauseUntilEpochDay = prefs[Keys.PAUSE_UNTIL],
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun setTone(tone: Tone) {
        context.dataStore.edit { it[Keys.TONE] = tone.name }
    }

    suspend fun setMaxNudgesPerDay(value: Int) {
        context.dataStore.edit { it[Keys.MAX_PER_DAY] = value.coerceIn(1, 8) }
    }

    suspend fun setCooldownMinutes(value: Int) {
        context.dataStore.edit { it[Keys.COOLDOWN_MIN] = value.coerceIn(15, 480) }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.dataStore.edit {
            it[Keys.QUIET_START] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_END] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun pauseFor(days: Long) {
        context.dataStore.edit { it[Keys.PAUSE_UNTIL] = LocalDate.now().plusDays(days).toEpochDay() }
    }

    suspend fun clearPause() {
        context.dataStore.edit { it.remove(Keys.PAUSE_UNTIL) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }
}
