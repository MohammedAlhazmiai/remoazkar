package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * أدوات النسخ والمشاركة — تدعم النسخ النظيف، والمشاركة العامة عبر جميع تطبيقات التواصل،
 * والمشاركة المباشرة عبر واتساب، والمشاركة عبر البلوتوث (باستخدام FileProvider وبإسناد احتياطي آمن).
 */
object ShareUtil {

    /** رابط التطبيق الرسمي على جيت‌هاب (يُستخدم في المشاركة). */
    const val APP_LINK = "https://github.com/MohammedAlhazmiai/remoazkar"

    /** رسالة المشاركة الجاهزة. */
    fun inviteMessage(): String =
        "تطبيق زاد المسلم — قرآن كريم وأذكار وأدعية ورقية شرعية، مع مساعد إسلامي ذكي وقارئ صوتي.\n" +
            "حمّل التطبيق من هنا: $APP_LINK"

    /**
     * يزيل رموز التنسيق البرمجية مثل علامات النجمة المزدوجة `**` من الردود
     * ليظهر النص مقروءًا وأنيقًا عند النسخ أو المشاركة عبر واتساب وتطبيقات التواصل.
     */
    fun formatReadableText(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("**", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /** نسخ النص بأمان إلى الحافظة. */
    fun copyToClipboard(context: Context, text: String, label: String = "زاد المسلم"): Boolean {
        val clean = formatReadableText(text)
        if (clean.isEmpty()) return false
        return runCatching {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            manager.setPrimaryClip(ClipData.newPlainText(label, clean))
            true
        }.getOrDefault(false)
    }

    private fun Intent.applyLaunchFlags(context: Context): Intent {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return this
    }

    private fun baseTextIntent(cleanText: String, subject: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, cleanText)
        }

    /** مشاركة عامة عبر أي تطبيق تواصل اجتماعي أو بريد أو مشاركة قريبة مثبت على الهاتف. */
    fun share(context: Context, text: String, subject: String = "زاد المسلم") {
        val clean = formatReadableText(text)
        if (clean.isEmpty()) return
        val chooser = Intent.createChooser(baseTextIntent(clean, subject), "مشاركة عبر")
            .applyLaunchFlags(context)
        runCatching { context.startActivity(chooser) }
    }

    /** مشاركة مباشرة إلى واتساب (أو واتساب للأعمال)، مع رجوع آمن لقائمة المشاركة العامة. */
    fun shareWhatsapp(context: Context, text: String) {
        val clean = formatReadableText(text)
        if (clean.isEmpty()) return
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            val intent = baseTextIntent(clean, "زاد المسلم")
                .setPackage(pkg)
                .applyLaunchFlags(context)
            val launched = runCatching { context.startActivity(intent) }.isSuccess
            if (launched) return
        }
        share(context, clean)
    }

    /**
     * مشاركة عبر البلوتوث:
     * ينشئ ملفًا نصيًا بترميز UTF-8 (مع BOM لدعم العربية على كافة الأجهزة) ويشاركه عبر FileProvider
     * لتطبيق البلوتوث النظامي، ثم يحاول إرسال النص مباشرة، وإذا تعذّر ذلك في إصدار الجهاز يفتح
     * قائمة المشاركة النظامية التي تتضمن البلوتوث والمشاركة السريعة بأمان.
     */
    fun shareBluetooth(context: Context, text: String, subject: String = "زاد المسلم") {
        val clean = formatReadableText(text)
        if (clean.isEmpty()) return

        val btPackages = listOf("com.android.bluetooth", "com.google.android.bluetooth")
        val uri = createSharedTextFileUri(context, clean)

        if (uri != null) {
            for (pkg in btPackages) {
                val fileIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(pkg)
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, clean)
                    clipData = ClipData.newUri(context.contentResolver, subject, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    applyLaunchFlags(context)
                }
                if (runCatching { context.startActivity(fileIntent) }.isSuccess) {
                    return
                }
            }
        }

        for (pkg in btPackages) {
            val textIntent = baseTextIntent(clean, subject)
                .setPackage(pkg)
                .applyLaunchFlags(context)
            if (runCatching { context.startActivity(textIntent) }.isSuccess) {
                return
            }
        }

        share(context, clean, subject)
    }

    private fun createSharedTextFileUri(context: Context, cleanText: String): Uri? {
        return runCatching {
            val dir = File(context.cacheDir, "shared_texts").apply { mkdirs() }
            // تنظيف الملفات المؤقتة الأقدم من ساعة واحدة لعدم تراكمها في الكاش
            val cutoff = System.currentTimeMillis() - 3_600_000L
            dir.listFiles()?.forEach { old ->
                if (old.lastModified() < cutoff) {
                    old.delete()
                }
            }
            val file = File(dir, "zad_almuslim_${System.currentTimeMillis()}.txt")
            // كتابة BOM لضمان ظهور الأحرف العربية بوضوح عند فتح الملف على أي هاتف أو حاسوب
            file.writeText("\uFEFF$cleanText", Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    /** فتح رابط خارجي (تلجرام، المدونة، المنصة). */
    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).applyLaunchFlags(context)
        runCatching { context.startActivity(intent) }
    }
}

object FavKeys {
    fun hisn(chapterId: Int, index: Int) = "hisn:$chapterId:$index"
    fun ayah(surah: Int, number: Int) = "ayah:$surah:$number"
    fun surah(number: Int) = "surah:$number"
    fun custom(id: Long) = "custom:$id"
}
