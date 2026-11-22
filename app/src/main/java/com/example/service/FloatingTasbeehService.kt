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
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlin.math.abs

/**
 * مسبحة عائمة فوق شاشة الهاتف تعمل خارج التطبيق.
 *
 * نافذة نظام حقيقية (SYSTEM_ALERT_WINDOW) عبر خدمة أمامية، قابلة للسحب بإصبع واحد،
 * وقابلة للتكبير والتصغير بالسحب بإصبعين (Pinch-to-Zoom) مع حفظ الحجم المختار،
 * تُحتسب بالنقر على البطاقة، وتهتزّ عند إكمال العدد، وتُغلق بزر ✕.
 */
class FloatingTasbeehService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var cardView: LinearLayout? = null
    private var titleView: TextView? = null
    private var countView: TextView? = null
    private var cycleView: TextView? = null
    private var resetView: TextView? = null
    private var closeView: TextView? = null

    /** الحجم الحالي (1.0 = الحجم الافتراضي). */
    private var currentScale = 1f

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

        currentScale = prefs.getFloat(KEY_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        cardView = card

        // شريط العنوان مع زر الإغلاق ✕
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "مسبحة"
            setTextColor(Color.parseColor("#E8F0EC"))
            setPadding(0, 0, dp(10), 0)
        }
        titleView = title
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF8A80"))
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener {
                setEnabledFlag(false)
                stopSelf()
            }
        }
        closeView = close
        header.addView(title)
        header.addView(close)

        val count = TextView(this).apply {
            text = prefs.getInt(KEY_COUNT, 0).toString()
            setTextColor(Color.parseColor("#E5C158"))
            gravity = Gravity.CENTER
        }
        countView = count

        val cycle = TextView(this).apply {
            text = "الدورات: ${prefs.getInt(KEY_CYCLES, 0)} / ${prefs.getInt(KEY_TARGET, 33)}"
            setTextColor(Color.parseColor("#9EABA3"))
            gravity = Gravity.CENTER
        }
        cycleView = cycle

        val reset = TextView(this).apply {
            text = "تصفير"
            setTextColor(Color.parseColor("#9EABA3"))
            setOnClickListener {
                prefs.edit().putInt(KEY_COUNT, 0).apply()
                count.text = "0"
            }
        }
        resetView = reset

        card.addView(header)
        card.addView(count)
        card.addView(cycle)
        card.addView(reset)

        // تطبيق المقاسات حسب الحجم المختار (يُستخدم عند البدء وعند تغيير الحجم بالسحب).
        val applySizes: (Float) -> Unit = { scale ->
            val s = scale.coerceIn(MIN_SCALE, MAX_SCALE)
            title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f * s)
            close.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f * s)
            count.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 34f * s)
            cycle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f * s)
            reset.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f * s)
            card.setPadding(
                dp((14 * s).toInt()),
                dp((10 * s).toInt()),
                dp((14 * s).toInt()),
                dp((12 * s).toInt())
            )
            reset.setPadding(
                dp((8 * s).toInt()),
                dp((6 * s).toInt()),
                dp((8 * s).toInt()),
                dp((2 * s).toInt())
            )
            title.setPadding(0, 0, dp((10 * s).toInt()), 0)
            close.setPadding(
                dp((8 * s).toInt()), 0, dp((4 * s).toInt()), 0
            )
            count.setPadding(0, dp((2 * s).toInt()), 0, 0)
            card.background = GradientDrawable().apply {
                cornerRadius = dp((22 * s).toInt()).toFloat()
                setColor(Color.parseColor("#E6101A15"))
                setStroke(dp((2 * s).coerceAtLeast(1f).toInt()), Color.parseColor("#4E9A78"))
            }
            card.requestLayout()
        }
        applySizes(currentScale)

        // --- التكبير والتصغير بالسحب بإصبعين ---
        var previewScale = currentScale
        val scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    previewScale = currentScale
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    previewScale = (previewScale * detector.scaleFactor)
                        .coerceIn(MIN_SCALE, MAX_SCALE)
                    // معاينة فورية أثناء السحب
                    val relative = previewScale / currentScale
                    card.scaleX = relative
                    card.scaleY = relative
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    currentScale = previewScale.coerceIn(MIN_SCALE, MAX_SCALE)
                    card.scaleX = 1f
                    card.scaleY = 1f
                    applySizes(currentScale)
                    prefs.edit().putFloat(KEY_SCALE, currentScale).apply()
                }
            }
        )

        // --- السحب بإصبع واحد + النقر للعدّ ---
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        var downAt = 0L

        card.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params?.x ?: 0
                    startY = params?.y ?: 0
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    downAt = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // إصبع ثانٍ = تكبير/تصغير، وليس نقرة عدّ
                    moved = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount > 1) {
                        moved = true
                        return@setOnTouchListener true
                    }
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                    if (moved) {
                        val p = params ?: return@setOnTouchListener false
                        p.x = startX + dx.toInt()
                        p.y = startY + dy.toInt()
                        runCatching { windowManager?.updateViewLayout(card, p) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val quickTap = System.currentTimeMillis() - downAt < 700
                    if (!moved && quickTap && event.pointerCount <= 1) {
                        increment()
                    }
                    moved = false
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    moved = false
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
            x = prefs.getInt(KEY_X, dp(12))
            y = prefs.getInt(KEY_Y, dp(160))
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

    private fun vibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    private fun vibrate() {
        runCatching {
            val v = vibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(120)
            }
        }
    }

    private fun smallTick() {
        runCatching {
            val v = vibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(18, 40))
            } else {
                @Suppress("DEPRECATION") v.vibrate(18)
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
            .setContentText("اضغط للعدّ · اسحب بإصبعين للتكبير والتصغير · ✕ للإغلاق")
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
        // حفظ موضع المسبحة الأخير لتعود في نفس المكان.
        params?.let { p ->
            prefs.edit().putInt(KEY_X, p.x).putInt(KEY_Y, p.y).apply()
        }
        rootView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootView = null
        cardView = null
        titleView = null
        countView = null
        cycleView = null
        resetView = null
        closeView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.action.START_FLOATING_TASBEEH"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING_TASBEEH"
        const val CHANNEL_ID = "rim_floating_tasbeeh"
        const val NOTIFICATION_ID = 7052

        /** حدود التكبير والتصغير بالسحب بالإصبعين. */
        const val MIN_SCALE = 0.6f
        const val MAX_SCALE = 2.4f

        private const val PREFS_NAME = "rim_floating_tasbeeh"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_COUNT = "count"
        private const val KEY_CYCLES = "cycles"
        private const val KEY_TARGET = "target"
        private const val KEY_SCALE = "scale"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"

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
