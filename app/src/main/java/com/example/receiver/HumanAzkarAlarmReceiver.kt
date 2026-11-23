package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.service.AudioAzkarPlaybackService

class HumanAzkarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!HumanAzkarScheduler.isEnabled(context)) return

        if (!HumanAzkarScheduler.canScheduleExactAlarms(context)) {
            Log.w(TAG, "Exact-alarm access was revoked; pausing periodic human-azkar playback")
            HumanAzkarScheduler.cancel(context)
            return
        }

        val resourceName = HumanAzkarScheduler.takeNextTrackResource(context)
        val trackTitle = com.example.data.HumanAzkarAudioCatalog.trackByResourceName(resourceName)?.title
            ?: "أذكار صوتية بشرية"
        try {
            val playbackIntent = Intent(context, AudioAzkarPlaybackService::class.java).apply {
                putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, trackTitle)
                putExtra(AudioAzkarPlaybackService.EXTRA_AUDIO_RESOURCE_NAME, resourceName)
            }
            ContextCompat.startForegroundService(context, playbackIntent)
        } catch (error: Exception) {
            Log.e(TAG, "Android refused to start the human-azkar media service", error)
            runCatching {
                NotificationHelper.showHumanAzkarFallback(
                    context = context,
                    id = (System.currentTimeMillis() and 0x7fffffff).toInt(),
                    title = "حان وقت الذكر: $trackTitle",
                    body = "تعذّر بدء خدمة تشغيل التسجيلات. تحقق من إعدادات البطارية والسماح بالتشغيل في الخلفية.",
                    resourceName = resourceName
                )
            }
        } finally {
            // Schedule from alarm delivery time so a delayed Doze alarm does not create a catch-up burst.
            HumanAzkarScheduler.rescheduleIfEnabled(context)
        }
    }

    private companion object {
        const val TAG = "HumanAzkarAlarmReceiver"
    }
}
