package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisnRepository
import com.example.data.PreferencesManager
import com.example.data.TtsManager
import com.example.data.tr
import com.example.ui.theme.GoldAccent
import com.example.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * الميزات الحصرية — ثلاث ميزات مبتكرة لا توجد في تطبيقات الأذكار المألوفة:
 *
 *  1. بصمة النور: لوحة هندسية فريدة تُشتق من تاريخ اليوم ومجموع تسبيحك،
 *     فلكل يوم «بصمة» مختلفة تُحفظ ذكرياتك الرقمية عليها ويمكن مشاركتها.
 *  2. مسبحة الحركة: عدّاد تسبيح يعمل بهزّ الهاتف عبر حسّاس التسارع، بلا لمس الشاشة.
 *  3. الذكر الآن: محرّك يقترح الذكر المناسب لوقت اليوم تلقائيًا.
 */
@Composable
fun ExclusiveFeaturesScreen(onBackClick: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(tr("light_seal"), tr("motion_tasbeeh"), tr("adaptive_azkar"))

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScreenHeader(
                title = tr("exclusive"),
                subtitle = "ثلاث ميزات من ابتكار ريمواذكار",
                onBackClick = onBackClick
            )

            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            when (tab) {
                0 -> LightSealTab()
                1 -> MotionTasbeehTab()
                else -> AdaptiveDhikrTab()
            }
        }
    }
}

/* ================================================================== */
/*  1) بصمة النور                                                      */
/* ================================================================== */

@Composable
private fun LightSealTab() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var total by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        prefs.tasbeehTotalFlow.collect { value -> total = value }
    }

    val calendar = remember { Calendar.getInstance() }
    val dateKey = remember {
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
    val daySeed = remember { dateKey.toLongOrNull() ?: 0L }
    val seed = remember(total, daySeed) { daySeed * 31 + total }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "بصمة النور",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "لوحة فريدة تُشتق من تاريخ اليوم ومجموع تسبيحك — تتغيّر كل يوم، ولا تتكرر.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF10241C), Color(0xFF07110C))
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension / 2f

                // حلقة خارجية
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.55f),
                    radius = maxR * 0.96f,
                    style = Stroke(width = 1.6f)
                )
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.25f),
                    radius = maxR * 0.86f,
                    style = Stroke(width = 0.9f)
                )

                // مولّد أرقام شبه عشوائي ثابت (حتى تتكرر نفس البصمة لنفس اليوم والعدد)
                var state = if (seed == 0L) 7919L else abs(seed)
                fun next(): Float {
                    state = (state * 1103515245L + 12345L) and 0x7FFFFFFFL
                    return (state % 10000L) / 10000f
                }

                val petals = 8 + (abs(seed) % 6).toInt()
                val rings = 4 + (abs(seed / 7) % 4).toInt()

                for (ring in 0 until rings) {
                    val radius = maxR * (0.18f + 0.16f * ring)
                    val points = petals + ring * 2
                    var prev: Offset? = null
                    for (i in 0..points) {
                        val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat()
                        val wobble = 0.82f + next() * 0.32f
                        val r = radius * wobble
                        val p = Offset(
                            cx + r * kotlin.math.cos(angle),
                            cy + r * kotlin.math.sin(angle)
                        )
                        if (prev != null) {
                            drawLine(
                                color = if ((i + ring) % 3 == 0)
                                    GoldAccent.copy(alpha = 0.55f)
                                else Color(0xFF4E9A78).copy(alpha = 0.55f),
                                start = prev!!,
                                end = p,
                                strokeWidth = if ((i + ring) % 3 == 0) 1.8f else 1.0f
                            )
                        }
                        prev = p
                    }
                }

                // نقاط نور
                repeat(14 + (abs(seed) % 10).toInt()) {
                    val angle = next() * 2f * Math.PI.toFloat()
                    val r = maxR * (0.25f + next() * 0.68f)
                    drawCircle(
                        color = Color(0xFFF3E5AB).copy(alpha = 0.35f + next() * 0.5f),
                        radius = 1.4f + next() * 2.4f,
                        center = Offset(cx + r * kotlin.math.cos(angle), cy + r * kotlin.math.sin(angle))
                    )
                }

                // القلب
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.85f),
                    radius = maxR * 0.09f
                )
                drawCircle(
                    color = Color(0xFF0B1A13),
                    radius = maxR * 0.045f
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                InfoRow("تاريخ البصمة", dateKey)
                InfoRow("مجموع التسبيح", "$total")
                InfoRow("عدد البتلات", "${8 + (abs(seed) % 6).toInt()}")
                InfoRow("بصمة اليوم", Integer.toHexString(abs(seed).toInt()).uppercase())
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                val text = "بصمة النور الخاصة بي اليوم ${dateKey} — مجموع تسبيحي $total.\n" +
                    "بصمة: ${Integer.toHexString(abs(seed).toInt()).uppercase()}\n" +
                    "من تطبيق ريمواذكار: ${ShareUtil.APP_LINK}"
                ShareUtil.share(context, text, "بصمة النور")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("شارك بصمة نورك", fontWeight = FontWeight.Bold)
        }

        Text(
            "كل يوم تحصل على بصمة جديدة. اجعل تسبيحك اليومي يترك أثرًا.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(vertical = 14.dp)
        )
    }
}

