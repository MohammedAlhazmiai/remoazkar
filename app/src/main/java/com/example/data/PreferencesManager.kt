package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[FAVORITES_KEY] ?: emptySet() }

    val autoRemindersFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_REMINDERS_KEY] ?: true }

    val reminderIntervalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[REMINDER_INTERVAL_KEY] ?: 60 } // minutes

    val tasbeehTotalFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[TASBEEH_TOTAL_KEY] ?: 0 }

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
}
