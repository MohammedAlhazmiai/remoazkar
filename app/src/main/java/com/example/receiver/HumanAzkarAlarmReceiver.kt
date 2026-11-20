package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.service.AudioAzkarPlaybackService

class HumanAzkarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!HumanAzkarScheduler.isEnabled(context)) return

        val resourceName = HumanAzkarScheduler.takeNextTrackResource(context)
        val canStartMediaService = HumanAzkarScheduler.canScheduleExactAlarms(context)

        if (canStartMediaService) {
            val playbackIntent = Intent(context, AudioAzkarPlaybackService::class.java).apply {
                putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, "أذكار صوتية بشرية")
                putExtra(AudioAzkarPlaybackService.EXTRA_AUDIO_RESOURCE_NAME, resourceName)
            }
            val started = runCatching {
                ContextCompat.startForegroundService(context, playbackIntent)
            }.isSuccess
            if (!started) {
                NotificationHelper.showHumanAzkarFallback(
                    context = context,
                    id = (System.currentTimeMillis() and 0x7fffffff).toInt(),
                    title = "حان وقت الذكر",
                    body = "تعذّر بدء مشغل التسجيلات؛ سيصدر إشعار بالمقطع التالي.",
                    resourceName = resourceName
                )
            }
        } else {
            NotificationHelper.showHumanAzkarFallback(
                context = context,
                id = (System.currentTimeMillis() and 0x7fffffff).toInt(),
                title = "حان وقت الذكر",
                body = "المقطع التالي من التسجيلات البشرية. اسمح بالمنبهات الدقيقة لتحسين الالتزام بالموعد.",
                resourceName = resourceName
            )
        }

        // Keep notifications spaced from the alarm delivery time, even if Android delays it.
        HumanAzkarScheduler.rescheduleIfEnabled(context)
    }
}
