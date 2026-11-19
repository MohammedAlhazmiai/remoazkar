package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * معالج تلف ملف التفضيلات.
 *
 * سبب انهيار التطبيق عند الإقلاع: ملف DataStore التالف (الذي يُستعاد من النسخ
 * الاحتياطي لأندرويد عند إعادة التثبيت) كان يرمي CorruptionException أثناء أول
 * قراءة، فيقتل التطبيق قبل ظهور شاشة الدخول. هذا المعالج يستبدل الملف التالف
 * بملف فارغ سليم بدل الانهيار.
 */
private val preferencesCorruptionHandler =
    ReplaceFileCorruptionHandler<Preferences> { emptyPreferences() }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rim_athkar_preferences",
    corruptionHandler = preferencesCorruptionHandler
)

/**
 * قراءة آمنة: أي خطأ إدخال/إخراج (ملف تالف، قرص ممتلئ، صلاحيات) يُرجع تفضيلات
 * فارغة بدل إسقاط التطبيق.
 */
private val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }

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
        val AZKAR_TIME_HOUR_KEY = intPreferencesKey("azkar_time_hour")
        val AZKAR_TIME_MINUTE_KEY = intPreferencesKey("azkar_time_minute")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        val FONT_FAMILY_KEY = intPreferencesKey("font_family_index")
        val TASHKEEL_KEY = booleanPreferencesKey("tashkeel_enabled")

        // --- خطة ختم القرآن ---
        val KHATMAH_DAYS_KEY = intPreferencesKey("khatmah_days")
        val KHATMAH_START_KEY = longPreferencesKey("khatmah_start_millis")
        val KHATMAH_PAGES_DONE_KEY = intPreferencesKey("khatmah_pages_done")
        val KHATMAH_ACTIVE_KEY = booleanPreferencesKey("khatmah_active")

        /** ملف SharedPreferences مرآة للقراءة المتزامنة من مستقبِل الإشعارات. */
        private const val MIRROR_FILE = "rim_mirror"
        private const val MIRROR_NOTIFICATION_SOUND = "notification_sound"
        private const val MIRROR_AUTO_REMINDERS = "auto_reminders"
        private const val MIRROR_REMINDER_INTERVAL = "reminder_interval"
        private const val MIRROR_AZKAR_HOUR = "azkar_hour"
        private const val MIRROR_AZKAR_MINUTE = "azkar_minute"

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

        fun isAutoRemindersEnabled(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .getBoolean(MIRROR_AUTO_REMINDERS, true)

        fun reminderInterval(context: Context): Int =
            context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .getInt(MIRROR_REMINDER_INTERVAL, 60)

        fun azkarTime(context: Context): Pair<Int, Int> {
            val sp = context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
            return sp.getInt(MIRROR_AZKAR_HOUR, 6) to sp.getInt(MIRROR_AZKAR_MINUTE, 30)
        }

        fun mirrorAzkarSettings(
            context: Context,
            enabled: Boolean,
            intervalMinutes: Int,
            hour: Int,
            minute: Int
        ) {
            context.applicationContext
                .getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(MIRROR_AUTO_REMINDERS, enabled)
                .putInt(MIRROR_REMINDER_INTERVAL, intervalMinutes)
                .putInt(MIRROR_AZKAR_HOUR, hour)
                .putInt(MIRROR_AZKAR_MINUTE, minute)
                .apply()
        }
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.safeData
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.safeData
        .map { preferences -> preferences[FAVORITES_KEY] ?: emptySet() }

    val autoRemindersFlow: Flow<Boolean> = context.dataStore.safeData
        .map { preferences -> preferences[AUTO_REMINDERS_KEY] ?: true }

    val reminderIntervalFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[REMINDER_INTERVAL_KEY] ?: 60 }

    val tasbeehTotalFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[TASBEEH_TOTAL_KEY] ?: 0 }

    val speechRateFlow: Flow<Float> = context.dataStore.safeData
        .map { preferences -> preferences[SPEECH_RATE_KEY] ?: 1.0f }

    val speechPitchFlow: Flow<Float> = context.dataStore.safeData
        .map { preferences -> preferences[SPEECH_PITCH_KEY] ?: 1.0f }

    val notificationSoundFlow: Flow<Boolean> = context.dataStore.safeData
        .map { preferences -> preferences[NOTIFICATION_SOUND_KEY] ?: true }

    val fontSizeFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[FONT_SIZE_KEY] ?: 1 } // 0: Small, 1: Medium, 2: Large

    /** وقت تشغيل إشعارات الأذكار اليومية (ساعة، دقيقة). */
    val azkarTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.safeData
        .map { preferences ->
            val hour = preferences[AZKAR_TIME_HOUR_KEY] ?: 6
            val minute = preferences[AZKAR_TIME_MINUTE_KEY] ?: 30
            hour to minute
        }

    val languageFlow: Flow<String> = context.dataStore.safeData
        .map { preferences -> preferences[LANGUAGE_KEY] ?: AppLocale.ARABIC }

    val fontFamilyFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[FONT_FAMILY_KEY] ?: 0 }

    val tashkeelFlow: Flow<Boolean> = context.dataStore.safeData
        .map { preferences -> preferences[TASHKEEL_KEY] ?: true }

    // --- خطة ختم القرآن ---
    val khatmahDaysFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[KHATMAH_DAYS_KEY] ?: 30 }

    val khatmahStartFlow: Flow<Long> = context.dataStore.safeData
        .map { preferences -> preferences[KHATMAH_START_KEY] ?: 0L }

    val khatmahPagesDoneFlow: Flow<Int> = context.dataStore.safeData
        .map { preferences -> preferences[KHATMAH_PAGES_DONE_KEY] ?: 0 }

    val khatmahActiveFlow: Flow<Boolean> = context.dataStore.safeData
        .map { preferences -> preferences[KHATMAH_ACTIVE_KEY] ?: false }

    suspend fun startKhatmah(days: Int, startMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[KHATMAH_DAYS_KEY] = days
            preferences[KHATMAH_START_KEY] = startMillis
            preferences[KHATMAH_PAGES_DONE_KEY] = 0
            preferences[KHATMAH_ACTIVE_KEY] = true
        }
    }

    suspend fun setKhatmahPagesDone(pages: Int) {
        context.dataStore.edit { preferences ->
            preferences[KHATMAH_PAGES_DONE_KEY] = pages.coerceIn(0, 604)
        }
    }

    suspend fun stopKhatmah() {
        context.dataStore.edit { preferences ->
            preferences[KHATMAH_ACTIVE_KEY] = false
            preferences[KHATMAH_PAGES_DONE_KEY] = 0
        }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = code
        }
    }

    suspend fun setFontFamily(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_FAMILY_KEY] = index.coerceIn(0, 20)
        }
    }

    suspend fun setTashkeel(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TASHKEEL_KEY] = enabled
        }
    }

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
        val interval = reminderInterval(context)
        val time = azkarTime(context)
        mirrorAzkarSettings(context, enabled, interval, time.first, time.second)
    }

    suspend fun setReminderInterval(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_INTERVAL_KEY] = minutes
        }
        val time = azkarTime(context)
        mirrorAzkarSettings(context, isAutoRemindersEnabled(context), minutes, time.first, time.second)
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

    suspend fun setAzkarTime(hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        context.dataStore.edit { preferences ->
            preferences[AZKAR_TIME_HOUR_KEY] = h
            preferences[AZKAR_TIME_MINUTE_KEY] = m
        }
        mirrorAzkarSettings(context, isAutoRemindersEnabled(context), reminderInterval(context), h, m)
    }
}
