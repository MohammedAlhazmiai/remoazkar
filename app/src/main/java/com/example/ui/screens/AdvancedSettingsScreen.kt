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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

            // خلفية التطبيق من معرض الهاتف
            item {
                AppBackgroundCard()
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
/*  خلفية التطبيق                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun AppBackgroundCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(AppBackgroundStore.revision) }
    var enabled by remember { mutableStateOf(AppBackgroundStore.isEnabled(context)) }
    var hasImage by remember { mutableStateOf(AppBackgroundStore.imagePath(context) != null) }
    var scrim by remember { mutableIntStateOf(AppBackgroundStore.scrimPercent(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { AppBackgroundStore.save(context, uri) }
                revision = AppBackgroundStore.revision
                hasImage = AppBackgroundStore.imagePath(context) != null
                enabled = AppBackgroundStore.isEnabled(context)
                message = if (ok) "تم تعيين الصورة خلفيةً لصفحات التطبيق" else "تعذّر قراءة الصورة، جرّب صورة أخرى"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "خلفية التطبيق",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "اختر صورة من معرض الهاتف لتظهر خلف جميع صفحات التطبيق، ويمكنك تغييرها أو إزالتها في أي وقت.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))

            if (hasImage) {
                val path = remember(revision) { AppBackgroundStore.imagePath(context) }
                val preview = remember(path) {
                    runCatching {
                        android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap()
                    }.getOrNull()
                }
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = "معاينة الخلفية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل الخلفية", fontSize = 15.sp)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { value ->
                            AppBackgroundStore.setEnabled(context, value)
                            enabled = value
                            revision = AppBackgroundStore.revision
                        }
                    )
                }

                Text("درجة التعتيم: $scrim%", fontSize = 13.sp)
                Slider(
                    value = scrim.toFloat(),
                    onValueChange = { value ->
                        scrim = value.toInt()
                        AppBackgroundStore.setScrimPercent(context, scrim)
                    },
                    valueRange = 0f..85f,
                    steps = 16
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { picker.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasImage) "تغيير الصورة" else "رفع صورة من الهاتف")
                }
                if (hasImage) {
                    OutlinedButton(onClick = {
                        AppBackgroundStore.clear(context)
                        revision = AppBackgroundStore.revision
                        hasImage = false
                        enabled = false
                        message = "تمت إزالة الخلفية"
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("إزالة")
                    }
                }
            }

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
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
