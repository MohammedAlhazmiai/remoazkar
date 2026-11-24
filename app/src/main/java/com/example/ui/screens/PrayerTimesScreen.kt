package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AsrMadhab
import com.example.data.HijriCalendar
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.data.PrayerAlarmScheduler
import com.example.data.PrayerCalculationMethod
import com.example.data.PrayerStore
import com.example.data.PrayerType
import com.example.data.ResolvedPrayerTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val rev = PrayerStore.revision

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val locationName = remember(rev) { PrayerStore.locationName(context) }
    val daySchedule = remember(rev, nowMillis / 60_000L) { PrayerStore.resolveDayPrayers(context) }
    val sunrise = daySchedule.first
    val prayers = daySchedule.second
    val nextPrayer = remember(rev, nowMillis / 1000L) {
        PrayerStore.nextPrayer(context, nowMillis)
    }

    var notifEnabled by remember(rev) { mutableStateOf(PrayerStore.isNotificationsEnabled(context)) }
    var adhanSoundEnabled by remember(rev) { mutableStateOf(PrayerStore.isAdhanSoundEnabled(context)) }
    var leadMinutes by remember(rev) { mutableIntStateOf(PrayerStore.leadMinutes(context)) }
    var selectedMethod by remember(rev) { mutableStateOf(PrayerStore.method(context)) }
    var selectedMadhab by remember(rev) { mutableStateOf(PrayerStore.madhab(context)) }
    var hasNotifPerm by remember { mutableStateOf(PrayerAlarmScheduler.hasNotificationPermission(context)) }
    var canScheduleExact by remember { mutableStateOf(PrayerAlarmScheduler.canScheduleExactAlarms(context)) }
    var ignoringBattery by remember { mutableStateOf(HumanAzkarScheduler.isIgnoringBatteryOptimizations(context)) }
    var isLocating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var editingPrayer by remember { mutableStateOf<ResolvedPrayerTime?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, notifEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPerm = PrayerAlarmScheduler.hasNotificationPermission(context)
                canScheduleExact = PrayerAlarmScheduler.canScheduleExactAlarms(context)
                ignoringBattery = HumanAzkarScheduler.isIgnoringBatteryOptimizations(context)
                if (notifEnabled) {
                    PrayerAlarmScheduler.scheduleAll(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(notifEnabled) {
        if (notifEnabled) {
            PrayerAlarmScheduler.scheduleAll(context)
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted || PrayerAlarmScheduler.hasNotificationPermission(context)
        if (granted) {
            PrayerAlarmScheduler.scheduleAll(context)
            statusMessage = "تم تفعيل إذن الإشعارات وجدولة مواقيت الصلاة بنجاح."
        } else {
            statusMessage = "يرجى السماح بإشعارات التطبيق من الإعدادات لتظهر تنبيهات الصلاة."
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            PrayerStore.refreshLocationFromDevice(context) { _, msg ->
                isLocating = false
                statusMessage = msg
            }
        } else {
            statusMessage = "تم رفض إذن الموقع؛ يمكنك اختيار مدينتك يدويًا."
        }
    }

    fun requestGpsUpdate() {
        if (PrayerStore.hasLocationPermission(context)) {
            isLocating = true
            PrayerStore.refreshLocationFromDevice(context) { _, msg ->
                isLocating = false
                statusMessage = msg
            }
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("مواقيت الصلوات الخمس", fontWeight = FontWeight.Bold)
                        Text(
                            HijriCalendar.today().formatted,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // بطاقة الصلاة القادمة والعدّاد التنازلي
            item {
                NextPrayerHeroBanner(
                    locationName = locationName,
                    nextPrayer = nextPrayer?.prayer,
                    remainingMillis = ((nextPrayer?.triggerMillis ?: nowMillis) - nowMillis).coerceAtLeast(0L),
                    sunrise = sunrise
                )
            }

            // بطاقة الموقع الجغرافي
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("الموقع الجغرافي الحالي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    locationName,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { requestGpsUpdate() },
                                enabled = !isLocating,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("update_prayer_gps_button")
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(if (isLocating) "جارٍ التحديد..." else "تحديث بالموقع")
                            }
                            OutlinedButton(
                                onClick = { showCityPicker = true },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تحديد يدوي")
                            }
                        }
                        statusMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // قائمة الصلوات الخمس
            item {
                Text(
                    "مواقيت اليوم (اضغط تعديل لضبط أي صلاة يدويًا)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(prayers, key = { it.type.id }) { item ->
                val isNext = nextPrayer?.prayer?.type == item.type
                PrayerRowCard(
                    item = item,
                    isNext = isNext,
                    onToggleEnabled = { enabled ->
                        PrayerStore.setPrayerEnabled(context, item.type, enabled)
                    },
                    onEditClick = { editingPrayer = item }
                )
            }

            // إعدادات التنبيهات وطريقة الحساب
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "إعدادات تنبيهات الصلوات وطريقة الحساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل تنبيهات الصلوات تلقائيًا", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = notifEnabled,
                                onCheckedChange = { checked ->
                                    notifEnabled = checked
                                    PrayerStore.setNotificationsEnabled(context, checked)
                                    if (checked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        statusMessage = "تم تفعيل وجدولة تنبيهات الصلوات الخمس."
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("صوت بداية الأذان عند التذكير", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "ينطق «الله أكبر، الله أكبر» مرة واحدة عند حلول التذكير فقط",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = adhanSoundEnabled,
                                onCheckedChange = { checked ->
                                    adhanSoundEnabled = checked
                                    PrayerStore.setAdhanSoundEnabled(context, checked)
                                }
                            )
                        }

                        if (notifEnabled && (!hasNotifPerm || !canScheduleExact || !ignoringBattery)) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                        Text(
                                            "• إذن الإشعارات غير مفعّل؛ اضغط لتفعيله الآن.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        OutlinedButton(
                                            onClick = { notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("منح إذن الإشعارات") }
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                                        Text(
                                            "• إذن المنبهات الدقيقة غير مفعّل؛ قد يتأخر الأذان في وضع السكون.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                            data = Uri.fromParts("package", context.packageName, null)
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("تفعيل المنبهات الدقيقة للصلاة") }
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !ignoringBattery) {
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(
                                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                            Uri.fromParts("package", context.packageName, null)
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("استثناء من تحسين البطارية (لقفل الشاشة)") }
                                    }
                                }
                            }
                        } else if (notifEnabled) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "التنبيهات مفعّلة فعليًا وجاهزة للعمل في الخلفية وقفل الشاشة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("وقت التنبيه", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "عند دخول الوقت",
                                5 to "قبل 5 دقائق",
                                10 to "قبل 10 دقائق",
                                15 to "قبل 15 دقيقة"
                            ).forEach { (mins, label) ->
                                FilterChip(
                                    selected = leadMinutes == mins,
                                    onClick = {
                                        leadMinutes = mins
                                        PrayerStore.setLeadMinutes(context, mins)
                                    },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("طريقة الحساب الفلكي", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrayerCalculationMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = selectedMethod == method,
                                    onClick = {
                                        selectedMethod = method
                                        PrayerStore.setMethod(context, method)
                                    },
                                    label = { Text(method.arabicTitle, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("مذهب صلاة العصر", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsrMadhab.entries.forEach { madhab ->
                                FilterChip(
                                    selected = selectedMadhab == madhab,
                                    onClick = {
                                        selectedMadhab = madhab
                                        PrayerStore.setMadhab(context, madhab)
                                    },
                                    label = { Text(madhab.arabicTitle, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PrayerAlarmScheduler.scheduleAll(context)
                                    NotificationHelper.triggerPrayerReminderAlert(
                                        context = context,
                                        id = 5199,
                                        title = "تنبيه تجريبي لمواعيد الصلاة",
                                        body = "بداية الأذان: الله أكبر، الله أكبر — تنبيهات الصلوات الخمس مفعّلة وتعمل بنجاح.",
                                        playAdhanStart = adhanSoundEnabled
                                    )
                                    statusMessage = if (adhanSoundEnabled) {
                                        "تم تشغيل التنبيه التجريبي مع صوت بداية الأذان"
                                    } else {
                                        "تم إرسال إشعار تجريبي صامت لمواعيد الصلاة"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تجربة التنبيه")
                            }
                            OutlinedButton(
                                onClick = {
                                    PrayerStore.clearAllManualOverrides(context)
                                    PrayerAlarmScheduler.scheduleAll(context)
                                    statusMessage = "تمت استعادة جميع المواقيت للحساب التلقائي"
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تلقائي")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    editingPrayer?.let { prayer ->
        ManualPrayerEditDialog(
            prayer = prayer,
            onDismiss = { editingPrayer = null },
            onSave = { h, m ->
                PrayerStore.setManualTime(context, prayer.type, h, m)
                editingPrayer = null
                statusMessage = "تم حفظ وقت صلاة ${prayer.type.arabicName} يدويًا (${String.format("%02d:%02d", h, m)})"
            },
            onResetAuto = {
                PrayerStore.setManualTime(context, prayer.type, null, null)
                PrayerStore.setOffsetMinutes(context, prayer.type, 0)
                editingPrayer = null
                statusMessage = "تمت إعادة صلاة ${prayer.type.arabicName} إلى الحساب التلقائي"
            }
        )
    }

    if (showCityPicker) {
        CityLocationPickerDialog(
            onDismiss = { showCityPicker = false },
            onSelectCity = { name, lat, lon ->
                PrayerStore.setLocation(context, lat, lon, name, isAuto = false)
                showCityPicker = false
                statusMessage = "تم ضبط الموقع على: $name"
            }
        )
    }
}

@Composable
private fun NextPrayerHeroBanner(
    locationName: String,
    nextPrayer: ResolvedPrayerTime?,
    remainingMillis: Long,
    sunrise: Pair<Int, Int>
) {
    val totalSeconds = remainingMillis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val sunriseH = if (sunrise.first == 0) 12 else if (sunrise.first > 12) sunrise.first - 12 else sunrise.first
    val sunriseLabel = String.format("%d:%02d ص", sunriseH, sunrise.second)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF144235),
                            Color(0xFF1D5D49),
                            Color(0xFF103127)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "الصلاة القادمة",
                        color = Color(0xFFFFD875),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "الشروق: $sunriseLabel",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            nextPrayer?.type?.arabicName ?: "—",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            nextPrayer?.formatted12() ?: "",
                            color = Color(0xFFDCECE4),
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("المتبقي", color = Color(0xFFFFE39C), fontSize = 11.sp)
                        Text(
                            countdown,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    locationName,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    item: ResolvedPrayerTime,
    isNext: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isNext) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.type) {
                        PrayerType.FAJR -> Icons.Default.WbTwilight
                        PrayerType.DHUHR -> Icons.Default.WbSunny
                        PrayerType.ASR -> Icons.Default.LightMode
                        PrayerType.MAGHRIB -> Icons.Default.NightsStay
                        PrayerType.ISHA -> Icons.Default.Bedtime
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.type.arabicName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (item.isManualOverride) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                "يدوي",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    item.formatted12(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل وقت ${item.type.arabicName}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = onToggleEnabled
            )
        }
    }
}

@Composable
fun ManualPrayerEditDialog(
    prayer: ResolvedPrayerTime,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int) -> Unit,
    onResetAuto: () -> Unit
) {
    var hourText by remember(prayer) { mutableStateOf(prayer.hour.toString()) }
    var minuteText by remember(prayer) { mutableStateOf(prayer.minute.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل وقت صلاة ${prayer.type.arabicName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "أدخل الساعة (0 إلى 23) والدقيقة (0 إلى 59) يدويًا:",
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                        label = { Text("الساعة (24)") },
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
                errorText?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val h = hourText.toIntOrNull()
                val m = minuteText.toIntOrNull()
                if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                    errorText = "يرجى إدخال وقت صحيح: الساعة 0-23 والدقيقة 0-59"
                } else {
                    onSave(h, m)
                }
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            Row {
                if (prayer.isManualOverride) {
                    TextButton(onClick = onResetAuto) {
                        Text("تلقائي")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@Composable
fun CityLocationPickerDialog(
    onDismiss: () -> Unit,
    onSelectCity: (name: String, lat: Double, lon: Double) -> Unit
) {
    val context = LocalContext.current
    var customName by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf(PrayerStore.latitude(context).toString()) }
    var customLon by remember { mutableStateOf(PrayerStore.longitude(context).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديد الموقع يدويًا", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("اختر مدينة جاهزة:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(PrayerStore.cityPresets) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectCity(city.name, city.latitude, city.longitude) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(city.name, fontSize = 14.sp)
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
                Text("أو أدخل الإحداثيات يدويًا:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("اسم المنطقة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customLat,
                        onValueChange = { customLat = it },
                        label = { Text("خط العرض") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customLon,
                        onValueChange = { customLon = it },
                        label = { Text("خط الطول") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                errorMsg?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = customLat.trim().toDoubleOrNull()
                val lon = customLon.trim().toDoubleOrNull()
                if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    errorMsg = "أدخل إحداثيات صالحة (العرض -90..90 والطول -180..180)"
                } else {
                    val label = customName.trim().ifBlank {
                        String.format("%.2f°, %.2f°", lat, lon)
                    }
                    onSelectCity(label, lat, lon)
                }
            }) {
                Text("تطبيق الإحداثيات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
