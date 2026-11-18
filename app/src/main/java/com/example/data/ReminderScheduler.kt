package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.ReminderReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Reminder(
    val id: Int,
    val title: String,
    val body: String,
    val hour: Int,
    val minute: Int,
    val daysMask: Int, // bit0=الأحد .. bit6=السبت ، 0 = كل يوم
    val enabled: Boolean
) {
    fun daysLabel(): String {
        if (daysMask == 0) return "كل يوم"
        if (daysMask == 0b1111111) return "كل أيام الأسبوع"
        val names = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val picked = names.filterIndexed { i, _ -> daysMask and (1 shl i) != 0 }
        return picked.joinToString("، ")
    }

    fun timeLabel(): String = String.format("%02d:%02d", hour, minute)
}

object ReminderScheduler {

    private const val PREFS = "rim_reminders_store"
    private const val KEY = "reminders_json"

    private const val REQ_KAHF = 9001
    private const val REQ_MULK = 9002

    fun load(context: Context): List<Reminder> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return defaults()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Reminder>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Reminder(
                        id = o.optInt("id"),
                        title = o.optString("title"),
                        body = o.optString("body"),
                        hour = o.optInt("hour", 8),
                        minute = o.optInt("minute", 0),
                        daysMask = o.optInt("days", 0),
                        enabled = o.optBoolean("enabled", true)
                    )
                )
            }
            out
        } catch (_: Exception) {
            defaults()
        }
    }

    fun save(context: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("body", r.body)
                    put("hour", r.hour)
                    put("minute", r.minute)
                    put("days", r.daysMask)
                    put("enabled", r.enabled)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun defaults(): List<Reminder> = listOf(
        Reminder(1, "أذكار الصباح", "حان وقت أذكار الصباح، ابدأ يومك بذكر الله.", 6, 30, 0, true),
        Reminder(2, "أذكار المساء", "حان وقت أذكار المساء، اجعل ليلك مطمئنًا بذكر الله.", 18, 0, 0, true),
        Reminder(3, "أذكار النوم", "قبل أن تنام، لا تنسَ أذكار النوم.", 22, 0, 0, true)
    )

    fun upsert(context: Context, reminder: Reminder) {
        val list = load(context).toMutableList()
        val idx = list.indexOfFirst { it.id == reminder.id }
        if (idx >= 0) list[idx] = reminder else list.add(reminder)
        save(context, list)
        if (reminder.enabled) schedule(context, reminder) else cancel(context, reminder.id)
    }

    fun remove(context: Context, id: Int) {
        val list = load(context).filterNot { it.id == id }
        save(context, list)
        cancel(context, id)
    }

    fun scheduleAll(context: Context) {
        load(context).filter { it.enabled }.forEach { schedule(context, it) }
        scheduleDailyFeatures(context)
    }

    fun schedule(context: Context, reminder: Reminder) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val trigger = nextTriggerMillis(reminder)
        val pi = pendingIntent(context, reminder)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.REMINDER_$id"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(context, id, intent, flags)
        am.cancel(pi)
    }

    /** الكهف يوم الجمعة، والملك كل ليلة */
    fun scheduleDailyFeatures(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val kahf = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.REMINDER_$REQ_KAHF"
            putExtra("id", REQ_KAHF)
            putExtra("title", "سورة الكهف")
            putExtra("body", "يوم الجمعة، لا تنسَ قراءة سورة الكهف؛ نور ما بين الجمعتين.")
        }
        val mulk = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.REMINDER_$REQ_MULK"
            putExtra("id", REQ_MULK)
            putExtra("title", "سورة الملك")
            putExtra("body", "قبل النوم، اقرأ سورة الملك؛ المانعة من عذاب القبر.")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val kahfPi = PendingIntent.getBroadcast(context, REQ_KAHF, kahf, flags)
        val mulkPi = PendingIntent.getBroadcast(context, REQ_MULK, mulk, flags)

        val kahfTime = nextWeekly(Calendar.FRIDAY, 9, 0)
        val mulkTime = nextDaily(21, 30)

        try {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, kahfTime, AlarmManager.INTERVAL_DAY * 7, kahfPi)
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, mulkTime, AlarmManager.INTERVAL_DAY, mulkPi)
        } catch (_: SecurityException) {
            // تجاهل
        }
    }

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.REMINDER_${reminder.id}"
            putExtra("id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("body", reminder.body)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, reminder.id, intent, flags)
    }

    private fun nextTriggerMillis(r: Reminder): Long {
        val now = Calendar.getInstance()
        for (offset in 0..7) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, offset)
            c.set(Calendar.HOUR_OF_DAY, r.hour)
            c.set(Calendar.MINUTE, r.minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis <= now.timeInMillis) continue
            if (r.daysMask == 0) return c.timeInMillis
            val dow = c.get(Calendar.DAY_OF_WEEK) - 1
            if (r.daysMask and (1 shl dow) != 0) return c.timeInMillis
        }
        return System.currentTimeMillis() + 24L * 60 * 60 * 1000
    }

    private fun nextDaily(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun nextWeekly(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        while (c.get(Calendar.DAY_OF_WEEK) != dayOfWeek || c.timeInMillis <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }
}