/* ================================================================== */
/*  2) مسبحة الحركة                                                    */
/* ================================================================== */

@Composable
private fun MotionTasbeehTab() {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var active by remember { mutableStateOf(true) }
    var sensorAvailable by remember { mutableStateOf(true) }
    var lastPeak by remember { mutableLongStateOf(0L) }
    var peak by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.96f,
        animationSpec = tween(400),
        label = "motionScale"
    )

    DisposableEffect(active) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            sensorAvailable = false
            return@DisposableEffect onDispose { }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!active) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                // يبدأ من 9.8 تقريبًا عند السكون؛ الهزّة ترفعه فوق 15
                if (magnitude > 15f && magnitude > peak) {
                    peak = magnitude
                }
                if (peak > 15f && magnitude < 12f) {
                    val now = System.currentTimeMillis()
                    if (now - lastPeak > 320) {
                        lastPeak = now
                        count += 1
                    }
                    peak = 0f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "مسبحة الحركة",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "سبّح بهزّ الهاتف دون أن تلمس الشاشة — مفيدة أثناء المشي أو العمل.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )

        Box(
            modifier = Modifier
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1B4D3E), Color(0xFF0B1A13))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$count",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "تسبيحة",
                    fontSize = 13.sp,
                    color = Color(0xFFD9E7DF)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (!sensorAvailable) {
            Text(
                "هذا الجهاز لا يحتوي حسّاس تسارع، لذا لا تعمل مسبحة الحركة عليه. استخدم المسبحة العادية من الصفحة الرئيسية.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { active = !active },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        if (active) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (active) "إيقاف مؤقت" else "تشغيل")
                }
                OutlinedButton(
                    onClick = { count = 0 },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (active) "الهزّاز يعمل — حرّك هاتفك بهدوء لكل تسبيحة"
                else "متوقف — اضغط تشغيل للمتابعة",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "كل هزّة تُحتسب بعد عودة الهاتف للسكون، وبفاصل 320 مللي ثانية لمنع الاحتساب المزدوج.",
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/* ================================================================== */
/*  3) الذكر الآن                                                      */
/* ================================================================== */

private data class DhikrSuggestion(
    val period: String,
    val title: String,
    val body: String
)

private fun suggestionsForHour(hour: Int): DhikrSuggestion = when (hour) {
    in 4..8 -> DhikrSuggestion(
        "الفجر والصباح",
        "أذكار الصباح",
        "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ."
    )
    in 9..15 -> DhikrSuggestion(
        "الضحى والظهر",
        "الصلاة على النبي",
        "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ، وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ، عَدَدَ خَلْقِكَ وَرِضَا نَفْسِكَ وَزِنَةَ عَرْشِكَ وَمِدَادَ كَلِمَاتِكَ."
    )
    in 16..19 -> DhikrSuggestion(
        "العصر والمغرب",
        "أذكار المساء",
        "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ."
    )
    else -> DhikrSuggestion(
        "الليل والنوم",
        "أذكار النوم",
        "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا. اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَجْمَعُ عِبَادَكَ."
    )
}

@Composable
private fun AdaptiveDhikrTab() {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val hour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val suggestion = remember(hour) { suggestionsForHour(hour) }
    var done by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "الذكر الآن",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "يقترح عليك الذكر المناسب لوقت يومك تلقائيًا، دون أن تبحث.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10241C))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(GoldAccent.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            suggestion.period,
                            color = Color(0xFF9EABA3),
                            fontSize = 11.sp
                        )
                        Text(
                            suggestion.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "%02d:00".format(hour),
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    suggestion.body,
                    color = Color(0xFFE8F0EC),
                    fontSize = 16.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (TtsManager.isSpeaking) TtsManager.stop() else {
                        TtsManager.speak(suggestion.body)
                        started = true
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (TtsManager.isSpeaking) tr("stop_reading") else tr("read"))
            }
            OutlinedButton(
                onClick = { done += 1 },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("أتممت الذكر ($done)")
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "كيف يعمل؟",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "يقرأ التطبيق ساعة جهازك ويختار من أربع فترات: الفجر والصباح (4–8)،" +
                        " الضحى والظهر (9–15)، العصر والمغرب (16–19)، والليل والنوم (20–3)." +
                        " يمكنك دائمًا قراءة الذكر بالصوت أو الانتقال إلى قسم الأذكار الكامل.",
                    fontSize = 11.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                if (started) {
                    Text(
                        "تم تشغيل القراءة الصوتية لهذا الذكر.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
