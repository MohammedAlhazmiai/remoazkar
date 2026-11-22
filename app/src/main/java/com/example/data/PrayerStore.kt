package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

data class CityPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class ResolvedPrayerTime(
    val type: PrayerType,
    val hour: Int,
    val minute: Int,
    val isManualOverride: Boolean,
    val enabled: Boolean
) {
    fun formatted24(): String = String.format("%02d:%02d", hour, minute)

    fun formatted12(): String {
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val period = if (hour < 12) "ص" else "م"
        return String.format("%d:%02d %s", h, minute, period)
    }

    fun nextCalendarAfter(nowMillis: Long): Calendar {
        val c = Calendar.getInstance()
        c.timeInMillis = nowMillis
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= nowMillis) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c
    }
}

data class NextPrayerSummary(
    val prayer: ResolvedPrayerTime,
    val triggerMillis: Long
)

/**
 * مخزن إعدادات مواقيت الصلاة والموقع الجغرافي والقبلة.
 * يدعم:
 * 1. التحديد التلقائي عبر GPS / الشبكة + Geocoder.
 * 2. اختيار مدينة عربية/إسلامية جاهزة أو إدخال الإحداثيات يدويًا.
 * 3. تعديل وقت كل صلاة يدويًا (HH:MM) أو ضبط فرق الدقائق من الإعدادات أو شاشة المواقيت.
 */
object PrayerStore {

    private const val PREFS = "zad_prayer_settings"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_LOC_NAME = "loc_name"
    private const val KEY_AUTO_LOC = "auto_loc"
    private const val KEY_METHOD = "calc_method"
    private const val KEY_MADHAB = "asr_madhab"
    private const val KEY_NOTIF_ENABLED = "prayer_notif_enabled"
    private const val KEY_LEAD_MINUTES = "prayer_lead_minutes"
    private const val KEY_PRAYER_ENABLED_PREFIX = "enabled_"
    private const val KEY_MANUAL_TIME_PREFIX = "manual_time_"
    private const val KEY_OFFSET_PREFIX = "offset_min_"
    private const val KEY_LAST_GPS_MILLIS = "last_gps_millis"

    // الإحداثيات الافتراضية (صنعاء، اليمن) إلى أن يحدّث المستخدم موقعه أو يختار مدينة
    const val DEFAULT_LAT = 15.3694
    const val DEFAULT_LON = 44.1910
    const val DEFAULT_LOCATION_NAME = "صنعاء، اليمن"

    var revision by mutableIntStateOf(0)
        private set

    val cityPresets: List<CityPreset> = listOf(
        CityPreset("صنعاء، اليمن", 15.3694, 44.1910),
        CityPreset("عدن، اليمن", 12.7855, 45.0187),
        CityPreset("تعز، اليمن", 13.5795, 44.0209),
        CityPreset("المكلا، اليمن", 14.5425, 49.1242),
        CityPreset("مكة المكرمة", 21.3891, 39.8579),
        CityPreset("المدينة المنورة", 24.5247, 39.5692),
        CityPreset("الرياض", 24.7136, 46.6753),
        CityPreset("جدة", 21.4858, 39.1925),
        CityPreset("القاهرة", 30.0444, 31.2357),
        CityPreset("القدس", 31.7683, 35.2137),
        CityPreset("عمّان", 31.9454, 35.9284),
        CityPreset("دمشق", 33.5138, 36.2765),
        CityPreset("بغداد", 33.3152, 44.3661),
        CityPreset("الكويت", 29.3759, 47.9774),
        CityPreset("الدوحة", 25.2854, 51.5310),
        CityPreset("أبوظبي", 24.4539, 54.3773),
        CityPreset("مسقط", 23.5880, 58.3829),
        CityPreset("المنامة", 26.2285, 50.5860),
        CityPreset("إسطنبول", 41.0082, 28.9784),
        CityPreset("الرباط", 34.0209, -6.8416)
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun latitude(context: Context): Double =
        java.lang.Double.longBitsToDouble(
            prefs(context).getLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(DEFAULT_LAT))
        )

    fun longitude(context: Context): Double =
        java.lang.Double.longBitsToDouble(
            prefs(context).getLong(KEY_LON, java.lang.Double.doubleToRawLongBits(DEFAULT_LON))
        )

    fun locationName(context: Context): String =
        prefs(context).getString(KEY_LOC_NAME, DEFAULT_LOCATION_NAME) ?: DEFAULT_LOCATION_NAME

