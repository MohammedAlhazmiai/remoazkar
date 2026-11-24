package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أدوات تصدير وحفظ نصوص وبطاقات المساعد إلى ذاكرة الهاتف (Downloads & Pictures)،
 * مع قراءة وتفحص خصائص الصور والتسجيلات الصوتية المرفوعة من المستخدم.
 */
object MediaExportHelper {

    /**
     * حفظ رد المساعد كنص موثق داخل مجلد التنزيلات العام (Downloads) في جهاز المستخدم.
     * يُرجع المسار أو الوصف لعرضه للمستخدم.
     */
    fun saveTextToDownloads(
        context: Context,
        title: String,
        category: String,
        content: String,
        references: String
    ): Result<String> = runCatching {
        val cleanContent = ShareUtil.formatReadableText(content)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dateHuman = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date())
        val safeTitle = title
            .replace(Regex("[^\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFFa-zA-Z0-9_]"), "_")
            .take(30)
            .ifBlank { "رد_إسلامي" }
        val fileName = "زاد_المسلم_${safeTitle}_$timestamp.txt"

        val fileHeader = buildString {
            append("═══════════════════════════════════════════════════════════════\n")
            append("                زاد المسلم — إجابة المساعد الإسلامي الذكي       \n")
            append("═══════════════════════════════════════════════════════════════\n")
            append("الموضوع: $title\n")
            append("التصنيف: $category\n")
            append("تاريخ الحفظ: $dateHuman\n")
            if (references.isNotBlank()) append("المراجع والشواهد: $references\n")
            append("───────────────────────────────────────────────────────────────\n\n")
            append(cleanContent)
            append("\n\n───────────────────────────────────────────────────────────────\n")
            append("تطبيق زاد المسلم — القرآن الكريم والأذكار والمساعد الإسلامي\n")
            append("رابط التطبيق: ${ShareUtil.APP_LINK}\n")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ZadAlMuslim")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء الملف في التنزيلات")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write("\uFEFF".toByteArray(Charsets.UTF_8)) // UTF-8 BOM
                out.write(fileHeader.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("تعذر الكتابة في الملف")
            "مجلد التنزيلات (Downloads/ZadAlMuslim/$fileName)"
        } else {
            val publicSubDir = runCatching {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, "ZadAlMuslim").apply { mkdirs() }
            }.getOrNull()
            val targetDir = if (publicSubDir != null && publicSubDir.canWrite()) {
                publicSubDir
            } else {
                val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                File(appDownloads, "ZadAlMuslim").apply { mkdirs() }
            }
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                out.write("\uFEFF".toByteArray(Charsets.UTF_8))
                out.write(fileHeader.toByteArray(Charsets.UTF_8))
            }
            file.absolutePath
        }
    }

    /**
     * حفظ صورة أو بطاقة إسلامية في معرض صور الهاتف (Pictures/ZadAlMuslim).
     */
    fun saveImageToGallery(context: Context, imageFile: File, title: String): Result<String> = runCatching {
        if (!imageFile.exists()) throw IllegalArgumentException("ملف الصورة غير موجود")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "زاد_المسلم_بطاقة_$timestamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZadAlMuslim")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء الصورة في المعرض")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(imageFile).use { it.copyTo(out) }
            } ?: throw IllegalStateException("تعذر حفظ بيانات الصورة")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "معرض الصور (Pictures/ZadAlMuslim)"
        } else {
            val publicSubDir = runCatching {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                File(picturesDir, "ZadAlMuslim").apply { mkdirs() }
            }.getOrNull()
            val targetDir = if (publicSubDir != null && publicSubDir.canWrite()) {
                publicSubDir
            } else {
                val appPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                File(appPictures, "ZadAlMuslim").apply { mkdirs() }
            }
            val destFile = File(targetDir, fileName)
            imageFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        }
    }

    /** قراءة بيانات واستخراج ملخص عن صورة مرفوعة. */
    fun inspectImage(context: Context, uri: Uri): String {
        return runCatching {
            val filename = getFileName(context, uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            val w = options.outWidth
            val h = options.outHeight
            "صورة مرفوعة: $filename (${w}x${h} بكسل)"
        }.getOrDefault("صورة مرفوعة من الهاتف")
    }

    /** قراءة بيانات وتفحص ملف صوتي مرفوع. */
    fun inspectAudio(context: Context, uri: Uri): String {
        return runCatching {
            val filename = getFileName(context, uri)
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            val seconds = (durationMs / 1000).toInt()
            val mins = seconds / 60
            val secs = seconds % 60
            val durationStr = String.format(Locale.US, "%02d:%02d", mins, secs)
            "تسجيل صوتي: $filename (المدة: $durationStr)"
        }.getOrDefault("تسجيل صوتي مرفوع من الهاتف")
    }

    fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "ملف"
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) cursor.getString(nameIdx) else null
                } else null
            }
        }.getOrNull() ?: (uri.lastPathSegment ?: "ملف")
    }
}
