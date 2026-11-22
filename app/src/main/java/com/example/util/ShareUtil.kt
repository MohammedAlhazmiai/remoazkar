package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * أدوات المشاركة — تشارك رابط التطبيق أو أي نص عبر أي تطبيق مثبّت،
 * مع مسارات مباشرة للواتساب والبلوتوث عند توفّرهما.
 */
object ShareUtil {

    /** رابط التطبيق الرسمي على جيت‌هاب (يُستخدم في المشاركة). */
    const val APP_LINK = "https://github.com/MohammedAlhazmiai/remoazkar"

    /** رسالة المشاركة الجاهزة. */
    fun inviteMessage(): String =
        "تطبيق زاد المسلم — قرآن كريم وأذكار وأدعية ورقية شرعية، مع قارئ ذكي يقرأ لك أي نص.\n" +
            "حمّل التطبيق من هنا: $APP_LINK"

    private fun baseIntent(text: String, subject: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }

    /** مشاركة عامة عبر أي تطبيق (يشمل البلوتوث لأنه ضمن قائمة المشاركة). */
    fun share(context: Context, text: String, subject: String = "زاد المسلم") {
        val chooser = Intent.createChooser(baseIntent(text, subject), "مشاركة").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            // لا يوجد أي تطبيق يدعم المشاركة — لا نفعل شيئًا بدلًا من الانهيار.
        }
    }

    /** مشاركة مباشرة إلى واتساب (أو واتساب للأعمال)، مع رجوع آمن للمشاركة العامة. */
    fun shareWhatsapp(context: Context, text: String) {
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            val intent = baseIntent(text, "زاد المسلم").apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // جرّب الحزمة التالية.
            }
        }
        share(context, text)
    }

    /**
     * مشاركة عبر البلوتوث: يوجّه الطلب إلى تطبيق البلوتوث النظامي إن وُجد،
     * وإلا يفتح قائمة المشاركة العامة التي تحتوي خيار البلوتوث.
     */
    fun shareBluetooth(context: Context, text: String) {
        val packages = listOf("com.android.bluetooth", "com.google.android.bluetooth")
        for (pkg in packages) {
            val intent = baseIntent(text, "زاد المسلم").apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // جرّب الحزمة التالية.
            }
        }
        share(context, text)
    }

    /** فتح رابط خارجي (تلجرام، المدونة، المنصة). */
    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // لا يوجد متصفح.
        }
    }
}

object FavKeys {
    fun hisn(chapterId: Int, index: Int) = "hisn:$chapterId:$index"
    fun ayah(surah: Int, number: Int) = "ayah:$surah:$number"
    fun surah(number: Int) = "surah:$number"
    fun custom(id: Long) = "custom:$id"
}
