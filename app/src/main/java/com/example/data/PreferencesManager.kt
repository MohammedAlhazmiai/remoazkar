package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rim_athkar_preferences")

class PreferencesManager(private val context: Context) {
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        val AUTO_REMINDERS_KEY = booleanPreferencesKey("auto_reminders")
        val REMINDER_INTERVAL_KEY = intPreferencesKey("reminder_interval")
        val TASBEEH_TOTAL_KEY = intPreferencesKey("tasbeeh_total")
        val SPEECH_RATE_KEY = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH_KEY = floatPreferencesKey("speech_pitch")
        val NOTIFICATION_SOUND_KEY = booleanPreferencesKey("notification_sound")
        val FONT_SIZE_KEY = intPreferencesKey("font_size_index")

        /** ملف SharedPreferences مرآة للقراءة المتزامنة من مستقبِل الإشعارات. */
        private const val MIRROR_FILE = "rim_mirror"
        private const val MIRROR_NOTIFICATION_SOUND = "notification_sound"

        /** يُقرأ بشكل متزامن داخل BroadcastReceiver (لا يمكن قراءة DataStore هناك). */
        fun isNotificationSoundEnabled(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .getBoolean(MIRROR_NOTIFICATION_SOUND, true)

        fun mirrorNotificationSound(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(MIRROR_NOTIFICATION_SOUND, enabled)
                .apply()
        }
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[FAVORITES_KEY] ?: emptySet() }

    val autoRemindersFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_REMINDERS_KEY] ?: true }

    val reminderIntervalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[REMINDER_INTERVAL_KEY] ?: 60 }

    val tasbeehTotalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[TASBEEH_TOTAL_KEY] ?: 0 }

    val speechRateFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[SPEECH_RATE_KEY] ?: 1.0f }

    val speechPitchFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[SPEECH_PITCH_KEY] ?: 1.0f }

    val notificationSoundFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_SOUND_KEY] ?: true }

    val fontSizeFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[FONT_SIZE_KEY] ?: 1 } // 0: Small, 1: Medium, 2: Large

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun toggleFavorite(zikrId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = if (current.contains(zikrId)) {
                current - zikrId
            } else {
                current + zikrId
            }
        }
    }

    suspend fun setAutoReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_REMINDERS_KEY] = enabled
        }
    }

    suspend fun setReminderInterval(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_INTERVAL_KEY] = minutes
        }
    }

    suspend fun saveTasbeehTotal(total: Int) {
        context.dataStore.edit { preferences ->
            preferences[TASBEEH_TOTAL_KEY] = total
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEECH_RATE_KEY] = rate
        }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEECH_PITCH_KEY] = pitch
        }
    }

    suspend fun setNotificationSound(enabled: Boolean) {
        mirrorNotificationSound(context, enabled)
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_KEY] = enabled
        }
    }

    suspend fun setFontSize(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = index
        }
    }
}
