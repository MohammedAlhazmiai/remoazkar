package com.example.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.data.HisnRepository
import com.example.data.NotificationHelper
import java.util.Calendar

/**
 * عمّال الأذكار الدوري:
 * يختار بابًا مناسبًا للوقت الحالي من بيانات "حصن المسلم" الكاملة (132 بابًا)
 * ويُظهر إشعارًا حقيقيًا بذكرٍ من داخله — لا نصوص مكتوبة يدويًا.
 */
class AthkarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = NotificationHelper.CHANNEL_REMINDERS
        const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        val context = applicationContext
        HisnRepository.ensureLoaded(context)

        val chapterTitle = chapterForNow()
        val chapter = HisnRepository.chapterByTitle(chapterTitle)
            ?: HisnRepository.chapters.firstOrNull()
            ?: return Result.success()

        val item = chapter.items.randomOrNull() ?: return Result.success()

        val body = buildString {
            append(item.text)
            if (item.repeat > 1) append("\n\nتُكرَّر ${item.repeat} مرات.")
            if (item.footnote.isNotBlank()) append("\n\n${item.footnote}")
        }

        NotificationHelper.show(
            context = context,
            id = NOTIFICATION_ID,
            title = chapter.title,
            body = body,
            channel = CHANNEL_ID
        )
        return Result.success()
    }

    private fun chapterForNow(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "أذكار الصباح والمساء"
            in 12..15 -> "الأذكار بعد السلام من الصلاة"
            in 16..21 -> "أذكار الصباح والمساء"
            else -> "أذكار النوم"
        }
    }
}
