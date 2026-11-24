package com.example.service

import android.content.Context
import android.os.PowerManager

/**
 * قفل إيقاظ مشترك (WakeLock) لتأمين تسليم التنبيهات الصوتية
 * من BroadcastReceiver إلى AudioAzkarPlaybackService أو المشغّل الاحتياطي أثناء قفل الشاشة.
 */
object PlaybackWakeLock {
    private const val TAG = "ZadAlMuslim:BackgroundAudioWakeLock"

    @Volatile
    private var lock: PowerManager.WakeLock? = null

    fun acquire(context: Context, timeoutMs: Long = 45_000L) {
        synchronized(this) {
            runCatching {
                val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    ?: return
                val current = lock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
                    setReferenceCounted(false)
                }.also { lock = it }
                current.acquire(timeoutMs)
            }
        }
    }

    fun release() {
        synchronized(this) {
            runCatching {
                if (lock?.isHeld == true) {
                    lock?.release()
                }
            }
            lock = null
        }
    }
}
