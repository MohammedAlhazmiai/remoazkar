package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * مسبحة عائمة فوق شاشة الهاتف تعمل خارج التطبيق.
 *
 * نافذة نظام حقيقية (SYSTEM_ALERT_WINDOW) عبر خدمة أمامية، قابلة للسحب،
 * تُحتسب بالنقر على البطاقة، وتهتزّ عند إكمال العدد، وتُغلق بزر ✕.
 */
class FloatingTasbeehService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var countView: TextView? = null
    private var cycleView: TextView? = null

    private val prefs by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            setEnabledFlag(false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            setEnabledFlag(false)
            stopSelf()
            return START_NOT_STICKY
        }
        setEnabledFlag(true)
        startForegroundCompat(buildNotification())
        showOverlay()
        return START_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        if (rootView != null) return
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.parseColor("#E6101A15"))
                setStroke(dp(2), Color.parseColor("#4E9A78"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(12))
        }

        // شريط العنوان مع زر الإغلاق ✕
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "مسبحة"
            setTextColor(Color.parseColor("#E8F0EC"))
            textSize = 13f
            setPadding(0, 0, dp(10), 0)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF8A80"))
            textSize = 18f
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener {
                setEnabledFlag(false)
                stopSelf()
            }
        }
        header.addView(title)
        header.addView(close)

        val count = TextView(this).apply {
            text = prefs.getInt(KEY_COUNT, 0).toString()
            setTextColor(Color.parseColor("#E5C158"))
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
        }
        countView = count

        val cycle = TextView(this).apply {
            text = "الدورات: ${prefs.getInt(KEY_CYCLES, 0)} / ${prefs.getInt(KEY_TARGET, 33)}"
            setTextColor(Color.parseColor("#9EABA3"))
            textSize = 10f
            gravity = Gravity.CENTER
        }
        cycleView = cycle

        val reset = TextView(this).apply {
            text = "تصفير"
            setTextColor(Color.parseColor("#9EABA3"))
            textSize = 11f
            setPadding(dp(8), dp(6), dp(8), dp(2))
            setOnClickListener {
                prefs.edit().putInt(KEY_COUNT, 0).apply()
                count.text = "0"
            }
        }

        card.addView(header)
        card.addView(count)
        card.addView(cycle)
        card.addView(reset)

        // النقر على البطاقة (عدا زر الإغلاق والتصفير) يزيد العدد.
        card.setOnClickListener { increment() }

        // سحب البطاقة عبر شريط العنوان.
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params?.x ?: 0
                    startY = params?.y ?: 0
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val p = params ?: return@setOnTouchListener false
                    p.x = startX + (event.rawX - touchX).toInt()
                    p.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager?.updateViewLayout(card, p) }
                    true
                }
                else -> true
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(160)
        }

        runCatching { manager.addView(card, params) }
            .onFailure { stopSelf() }
        rootView = card
    }

    private var params: WindowManager.LayoutParams? = null

    private fun increment() {
        val target = prefs.getInt(KEY_TARGET, 33).coerceAtLeast(1)
        var current = prefs.getInt(KEY_COUNT, 0) + 1
        var cycles = prefs.getInt(KEY_CYCLES, 0)
        if (current >= target) {
            current = 0
            cycles += 1
            vibrate()
        } else {
            smallTick()
        }
        prefs.edit().putInt(KEY_COUNT, current).putInt(KEY_CYCLES, cycles).apply()
        countView?.text = current.toString()
        cycleView?.text = "الدورات: $cycles / $target"
    }

    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(120)
            }
        }
    }

    private fun smallTick() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, 40))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(18)
            }
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 711, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, FloatingTasbeehService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 712, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("المسبحة العائمة تعمل")
            .setContentText("اضغط على المسبحة لعدّ الأذكار، وزر ✕ لإغلاقها")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "المسبحة العائمة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر أثناء عرض المسبحة فوق شاشة الهاتف"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun setEnabledFlag(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun onDestroy() {
        rootView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootView = null
        countView = null
        cycleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.action.START_FLOATING_TASBEEH"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING_TASBEEH"
        const val CHANNEL_ID = "rim_floating_tasbeeh"
        const val NOTIFICATION_ID = 7052

        private const val PREFS_NAME = "rim_floating_tasbeeh"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_COUNT = "count"
        private const val KEY_CYCLES = "cycles"
        private const val KEY_TARGET = "target"

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
    }
}
