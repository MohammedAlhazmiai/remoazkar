package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppBackgroundStore
import com.example.data.AthkarReminderScheduler
import com.example.data.PrayerAlarmScheduler
import com.example.data.PrayerCalculationMethod
import com.example.data.PrayerStore
import com.example.data.PreferencesManager
import com.example.data.ResolvedPrayerTime
import com.example.service.FloatingTasbeehService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsCategory(
    val title: String,
    val icon: ImageVector
) {
    ALL("الكل", Icons.Default.GridView),
    APPEARANCE("المظهر والخلفيات", Icons.Default.Palette),
    READING_AUDIO("القراءة والصوت", Icons.Default.RecordVoiceOver),
    NOTIFICATIONS("المسبحة والتنبيهات", Icons.Default.NotificationsActive),
    DEVELOPER("المطور", Icons.Default.Verified)
}

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
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.ALL) }
    val bgRevision = AppBackgroundStore.revision
    val islamicBgEnabled = remember(bgRevision) {
        AppBackgroundStore.isIslamicBackgroundsEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الإعدادات المتقدمة", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(
                            "تخصيص المظهر والخلفيات والصوت والتنبيهات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة ملخّص الحالة الحالية
            item {
                SettingsOverviewBanner(
                    darkMode = darkMode,
                    fontSizeIndex = fontSizeIndex,
                    speechRate = speechRate,
                    islamicBgEnabled = islamicBgEnabled
                )
            }

            // شريط تصفية الأقسام السريع
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    category.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // 1. قسم المظهر والخلفيات والألوان
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.APPEARANCE) {
                item {
                    SettingsSectionHeader(
                        title = "المظهر والخلفيات والألوان",
                        subtitle = "الوضع الداكن، الخلفيات الإسلامية الدينية، وألوان الصفحات والخطوط",
                        icon = Icons.Default.Palette
                    )
                }

                item {
                    ThemeAppearanceCard(
                        darkMode = darkMode,
                        onToggleDarkMode = onToggleDarkMode
                    )
                }

                item {
                    AppBackgroundCard()
                }

                item {
                    PageColorsCard(
                        pageColor = pageColor,
                        fontColor = fontColor,
                        onPageColorChange = onPageColorChange,
                        onFontColorChange = onFontColorChange
                    )
                }
            }

            // 2. قسم القراءة والقارئ الذكي
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.READING_AUDIO) {
                item {
                    SettingsSectionHeader(
                        title = "القراءة والقارئ الذكي",
                        subtitle = "مقياس الخط في المصحف والأذكار وضبط سرعة ونبرة النطق",
                        icon = Icons.Default.RecordVoiceOver
                    )
                }

                item {
                    FontSizeSettingsCard(
                        fontSizeIndex = fontSizeIndex,
                        onFontSizeChange = onFontSizeChange
                    )
                }

                item {
                    AiVoiceSettingsCard(
                        speechRate = speechRate,
                        onSpeechRateChange = onSpeechRateChange,
                        speechPitch = speechPitch,
                        onSpeechPitchChange = onSpeechPitchChange
                    )
                }
            }

            // 3. قسم المسبحة العائمة والإشعارات
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.NOTIFICATIONS) {
                item {
                    SettingsSectionHeader(
                        title = "المسبحة العائمة والتنبيهات",
                        subtitle = "ضبط ظهور المسبحة فوق الشاشة وإشعارات الأذكار التلقائية",
                        icon = Icons.Default.NotificationsActive
                    )
                }

                item {
                    FloatingTasbeehCard()
                }

                item {
                    BackgroundNotificationsCard(
                        notificationSound = notificationSound,
                        onToggleNotificationSound = onToggleNotificationSound
                    )
                }

                item {
                    AzkarNotificationsCard()
                }

                item {
                    PrayerTimesSettingsCard()
                }
            }

            // 4. قسم صفحات المطور
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DEVELOPER) {
                item {
                    SettingsSectionHeader(
                        title = "التواصل وصفحات المطور",
                        subtitle = "روابط الدعم والقناة والمدونة ومنصة الذكاء الاصطناعي",
                        icon = Icons.Default.Verified
                    )
                }

                item {
                    DeveloperPagesCard(
                        onSupportDeveloper = onSupportDeveloper,
                        onTelegram = onTelegram,
                        onBlog = onBlog,
                        onPlatform = onPlatform
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  عناصر تنظيمية قابلة لإعادة الاستخدام                             */
/* ------------------------------------------------------------------ */

@Composable
private fun SettingsOverviewBanner(
    darkMode: Boolean,
    fontSizeIndex: Int,
    speechRate: Float,
    islamicBgEnabled: Boolean
) {
    val fontLabel = when (fontSizeIndex) {
        0 -> "صغير"
        2 -> "كبير"
        else -> "متوسط"
    }
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
                            Color(0xFF154739),
                            Color(0xFF1E5B48),
                            Color(0xFF13342A)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFD36A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFFFD36A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "مركز تخصيص زاد المسلم",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "خلفيات إسلامية متجددة وتجربة قراءة مريحة",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewStatPill(
                        label = if (darkMode) "داكن" else "فاتح",
                        caption = "المظهر",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = if (islamicBgEnabled) "مفعّلة" else "موقفة",
                        caption = "الخلفيات",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = fontLabel,
                        caption = "الخط",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewStatPill(
                        label = "${String.format("%.1f", speechRate)}x",
                        caption = "النطق",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStatPill(
    label: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            caption,
            color = Color(0xFFFFE39C),
            fontSize = 10.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsCardShell(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/* ------------------------------------------------------------------ */
/*  بطاقات المظهر والخط والصوت والتنبيهات                            */
/* ------------------------------------------------------------------ */

@Composable
private fun ThemeAppearanceCard(
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    SettingsCardShell(
        title = "مظهر التطبيق",
        icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
        badge = if (darkMode) "داكن" else "فاتح"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onToggleDarkMode(!darkMode) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("الوضع الداكن (Night Mode)", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    "مريح للعين في القراءة الليلية ويخفض سطوع الشاشة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
        }
    }
}

@Composable
private fun FontSizeSettingsCard(
    fontSizeIndex: Int,
    onFontSizeChange: (Int) -> Unit
) {
    val fontSizes = listOf(
        0 to ("صغير (16sp)" to 16.sp),
        1 to ("متوسط (18sp)" to 18.sp),
        2 to ("كبير (22sp)" to 22.sp)
    )
    SettingsCardShell(
        title = "حجم الخط في شاشات القراءة",
        icon = Icons.Default.FormatSize,
        badge = when (fontSizeIndex) {
            0 -> "صغير"
            2 -> "كبير"
            else -> "متوسط"
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Text(
                text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
                fontSize = fontSizes.firstOrNull { it.first == fontSizeIndex }?.second?.second ?: 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(14.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        fontSizes.forEach { (index, pair) ->
            val selected = fontSizeIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .clickable { onFontSizeChange(index) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    pair.first,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                RadioButton(
                    selected = selected,
                    onClick = { onFontSizeChange(index) }
                )
            }
        }
    }
}

@Composable
private fun AiVoiceSettingsCard(
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChange: (Float) -> Unit
) {
    SettingsCardShell(
        title = "إعدادات روبوت الذكاء الاصطناعي الصوتي",
        icon = Icons.Default.GraphicEq
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سرعة النطق", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${String.format("%.1f", speechRate)}x",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = speechRate,
            onValueChange = onSpeechRateChange,
            valueRange = 0.5f..2.0f,
            steps = 5
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("نبرة الصوت", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                String.format("%.1f", speechPitch),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = speechPitch,
            onValueChange = onSpeechPitchChange,
            valueRange = 0.5f..1.5f,
            steps = 3
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                onSpeechRateChange(1.0f)
                onSpeechPitchChange(1.0f)
            },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("استعادة السرعة والنبرة الطبيعية")
        }
    }
}

@Composable
private fun BackgroundNotificationsCard(
    notificationSound: Boolean,
    onToggleNotificationSound: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    SettingsCardShell(
        title = "إشعارات الخلفية وشاشة القفل",
        icon = Icons.Default.Notifications,
        badge = if (notificationsAllowed) "الإذن مفعّل" else "يحتاج إذن"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onToggleNotificationSound(!notificationSound) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("صوت التنبيه مع الإشعارات", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Switch(checked = notificationSound, onCheckedChange = onToggleNotificationSound)
        }

        Spacer(Modifier.height(10.dp))
        PermissionStatusRow(
            label = "إذن إظهار الإشعارات",
            granted = notificationsAllowed,
            grantedText = "مفعّل",
            deniedText = "غير مفعّل"
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("فتح إعدادات إشعارات التطبيق")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
            val exactAllowed = alarmManager.canScheduleExactAlarms()
            Spacer(Modifier.height(14.dp))
            PermissionStatusRow(
                label = "المنبّه الدقيق (للتوقيت المحدد)",
                granted = exactAllowed,
                grantedText = "مسموح",
                deniedText = "غير مسموح؛ قد يتأخر التنبيه"
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إعداد وصول المنبّهات الدقيقة")
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    grantedText: String,
    deniedText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (granted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.09f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            if (granted) grantedText else deniedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

/* ------------------------------------------------------------------ */
/*  خلفية الصفحات والخلفيات الإسلامية المدمجة                         */
/* ------------------------------------------------------------------ */

@Composable
private fun AppBackgroundCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val revision = AppBackgroundStore.revision
    var pendingTargets by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var scrim by remember(revision) { mutableIntStateOf(AppBackgroundStore.scrimPercent(context)) }
    var islamicEnabled by remember(revision) {
        mutableStateOf(AppBackgroundStore.isIslamicBackgroundsEnabled(context))
    }
    var pagesExpanded by rememberSaveable { mutableStateOf(false) }

    val selectedPages = remember(revision) {
        AppBackgroundStore.pages.map { it.key }
            .filter { AppBackgroundStore.isPageSelected(context, it) }
            .toSet()
    }
    val customCount = remember(revision) {
        AppBackgroundStore.pages.count { AppBackgroundStore.isEnabled(context, it.key) }
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
            message = "حدّد صفحة واحدة على الأقل بوضع علامة ✔ في القائمة أولًا"
            pagesExpanded = true
            return
        }
        pendingTargets = keys
        picker.launch("image/*")
    }

    SettingsCardShell(
        title = "الخلفيات الإسلامية وخلفيات الصفحات",
        icon = Icons.Default.Wallpaper,
        badge = if (islamicEnabled) "إسلامية متجددة" else "معطّلة"
    ) {
        // تفعيل الخلفيات الإسلامية الدينية المدمجة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                    val next = !islamicEnabled
                    islamicEnabled = next
                    AppBackgroundStore.setIslamicBackgroundsEnabled(context, next)
                    message = if (next) {
                        "تم تفعيل الخلفيات الإسلامية لكل صفحات التطبيق"
                    } else {
                        "تم إيقاف الخلفيات الإسلامية الافتراضية للصفحات"
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("الخلفيات الإسلامية الدينية للصفحات", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "تُعرض خلفيات إسلامية هادئة في الصفحات، وتتبدّل خلفية الشاشة الرئيسية تلقائيًا عند الرجوع إليها.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = islamicEnabled,
                onCheckedChange = { checked ->
                    islamicEnabled = checked
                    AppBackgroundStore.setIslamicBackgroundsEnabled(context, checked)
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "مجموعة الخلفيات الإسلامية المولّدة (انقر لتطبيقها على الصفحات المحددة أو معاينتها):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppBackgroundStore.builtInBackgrounds.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (selectedPages.isEmpty()) {
                                AppBackgroundStore.rotateHomeBackground()
                                message = "تم تفعيل «${item.title}» في الخلفيات المتجددة؛ حدّد صفحات من القائمة لتثبيتها عليها."
                            } else {
                                selectedPages.forEach { key ->
                                    AppBackgroundStore.clearPage(context, key)
                                    AppBackgroundStore.setBuiltInForPage(context, key, item.id)
                                }
                                message = "تم تعيين الخلفية الإسلامية «${item.title}» لـ ${selectedPages.size} صفحة"
                            }
                        }
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = item.drawableRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 64.dp, height = 88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.title,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(10.dp))

        // رأس قائمة التخصيص القابل للطي لتجنّب إطالة الصفحة بدون داعٍ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .clickable { pagesExpanded = !pagesExpanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "تخصيص خلفية كل صفحة من معرض الهاتف",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${AppBackgroundStore.pages.size} صفحة · $customCount صورة مخصصة مفعّلة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (pagesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (pagesExpanded) "طي القائمة" else "فتح القائمة"
            )
        }

        AnimatedVisibility(
            visible = pagesExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = { chooseFor(selectedPages.toList()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selectedPages.isEmpty()) "اختيار صورة من المعرض للصفحات المحددة"
                        else "تعيين صورة لـ ${selectedPages.size} صفحة",
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        message = "تمت إزالة الصور المخصصة والعودة للخلفيات الإسلامية التلقائية"
                    }) { Text("استعادة التلقائي", fontSize = 12.sp) }
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
                            message = "تمت إزالة الخلفية المخصصة لـ «${page.title}»"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("درجة التعتيم فوق الخلفية", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "$scrim%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
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
            .heightIn(min = 48.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(
                if (hasImage) (if (checked) "صورة مخصصة مفعّلة" else "صورة محفوظة · غير مفعّلة")
                else "خلفية إسلامية تلقائية",
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
        IconButton(onClick = onPick) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = "تعيين صورة",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (hasImage) {
            IconButton(onClick = onClear) {
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 160 || bounds.outHeight / sample > 160) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, options)?.asImageBitmap()
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

    SettingsCardShell(
        title = "ألوان الصفحات وألوان الخطوط",
        icon = Icons.Default.ColorLens
    ) {
        Text(
            "اختر لونًا ثابتًا لخلفية الصفحات ولون الخط، أو اتركه افتراضيًا لعرض الخلفيات الإسلامية.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        OutlinedButton(
            onClick = {
                onPageColorChange(0L)
                onFontColorChange(0L)
            },
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("استعادة الألوان الافتراضية")
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
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(value) }
                    .padding(vertical = 2.dp)
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
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

    SettingsCardShell(
        title = "المسبحة العائمة فوق الشاشة",
        icon = Icons.Default.Layers,
        badge = if (enabled) "مفعّلة" else "متوقفة"
    ) {
        Text(
            "مسبحة تظهر فوق شاشة الهاتف حتى وأنت خارج التطبيق. انقر للتسبيح، اسحبها لأي مكان، أو كبّرها وصغّرها بإصبعين.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تشغيل المسبحة العائمة", fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
            PermissionStatusRow(
                label = "إذن الظهور فوق التطبيقات",
                granted = false,
                grantedText = "ممنوح",
                deniedText = "مطلوب لعرض المسبحة"
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("منح إذن الظهور فوق التطبيقات")
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("استثناء التطبيق من تحسين البطارية")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "الاستثناء يمنع النظام من إيقاف المسبحة أو الأذكار الصوتية عند قفل الشاشة.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
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
    SettingsCardShell(
        title = "صفحات المطور",
        icon = Icons.Default.Person
    ) {
        Text(
            "محمد الحزمي — دعم المطور وقناته ومدونته ومنصته",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        DeveloperPageRow(
            icon = Icons.Default.Favorite,
            title = "دعم المطور",
            subtitle = "محمد الحزمي · محفظة جيب",
            onClick = onSupportDeveloper
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        DeveloperPageRow(
            icon = Icons.Default.Verified,
            title = "قناة المطور على تليجرام",
            subtitle = "t.me/moh_alymani1",
            onClick = onTelegram
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        DeveloperPageRow(
            icon = Icons.Default.Article,
            title = "مدونة المطور",
            subtitle = "mohammedalhzmi.blogspot.com",
            onClick = onBlog
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        DeveloperPageRow(
            icon = Icons.Default.AutoAwesome,
            title = "منصة المطور محمد الحزمي",
            subtitle = "منصة الذكاء الاصطناعي",
            onClick = onPlatform
        )
    }
}

@Composable
private fun DeveloperPageRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

/* ------------------------------------------------------------------ */
/*  إشعارات الأذكار التلقائية                                         */
/* ------------------------------------------------------------------ */

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

    SettingsCardShell(
        title = "إشعارات الأذكار التلقائية",
        icon = Icons.Default.Schedule,
        badge = if (enabled) "نشط" else "متوقف"
    ) {
        Text(
            "يعمل التطبيق على تذكيرك بالأذكار تلقائيًا في الوقت الذي تحدده، حتى لو كان مغلقًا.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تفعيل الإشعارات الصوتية التلقائية", fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
        Spacer(modifier = Modifier.height(12.dp))
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
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ وتفعيل")
            }
            OutlinedButton(
                onClick = {
                    AthkarReminderScheduler.runOnceNow(context)
                    message = "تم إرسال إشعار تجريبي بعد لحظات"
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إشعار تجريبي")
            }
        }
        message?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  تنبيهات ومواقيت الصلوات الخمس والموقع الجغرافي                     */
/* ------------------------------------------------------------------ */

@Composable
private fun PrayerTimesSettingsCard() {
    val context = LocalContext.current
    val rev = PrayerStore.revision
    var enabled by remember(rev) { mutableStateOf(PrayerStore.isNotificationsEnabled(context)) }
    val locationName = remember(rev) { PrayerStore.locationName(context) }
    val (_, prayers) = remember(rev) { PrayerStore.resolveDayPrayers(context) }
    var selectedMethod by remember(rev) { mutableStateOf(PrayerStore.method(context)) }
    var isLocating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var editingPrayer by remember { mutableStateOf<ResolvedPrayerTime?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }

    val gpsLauncher = rememberLauncherForActivityResult(
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
            statusMessage = "تم رفض إذن الموقع؛ يمكنك اختيار المدينة يدويًا."
        }
    }

    SettingsCardShell(
        title = "مواقيت الصلوات الخمس والموقع",
        icon = Icons.Default.Mosque,
        badge = if (enabled) "مفعّلة" else "متوقفة"
    ) {
        Text(
            "تُحسب مواقيت الصلوات وتُجدول تنبيهاتها تلقائيًا حسب موقعك الجغرافي، أو يمكنك تعديل أي وقت صلاة يدويًا هنا.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تنبيهات الصلوات الخمس التلقائية", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    enabled = checked
                    PrayerStore.setNotificationsEnabled(context, checked)
                    statusMessage = if (checked) "تم تفعيل تنبيهات الصلوات الخمس" else "تم إيقاف تنبيهات الصلوات الخمس"
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "الموقع الحالي: $locationName",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (PrayerStore.hasLocationPermission(context)) {
                        isLocating = true
                        PrayerStore.refreshLocationFromDevice(context) { _, msg ->
                            isLocating = false
                            statusMessage = msg
                        }
                    } else {
                        gpsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLocating,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isLocating) "جارٍ التحديد..." else "تحديث بالموقع")
            }
            OutlinedButton(
                onClick = { showCityPicker = true },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("مدينة يدوية")
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("طريقة الحساب", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        statusMessage = "تم اختيار: ${method.arabicTitle}"
                    },
                    label = { Text(method.arabicTitle, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("تعديل مواقيت الصلوات يدويًا:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        prayers.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { editingPrayer = item }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.type.arabicName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (item.isManualOverride) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "(يدوي)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.formatted12(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "تعديل ${item.type.arabicName}",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                PrayerStore.clearAllManualOverrides(context)
                PrayerAlarmScheduler.scheduleAll(context)
                statusMessage = "تمت استعادة الحساب التلقائي لجميع الصلوات"
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("استعادة التوقيت التلقائي حسب الموقع")
        }

        statusMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }

    editingPrayer?.let { prayer ->
        ManualPrayerEditDialog(
            prayer = prayer,
            onDismiss = { editingPrayer = null },
            onSave = { h, m ->
                PrayerStore.setManualTime(context, prayer.type, h, m)
                editingPrayer = null
                statusMessage = "تم تعديل صلاة ${prayer.type.arabicName} إلى ${String.format("%02d:%02d", h, m)}"
            },
            onResetAuto = {
                PrayerStore.setManualTime(context, prayer.type, null, null)
                PrayerStore.setOffsetMinutes(context, prayer.type, 0)
                editingPrayer = null
                statusMessage = "تمت إعادة صلاة ${prayer.type.arabicName} للتوقيت التلقائي"
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
