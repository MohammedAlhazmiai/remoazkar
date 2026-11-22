package com.example.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit as dataStoreEdit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PREFERENCES_LOG_TAG = "PreferencesManager"

data class PreferencesSnapshot(
    val darkMode: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val autoReminders: Boolean = true,
    val reminderInterval: Int = 60,
    val tasbeehTotal: Int = 0,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val notificationSound: Boolean = true,
    val fontSizeIndex: Int = 1,
    val azkarTime: Pair<Int, Int> = 6 to 30,
    val language: String = AppLocale.ARABIC,
    val fontFamilyIndex: Int = 0,
    val tashkeel: Boolean = true,
    val khatmahDays: Int = 30,
    val khatmahStart: Long = 0L,
    val khatmahPagesDone: Int = 0,
    val khatmahActive: Boolean = false,
    /** لون خلفية الصفحات بصيغة ARGB، و0 يعني اللون الافتراضي. */
    val pageColor: Long = 0L,
    /** لون الخطوط بصيغة ARGB، و0 يعني اللون الافتراضي. */
    val fontColor: Long = 0L
)

data class TasbeehState(
    val count: Int = 0,
    val target: Int = 33,
    val zikrIndex: Int = 0,
    val cyclesCompleted: Int = 0,
    val vibrationEnabled: Boolean = true
)

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
        if (throwable is IOException) {
            Log.e(PREFERENCES_LOG_TAG, "Unable to read preferences; using defaults", throwable)
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

/** Catch expected storage failures so a failed preference write cannot crash the UI coroutine. */
private suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit
): Boolean {
    return try {
        dataStoreEdit(transform)
        true
    } catch (error: IOException) {
        Log.e(PREFERENCES_LOG_TAG, "Unable to persist preferences", error)
        false
    }
}

class PreferencesManager(private val context: Context) {
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        val AUTO_REMINDERS_KEY = booleanPreferencesKey("auto_reminders")
        val REMINDER_INTERVAL_KEY = intPreferencesKey("reminder_interval")
        val TASBEEH_TOTAL_KEY = intPreferencesKey("tasbeeh_total")
        val TASBEEH_COUNT_KEY = intPreferencesKey("tasbeeh_current_count")
        val TASBEEH_TARGET_KEY = intPreferencesKey("tasbeeh_target")
        val TASBEEH_ZIKR_KEY = intPreferencesKey("tasbeeh_zikr_index")
        val TASBEEH_CYCLES_KEY = intPreferencesKey("tasbeeh_cycles")
        val TASBEEH_VIBRATION_KEY = booleanPreferencesKey("tasbeeh_vibration")
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

        // --- ألوان الصفحات والخطوط ---
        val PAGE_COLOR_KEY = longPreferencesKey("page_color_argb")
        val FONT_COLOR_KEY = longPreferencesKey("font_color_argb")

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

    val snapshotFlow: Flow<PreferencesSnapshot> = context.dataStore.safeData.map { preferences ->
        PreferencesSnapshot(
            darkMode = preferences[DARK_MODE_KEY] ?: false,
            favorites = preferences[FAVORITES_KEY] ?: emptySet(),
            autoReminders = preferences[AUTO_REMINDERS_KEY] ?: true,
            reminderInterval = preferences[REMINDER_INTERVAL_KEY] ?: 60,
            tasbeehTotal = preferences[TASBEEH_TOTAL_KEY] ?: 0,
            speechRate = preferences[SPEECH_RATE_KEY] ?: 1.0f,
            speechPitch = preferences[SPEECH_PITCH_KEY] ?: 1.0f,
            notificationSound = preferences[NOTIFICATION_SOUND_KEY] ?: true,
            fontSizeIndex = preferences[FONT_SIZE_KEY] ?: 1,
            azkarTime = (preferences[AZKAR_TIME_HOUR_KEY] ?: 6) to
                (preferences[AZKAR_TIME_MINUTE_KEY] ?: 30),
            language = preferences[LANGUAGE_KEY] ?: AppLocale.ARABIC,
            fontFamilyIndex = preferences[FONT_FAMILY_KEY] ?: 0,
            tashkeel = preferences[TASHKEEL_KEY] ?: true,
            khatmahDays = preferences[KHATMAH_DAYS_KEY] ?: 30,
            khatmahStart = preferences[KHATMAH_START_KEY] ?: 0L,
            khatmahPagesDone = preferences[KHATMAH_PAGES_DONE_KEY] ?: 0,
            khatmahActive = preferences[KHATMAH_ACTIVE_KEY] ?: false,
            pageColor = preferences[PAGE_COLOR_KEY] ?: 0L,
            fontColor = preferences[FONT_COLOR_KEY] ?: 0L
        )
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

    val tasbeehStateFlow: Flow<TasbeehState> = context.dataStore.safeData.map { preferences ->
        TasbeehState(
            count = preferences[TASBEEH_COUNT_KEY] ?: 0,
            target = preferences[TASBEEH_TARGET_KEY] ?: 33,
            zikrIndex = preferences[TASBEEH_ZIKR_KEY] ?: 0,
            cyclesCompleted = preferences[TASBEEH_CYCLES_KEY] ?: 0,
            vibrationEnabled = preferences[TASBEEH_VIBRATION_KEY] ?: true
        )
    }

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

    suspend fun setPageColor(argb: Long) {
        context.dataStore.edit { preferences ->
            if (argb == 0L) preferences.remove(PAGE_COLOR_KEY) else preferences[PAGE_COLOR_KEY] = argb
        }
    }

    suspend fun setFontColor(argb: Long) {
        context.dataStore.edit { preferences ->
            if (argb == 0L) preferences.remove(FONT_COLOR_KEY) else preferences[FONT_COLOR_KEY] = argb
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
        if (!context.dataStore.edit { preferences ->
            preferences[AUTO_REMINDERS_KEY] = enabled
        }) return
        val interval = reminderInterval(context)
        val time = azkarTime(context)
        mirrorAzkarSettings(context, enabled, interval, time.first, time.second)
    }

    suspend fun setReminderInterval(minutes: Int) {
        if (!context.dataStore.edit { preferences ->
            preferences[REMINDER_INTERVAL_KEY] = minutes
        }) return
        val time = azkarTime(context)
        mirrorAzkarSettings(context, isAutoRemindersEnabled(context), minutes, time.first, time.second)
    }

    suspend fun saveTasbeehTotal(total: Int) {
        context.dataStore.edit { preferences ->
            preferences[TASBEEH_TOTAL_KEY] = total
        }
    }

    suspend fun saveTasbeehState(state: TasbeehState) {
        context.dataStore.edit { preferences ->
            preferences[TASBEEH_COUNT_KEY] = state.count.coerceAtLeast(0)
            preferences[TASBEEH_TARGET_KEY] = state.target.coerceIn(1, 10000)
            preferences[TASBEEH_ZIKR_KEY] = state.zikrIndex.coerceIn(0, 100)
            preferences[TASBEEH_CYCLES_KEY] = state.cyclesCompleted.coerceAtLeast(0)
            preferences[TASBEEH_VIBRATION_KEY] = state.vibrationEnabled
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
        if (context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_KEY] = enabled
        }) {
            mirrorNotificationSound(context, enabled)
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
        if (!context.dataStore.edit { preferences ->
            preferences[AZKAR_TIME_HOUR_KEY] = h
            preferences[AZKAR_TIME_MINUTE_KEY] = m
        }) return
        mirrorAzkarSettings(context, isAutoRemindersEnabled(context), reminderInterval(context), h, m)
    }
}
