package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * اهتزاز قصير يُستخدم كتنبيه عند إتمام عدد تكرار الذكر.
 */
object VibrationHelper {

    fun completed(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // نبضتان قصيرتان للدلالة على اكتمال العدد
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 90, 70), -1)
            }
        }
    }
}
