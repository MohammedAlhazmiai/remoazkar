package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomZikr(
    val id: Long,
    val title: String,
    val text: String,
    val repeat: Int
)

/**
 * أذكار المستخدم الخاصة: إضافة وتعديل وحذف، محفوظة على الجهاز.
 */
object CustomAzkarStore {

    private const val PREFS = "rim_custom_azkar"
    private const val KEY = "items"

    fun load(context: Context): List<CustomZikr> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<CustomZikr>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    CustomZikr(
                        id = o.optLong("id"),
                        title = o.optString("title"),
                        text = o.optString("text"),
                        repeat = o.optInt("repeat", 1).coerceAtLeast(1)
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, list: List<CustomZikr>) {
        val arr = JSONArray()
        list.forEach { z ->
            arr.put(
                JSONObject().apply {
                    put("id", z.id)
                    put("title", z.title)
                    put("text", z.text)
                    put("repeat", z.repeat)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(context: Context, zikr: CustomZikr) {
        val list = load(context).toMutableList()
        val idx = list.indexOfFirst { it.id == zikr.id }
        if (idx >= 0) list[idx] = zikr else list.add(zikr)
        save(context, list)
    }

    fun delete(context: Context, id: Long) {
        save(context, load(context).filterNot { it.id == id })
    }

    fun newId(): Long = System.currentTimeMillis()
}
