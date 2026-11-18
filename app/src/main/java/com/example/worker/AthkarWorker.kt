package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.R

class AthkarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "athkar_notification_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        showAthkarNotification()
        return Result.success()
    }

    private fun showAthkarNotification() {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات الأذكار التلقائية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات دورية لأذكار الصباح والمساء والاستغفار"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val athkarList = listOf(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه",
            "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير",
            "اللهم صل وسلم على نبينا محمد وعلى آله وصحبه أجمعين",
            "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر"
        )

        val randomZikr = athkarList.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تذكير بذكر الله (ريمواذكار)")
            .setContentText(randomZikr)
            .setStyle(NotificationCompat.BigTextStyle().bigText(randomZikr))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
