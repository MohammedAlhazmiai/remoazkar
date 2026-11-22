package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * مخزن خلفية التطبيق.
 *
 * يحفظ الصورة التي يختارها المستخدم من معرض الهاتف داخل ذاكرة التطبيق الداخلية
 * (وليس مسارًا مؤقتًا قد ينتهي صلاحيته)، ويحفظ حالة التفعيل ودرجة التعتيم في
 * SharedPreferences. المتغيّر [revision] يُستخدم لإعادة رسم الواجهة فور تغيير الخلفية.
 */
object AppBackgroundStore {

    private const val PREFS = "rim_app_background"
    private const val KEY_ENABLED = "background_enabled"
    private const val KEY_SCRIM = "background_scrim"
    private const val FILE_NAME = "app_background.img"
    private const val LOG_TAG = "AppBackgroundStore"

    const val DEFAULT_SCRIM = 35

    /** يُزاد في كل تغيير حتى تُعيد الواجهة قراءة الصورة. */
    var revision by mutableIntStateOf(0)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** مسار الصورة المحفوظة إن وُجدت فعلًا على القرص. */
    fun imagePath(context: Context): String? {
        val target = file(context)
        return if (target.exists() && target.length() > 0) target.absolutePath else null
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true) && imagePath(context) != null

    fun scrimPercent(context: Context): Int =
        prefs(context).getInt(KEY_SCRIM, DEFAULT_SCRIM).coerceIn(0, 85)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        revision++
    }

    fun setScrimPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_SCRIM, percent.coerceIn(0, 85)).apply()
        revision++
    }

    /** ينسخ الصورة المختارة إلى ذاكرة التطبيق ويعيد نجاح العملية. */
    fun save(context: Context, uri: Uri): Boolean {
        return try {
            val target = file(context)
            val input = context.contentResolver.openInputStream(uri)
                ?: return false
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
            // التأكد أن الملف صورة صالحة قبل اعتمادها كخلفية.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                target.delete()
                return false
            }
            prefs(context).edit()
                .putBoolean(KEY_ENABLED, true)
                .apply()
            revision++
            true
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to save background image", error)
            false
        }
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
        prefs(context).edit().remove(KEY_ENABLED).apply()
        revision++
    }
}
