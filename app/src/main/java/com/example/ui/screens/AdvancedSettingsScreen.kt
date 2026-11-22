package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppBackgroundStore
import com.example.data.AthkarReminderScheduler
import com.example.service.FloatingTasbeehService
import com.example.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChange: (Float) -> Unit,
    notificationSound: Boolean,
    onToggleNotificationSound: (Boolean) -> Unit,
    fontSizeIndex: Int,
    onFontSizeChange: (Int) -> Unit,
    onSupportDeveloper: () -> Unit = {},
    onTelegram: () -> Unit = {},
    onBlog: () -> Unit = {},
    onPlatform: () -> Unit = {},
    pageColor: Long = 0L,
    fontColor: Long = 0L,
    onPageColorChange: (Long) -> Unit = {},
    onFontColorChange: (Long) -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والخيارات المتقدمة الحصرية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme & Display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("مظهر التطبيق", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الوضع الداكن (Night Mode)", fontSize = 15.sp)
                            Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
                        }
                    }
                }
            }

            // ألوان الصفحات وألوان الخطوط
            item {
                PageColorsCard(
                    pageColor = pageColor,
                    fontColor = fontColor,
                    onPageColorChange = onPageColorChange,
                    onFontColorChange = onFontColorChange
                )
            }

            // خلفية الصفحات من معرض الهاتف
            item {
                AppBackgroundCard()
            }

            // المسبحة العائمة فوق الشاشة
            item {
                FloatingTasbeehCard()
            }

            // Font Size Control
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("حجم الخط في شاشات القراءة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))

                        val fontSizes = listOf(
                            0 to "صغير (16sp)",
                            1 to "متوسط (18sp)",
                            2 to "كبير (22sp)"
                        )

                        fontSizes.forEach { (index, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFontSizeChange(index) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 15.sp)
                                RadioButton(
                                    selected = fontSizeIndex == index,
                                    onClick = { onFontSizeChange(index) }
                                )
                            }
                        }
                    }
                }
            }

            // AI Voice Robot Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("إعدادات روبوت الذكاء الاصطناعي الصوتي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("سرعة النطق: ${String.format("%.1f", speechRate)}x", fontSize = 14.sp)
                        Slider(
                            value = speechRate,
                            onValueChange = onSpeechRateChange,
                            valueRange = 0.5f..2.0f,
                            steps = 5
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("نبرة الصوت: ${String.format("%.1f", speechPitch)}", fontSize = 14.sp)
                        Slider(
                            value = speechPitch,
                            onValueChange = onSpeechPitchChange,
                            valueRange = 0.5f..1.5f,
                            steps = 3
                        )
                    }
                }
            }

            // Notifications & Sound
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("إشعارات الخلفية وشاشة القفل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صوت التنبيه مع الإشعارات", fontSize = 15.sp)
                            Switch(checked = notificationSound, onCheckedChange = onToggleNotificationSound)
                        }
                        val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        Text(
                            if (notificationsAllowed) "إذن الإشعارات: مفعّل" else "إذن الإشعارات: غير مفعّل",
                            fontSize = 12.sp,
                            color = if (notificationsAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            runCatching { context.startActivity(intent) }
                        }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("فتح إعدادات إشعارات التطبيق")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
                            val exactAllowed = alarmManager.canScheduleExactAlarms()
                            Text(
                                if (exactAllowed) "المنبّه الدقيق: مسموح" else "المنبّه الدقيق: غير مسموح؛ قد يتأخر التنبيه اليومي",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 12.sp,
                                color = if (exactAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                runCatching { context.startActivity(intent) }
                            }) {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("إعداد وصول المنبّهات الدقيقة")
                            }
                        }
                    }
                }
            }

            // Automatic Azkar Notifications + Manual Time
            item {
                AzkarNotificationsCard()
            }

            // صفحات المطور — منقولة من تطبيق ريمو بلاير
            item {
                DeveloperPagesCard(
                    onSupportDeveloper = onSupportDeveloper,
                    onTelegram = onTelegram,
                    onBlog = onBlog,
                    onPlatform = onPlatform
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* ------------------------------------------------------------------ */
/*  خلفية الصفحات — لكل صفحة خلفية مستقلة                             */
/* ------------------------------------------------------------------ */

@Composable
private fun AppBackgroundCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val revision = AppBackgroundStore.revision
    var pendingTargets by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var scrim by remember(revision) { mutableIntStateOf(AppBackgroundStore.scrimPercent(context)) }

    val selectedPages = remember(revision) {
        AppBackgroundStore.pages.map { it.key }
            .filter { AppBackgroundStore.isPageSelected(context, it) }
            .toSet()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val targets = pendingTargets
        pendingTargets = emptyList()
        if (uri != null && targets.isNotEmpty()) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    targets.all { key -> AppBackgroundStore.save(context, key, uri) }
                }
                message = if (ok) {
                    if (targets.size == 1) "تم تعيين الصورة خلفيةً لصفحة «${AppBackgroundStore.titleFor(targets.first())}»"
                    else "تم تعيين الصورة خلفيةً لـ ${targets.size} صفحات"
                } else {
                    "تعذّر قراءة الصورة، جرّب صورة أخرى"
                }
            }
        }
    }

    fun chooseFor(keys: List<String>) {
        if (keys.isEmpty()) {
            message = "حدّد صفحة واحدة على الأقل بوضع علامة ✔ في الخانة الصغيرة أولًا"
            return
        }
        pendingTargets = keys
        picker.launch("image/*")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "خلفية الصفحات",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "ضع علامة ✔ في الخانة الصغيرة أمام الصفحة أو الصفحات التي تريد تغيير خلفيتها، ثم عيّن صورة من معرض الهاتف. لكل صفحة خلفيتها المستقلة، ويمكنك تغيير خلفية أي صفحة أو إزالتها دون غيرها.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { chooseFor(selectedPages.toList()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selectedPages.isEmpty()) "تعيين صورة للصفحات المحددة"
                        else "تعيين صورة لـ ${selectedPages.size} صفحة",
                        fontSize = 13.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    AppBackgroundStore.pages.forEach { page ->
                        AppBackgroundStore.setPageEnabled(context, page.key, true)
                    }
                }) { Text("تحديد الكل", fontSize = 12.sp) }
                TextButton(onClick = {
                    AppBackgroundStore.pages.forEach { page ->
                        AppBackgroundStore.setPageEnabled(context, page.key, false)
                    }
                }) { Text("إلغاء التحديد", fontSize = 12.sp) }
                TextButton(onClick = {
                    AppBackgroundStore.clearAll(context)
                    message = "تمت إزالة خلفيات جميع الصفحات"
                }) { Text("إزالة الكل", fontSize = 12.sp) }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            AppBackgroundStore.pages.forEach { page ->
                val hasImage = AppBackgroundStore.hasImage(context, page.key)
                val checked = page.key in selectedPages
                PageBackgroundRow(
                    title = page.title,
                    hasImage = hasImage,
                    checked = checked,
                    imagePath = if (hasImage) AppBackgroundStore.imagePath(context, page.key) else null,
                    onToggle = { value ->
                        if (value && !hasImage) {
                            chooseFor(listOf(page.key))
                        } else {
                            AppBackgroundStore.setPageEnabled(context, page.key, value)
                        }
                    },
                    onPick = { chooseFor(listOf(page.key)) },
                    onClear = {
                        AppBackgroundStore.clearPage(context, page.key)
                        message = "تمت إزالة خلفية «${page.title}»"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            Text("درجة التعتيم فوق الخلفية: $scrim%", fontSize = 13.sp)
            Slider(
                value = scrim.toFloat(),
                onValueChange = { value ->
                    scrim = value.toInt()
                    AppBackgroundStore.setScrimPercent(context, scrim)
                },
                valueRange = 0f..85f,
                steps = 16
            )
            Text(
                "كلما زادت درجة التعتيم صار النص أوضح فوق الصورة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PageBackgroundRow(
    title: String,
    hasImage: Boolean,
    checked: Boolean,
    imagePath: String?,
    onToggle: (Boolean) -> Unit,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(
                if (hasImage) (if (checked) "خلفية مفعّلة" else "صورة محفوظة · غير مفعّلة") else "بدون صورة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (imagePath != null) {
            val thumb = remember(imagePath) { loadThumbnail(imagePath) }
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(4.dp))
            }
        }
        IconButton(onClick = onPick, modifier = Modifier.size(38.dp)) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = "تعيين صورة",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (hasImage) {
            IconButton(onClick = onClear, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "إزالة الخلفية",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun loadThumbnail(path: String): ImageBitmap? {
    return runCatching {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 160 || bounds.outHeight / sample > 160) sample *= 2
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        android.graphics.BitmapFactory.decodeFile(path, options)?.asImageBitmap()
    }.getOrNull()
}

/* ------------------------------------------------------------------ */
/*  ألوان الصفحات وألوان الخطوط                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun PageColorsCard(
    pageColor: Long,
    fontColor: Long,
    onPageColorChange: (Long) -> Unit,
    onFontColorChange: (Long) -> Unit
) {
    val pageSwatches = listOf(
        0L to "افتراضي",
        0xFF0B1F17L to "أخضر داكن",
        0xFF101A2BL to "أزرق ليلي",
        0xFF1B1226L to "بنفسجي",
        0xFF231A12L to "بنّي دافئ",
        0xFF0F0F0FL to "أسود",
        0xFFF6F1E4L to "عاجي",
        0xFFEAF3ECL to "أخضر فاتح",
        0xFFFFFFFFL to "أبيض"
    )
    val fontSwatches = listOf(
        0L to "افتراضي",
        0xFFE5C158L to "ذهبي",
        0xFFFFFFFFL to "أبيض",
        0xFFF1E9D2L to "عاجي",
        0xFF9FD8B4L to "أخضر فاتح",
        0xFFBFD7FFL to "أزرق فاتح",
        0xFF101010L to "أسود"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "ألوان الصفحات وألوان الخطوط",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "اختر لون خلفية الصفحات ولون الخط. تُطبَّق الألوان فورًا على كل صفحات التطبيق، ويمكنك العودة للون الافتراضي في أي وقت.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(14.dp))
            Text("لون خلفية الصفحات", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ColorSwatchRow(
                swatches = pageSwatches,
                selected = pageColor,
                onSelect = onPageColorChange
            )

            Spacer(Modifier.height(16.dp))
            Text("لون الخط", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ColorSwatchRow(
                swatches = fontSwatches,
                selected = fontColor,
                onSelect = onFontColorChange
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    onPageColorChange(0L)
                    onFontColorChange(0L)
                }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("استعادة الألوان الافتراضية")
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        swatches.forEach { (value, label) ->
            val isSelected = value == selected
            val color = if (value == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(value)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onSelect(value) }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  المسبحة العائمة فوق شاشة الهاتف                                    */
/* ------------------------------------------------------------------ */

@Composable
private fun FloatingTasbeehCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(FloatingTasbeehService.isEnabled(context)) }
    var overlayGranted by remember { mutableStateOf(FloatingTasbeehService.canDrawOverlays(context)) }

    fun start() {
        val intent = Intent(context, FloatingTasbeehService::class.java)
            .setAction(FloatingTasbeehService.ACTION_START)
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    fun stop() {
        val intent = Intent(context, FloatingTasbeehService::class.java)
            .setAction(FloatingTasbeehService.ACTION_STOP)
        runCatching { context.startService(intent) }
        runCatching { context.stopService(Intent(context, FloatingTasbeehService::class.java)) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "المسبحة العائمة فوق الشاشة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "مسبحة تظهر فوق شاشة الهاتف حتى وأنت خارج التطبيق. انقر على البطاقة للتسبيح، اسحبها لأي مكان، وتهتزّ عند إكمال العدد. لإخفائها اضغط زر ✕.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تشغيل المسبحة العائمة", fontSize = 15.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = { value ->
                        if (value) {
                            overlayGranted = FloatingTasbeehService.canDrawOverlays(context)
                            if (!overlayGranted) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            } else {
                                FloatingTasbeehService.setEnabled(context, true)
                                start()
                                enabled = true
                            }
                        } else {
                            FloatingTasbeehService.setEnabled(context, false)
                            stop()
                            enabled = false
                        }
                    }
                )
            }

            if (!overlayGranted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "إذن «الظهور فوق التطبيقات» غير ممنوح، وهو مطلوب لعرض المسبحة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }) {
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("منح إذن الظهور فوق التطبيقات")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("استثناء التطبيق من تحسين البطارية")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "الاستثناء يمنع النظام من إيقاف المسبحة أو الأذكار الصوتية عند قفل الشاشة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  صفحات المطور                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun DeveloperPagesCard(
    onSupportDeveloper: () -> Unit,
    onTelegram: () -> Unit,
    onBlog: () -> Unit,
    onPlatform: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "صفحات المطور",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "محمد الحزمي — دعم المطور وقناته ومدونته ومنصته",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))

            DeveloperPageRow(
                icon = Icons.Default.Favorite,
                title = "دعم المطور",
                subtitle = "محمد الحزمي · محفظة جيب",
                onClick = onSupportDeveloper
            )
            DeveloperPageRow(
                icon = Icons.Default.Verified,
                title = "قناة المطور على تليجرام",
                subtitle = "t.me/moh_alymani1",
                onClick = onTelegram
            )
            DeveloperPageRow(
                icon = Icons.Default.Article,
                title = "مدونة المطور",
                subtitle = "mohammedalhzmi.blogspot.com",
                onClick = onBlog
            )
            DeveloperPageRow(
                icon = Icons.Default.AutoAwesome,
                title = "منصة المطور محمد الحزمي",
                subtitle = "منصة الذكاء الاصطناعي",
                onClick = onPlatform
            )
        }
    }
}