    fun isAutoLocation(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_LOC, true)

    fun method(context: Context): PrayerCalculationMethod =
        PrayerCalculationMethod.fromId(prefs(context).getString(KEY_METHOD, null))

    fun madhab(context: Context): AsrMadhab =
        AsrMadhab.fromId(prefs(context).getString(KEY_MADHAB, null))

    fun isNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIF_ENABLED, true)

    fun leadMinutes(context: Context): Int =
        prefs(context).getInt(KEY_LEAD_MINUTES, 0).coerceIn(0, 30)

    fun isPrayerEnabled(context: Context, type: PrayerType): Boolean =
        prefs(context).getBoolean(KEY_PRAYER_ENABLED_PREFIX + type.id, true)

    fun manualTime(context: Context, type: PrayerType): Pair<Int, Int>? {
        val raw = prefs(context).getString(KEY_MANUAL_TIME_PREFIX + type.id, null) ?: return null
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    fun offsetMinutes(context: Context, type: PrayerType): Int =
        prefs(context).getInt(KEY_OFFSET_PREFIX + type.id, 0).coerceIn(-60, 60)

    fun setLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        name: String,
        isAuto: Boolean
    ) {
        prefs(context).edit()
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(latitude))
            .putLong(KEY_LON, java.lang.Double.doubleToRawLongBits(longitude))
            .putString(KEY_LOC_NAME, name)
            .putBoolean(KEY_AUTO_LOC, isAuto)
            .putLong(KEY_LAST_GPS_MILLIS, System.currentTimeMillis())
            .apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setMethod(context: Context, method: PrayerCalculationMethod) {
        prefs(context).edit().putString(KEY_METHOD, method.id).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setMadhab(context: Context, madhab: AsrMadhab) {
        prefs(context).edit().putString(KEY_MADHAB, madhab.id).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setLeadMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_LEAD_MINUTES, minutes.coerceIn(0, 30)).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setPrayerEnabled(context: Context, type: PrayerType, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRAYER_ENABLED_PREFIX + type.id, enabled).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setManualTime(context: Context, type: PrayerType, hour: Int?, minute: Int?) {
        val editor = prefs(context).edit()
        if (hour == null || minute == null) {
            editor.remove(KEY_MANUAL_TIME_PREFIX + type.id)
        } else {
            val h = hour.coerceIn(0, 23)
            val m = minute.coerceIn(0, 59)
            editor.putString(KEY_MANUAL_TIME_PREFIX + type.id, String.format("%02d:%02d", h, m))
        }
        editor.apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun clearAllManualOverrides(context: Context) {
        val editor = prefs(context).edit()
        PrayerType.entries.forEach { type ->
            editor.remove(KEY_MANUAL_TIME_PREFIX + type.id)
            editor.remove(KEY_OFFSET_PREFIX + type.id)
        }
        editor.apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    fun setOffsetMinutes(context: Context, type: PrayerType, offset: Int) {
        prefs(context).edit().putInt(KEY_OFFSET_PREFIX + type.id, offset.coerceIn(-60, 60)).apply()
        revision++
        PrayerAlarmScheduler.scheduleAll(context)
    }

    /** يحسب مواقيت الصلوات الخمس ليوم معيّن مع دمج أي تعديل يدوي. */
    fun resolveDayPrayers(
        context: Context,
        date: Calendar = Calendar.getInstance()
    ): Pair<Pair<Int, Int>, List<ResolvedPrayerTime>> {
        val lat = latitude(context)
        val lon = longitude(context)
        val calc = PrayerTimesCalculator.calculateDay(
            date = date,
            latitude = lat,
            longitude = lon,
            method = method(context),
            madhab = madhab(context)
        )
        val list = PrayerType.entries.map { type ->
            val manual = manualTime(context, type)
            if (manual != null) {
                ResolvedPrayerTime(
                    type = type,
                    hour = manual.first,
                    minute = manual.second,
                    isManualOverride = true,
                    enabled = isPrayerEnabled(context, type)
                )
            } else {
                val base = calc.times[type] ?: (12 to 0)
                val offset = offsetMinutes(context, type)
                val totalMin = (base.first * 60 + base.second + offset).mod(24 * 60)
                ResolvedPrayerTime(
                    type = type,
                    hour = totalMin / 60,
                    minute = totalMin % 60,
                    isManualOverride = offset != 0,
                    enabled = isPrayerEnabled(context, type)
                )
            }
        }
        return (calc.sunriseHour to calc.sunriseMinute) to list
    }

    /** يعيد الصلاة القادمة الأقرب ووقت حلولها بالميلي ثانية. */
    fun nextPrayer(context: Context, nowMillis: Long = System.currentTimeMillis()): NextPrayerSummary? {
        val (_, list) = resolveDayPrayers(context)
        return list
            .map { item -> NextPrayerSummary(item, item.nextCalendarAfter(nowMillis).timeInMillis) }
            .minByOrNull { it.triggerMillis }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * يحاول قراءة الموقع الجغرافي الحالي للمستخدم عبر GPS / Network،
     * ويحدّث اسم المدينة وخطوط الطول والعرض تلقائيًا.
     */
    @SuppressLint("MissingPermission")
    fun refreshLocationFromDevice(
        context: Context,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onComplete(false, "يرجى منح إذن الموقع الجغرافي أولًا")
            return
        }
        val appContext = context.applicationContext
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onComplete(false, "خدمة الموقع غير متاحة على هذا الجهاز")
            return
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (bestLocation == null || loc.time > bestLocation.time) {
                bestLocation = loc
            }
        }

        val twoMinutesAgo = System.currentTimeMillis() - 2 * 60 * 1000L
        if (bestLocation != null && bestLocation.time >= twoMinutesAgo) {
            applyDetectedLocation(appContext, bestLocation, onComplete)
            return
        }

        val activeProvider = when {
            runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                LocationManager.NETWORK_PROVIDER
            runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                LocationManager.GPS_PROVIDER
            else -> null
        }

        if (activeProvider == null) {
            if (bestLocation != null) {
                applyDetectedLocation(appContext, bestLocation, onComplete)
            } else {
                onComplete(false, "فعّل GPS أو اختر مدينتك يدويًا")
            }
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var finished = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (finished) return
                finished = true
                runCatching { lm.removeUpdates(this) }
                applyDetectedLocation(appContext, location, onComplete)
            }

            @Deprecated("Deprecated in Android API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            lm.requestLocationUpdates(activeProvider, 0L, 0f, listener, Looper.getMainLooper())
            handler.postDelayed({
                if (finished) return@postDelayed
                finished = true
                runCatching { lm.removeUpdates(listener) }
                if (bestLocation != null) {
                    applyDetectedLocation(appContext, bestLocation, onComplete)
                } else {
                    onComplete(false, "تعذّر التقاط إشارة GPS حاليًا؛ يمكنك اختيار مدينتك يدويًا")
                }
            }, 6500L)
        } catch (_: Exception) {
            if (bestLocation != null) {
                applyDetectedLocation(appContext, bestLocation, onComplete)
            } else {
                onComplete(false, "تعذّر تحديث الموقع؛ يمكنك اختيار المدينة يدويًا")
            }
        }
    }

    private fun applyDetectedLocation(
        context: Context,
        location: Location,
        onComplete: (Boolean, String) -> Unit
    ) {
        val lat = location.latitude
        val lon = location.longitude
        val mainHandler = Handler(Looper.getMainLooper())
        thread(start = true) {
            val resolvedName = resolvePlaceName(context, lat, lon)
            mainHandler.post {
                setLocation(context, lat, lon, resolvedName, isAuto = true)
                onComplete(true, "تم تحديث الموقع: $resolvedName")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolvePlaceName(context: Context, lat: Double, lon: Double): String {
        val fallback = String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
        return runCatching {
            val geocoder = Geocoder(context, Locale("ar"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // استدعاء متزامن داخل خيط خلفي (يُنفّذ في thread)
                var label: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lon, 1) { list ->
                    val addr = list.firstOrNull()
                    val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                    val country = addr?.countryName
                    label = listOfNotNull(city, country).filter { it.isNotBlank() }.joinToString("، ")
                    latch.countDown()
                }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                label?.takeIf { it.isNotBlank() } ?: fallback
            } else {
                val list = geocoder.getFromLocation(lat, lon, 1)
                val addr = list?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                val country = addr?.countryName
                listOfNotNull(city, country).filter { it.isNotBlank() }.joinToString("، ")
                    .takeIf { it.isNotBlank() } ?: fallback
            }
        }.getOrDefault(fallback)
    }
}