@Composable
private fun DeveloperPageRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun AzkarNotificationsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val enabled by prefs.autoRemindersFlow.collectAsState(initial = true)
    val interval by prefs.reminderIntervalFlow.collectAsState(initial = 60)
    val time by prefs.azkarTimeFlow.collectAsState(initial = 6 to 30)

    var hourText by remember { mutableStateOf(time.first.toString()) }
    var minuteText by remember { mutableStateOf(time.second.toString()) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(time) {
        hourText = time.first.toString()
        minuteText = time.second.toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "إشعارات الأذكار التلقائية",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "يعمل التطبيق على تذكيرك بالأذكار تلقائيًا في الوقت الذي تحدده، حتى لو كان مغلقًا.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تفعيل الإشعارات الصوتية", fontSize = 15.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            prefs.setAutoReminders(checked)
                            AthkarReminderScheduler.sync(
                                context, checked, interval, time.first, time.second
                            )
                            message = if (checked) "تم تفعيل الإشعارات التلقائية" else "تم إيقاف الإشعارات التلقائية"
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("التكرار كل (دقيقة)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120).forEach { value ->
                    FilterChip(
                        selected = interval == value,
                        onClick = {
                            scope.launch {
                                prefs.setReminderInterval(value)
                                AthkarReminderScheduler.sync(
                                    context, enabled, value, time.first, time.second
                                )
                                message = "تم ضبط التكرار كل $value دقيقة"
                            }
                        },
                        label = { Text("$value", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("وقت التشغيل اليومي (إدخال يدوي)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                    label = { Text("الساعة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text(" : ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it.filter(Char::isDigit).take(2) },
                    label = { Text("الدقيقة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val h = hourText.toIntOrNull()
                        val m = minuteText.toIntOrNull()
                        if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                            message = "أدخل وقتًا صحيحًا: الساعة 0-23 والدقيقة 0-59"
                        } else {
                            scope.launch {
                                prefs.setAzkarTime(h, m)
                                prefs.setAutoReminders(true)
                                AthkarReminderScheduler.sync(context, true, interval, h, m)
                                message = "تم حفظ الوقت ${String.format("%02d:%02d", h, m)} وتفعيل الإشعارات"
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ وتفعيل")
                }
                OutlinedButton(
                    onClick = {
                        AthkarReminderScheduler.runOnceNow(context)
                        message = "تم إرسال إشعار تجريبي بعد لحظات"
                    }
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إشعار تجريبي")
                }
            }
            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
