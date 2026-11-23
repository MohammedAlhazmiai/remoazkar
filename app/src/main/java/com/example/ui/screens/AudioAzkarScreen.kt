package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.HisnRepository
import com.example.data.HumanAzkarAudioCatalog
import com.example.data.HumanAzkarScheduler
import com.example.service.AudioAzkarPlaybackService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAzkarScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val chapters = remember(HisnRepository.isLoaded) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.chapters.filter { it.items.isNotEmpty() }
    }
    var selectedId by remember(chapters) {
        mutableIntStateOf(HisnRepository.featuredIds().firstOrNull() ?: chapters.firstOrNull()?.id ?: -1)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val selected = chapters.firstOrNull { it.id == selectedId } ?: chapters.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأذكار الصوتية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { stopBackgroundPlayback(context) }) {
                        Icon(Icons.Default.Stop, contentDescription = "إيقاف الصوت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("حصن المسلم") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("تسجيلات بشرية") }
                )
            }
            if (selectedTab == 0) {
                TextAzkarContent(context, chapters, selected, onChapterSelected = { selectedId = it })
            } else {
                HumanRecordingsContent()
            }
        }
    }
}

@Composable
private fun TextAzkarContent(
    context: Context,
    chapters: List<com.example.data.HisnChapter>,
    selected: com.example.data.HisnChapter?,
    onChapterSelected: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                "في هذا التبويب ينطق النص محرّك الصوت العربي المثبّت على جهازك. للتسجيلات البشرية افتح تبويب «تسجيلات بشرية».",
                modifier = Modifier.padding(14.dp),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        if (chapters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("تعذّر تحميل الأذكار النصية.")
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    FilterChip(
                        selected = chapter.id == selected?.id,
                        onClick = { onChapterSelected(chapter.id) },
                        label = { Text(chapter.title, maxLines = 1) }
                    )
                }
            }
            selected?.let { chapter ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(chapter.title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    FilledTonalButton(onClick = {
                        val text = chapter.items.joinToString(". ") { it.text }
                        startTextPlayback(context, chapter.title, text)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("استمع للباب")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chapter.items.indices.toList(), key = { it }) { index ->
                        val item = chapter.items[index]
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(item.text, fontSize = 19.sp, lineHeight = 34.sp, textAlign = TextAlign.Start)
                                if (item.repeat > 1) {
                                    Text(
                                        "التكرار: ${item.repeat} مرات",
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (item.footnote.isNotBlank()) {
                                    Text(
                                        item.footnote,
                                        modifier = Modifier.padding(top = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { startTextPlayback(context, chapter.title, item.text) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Text("استمع")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HumanRecordingsContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(HumanAzkarScheduler.isEnabled(context)) }
    var intervalMinutes by remember { mutableIntStateOf(HumanAzkarScheduler.intervalMinutes(context)) }
    var customInterval by rememberSaveable { mutableStateOf("") }
    var customUnitIsHours by rememberSaveable { mutableStateOf(false) }
    var canScheduleExact by remember { mutableStateOf(HumanAzkarScheduler.canScheduleExactAlarms(context)) }
    var pendingExactPermissionEnable by rememberSaveable { mutableStateOf(false) }

    val applyEnabled: (Boolean) -> Unit = { shouldEnable ->
        val applied = HumanAzkarScheduler.setEnabled(context, shouldEnable)
        enabled = if (shouldEnable) applied else false
        if (shouldEnable && !applied) {
            Toast.makeText(context, "تعذّر تفعيل الجدولة. تحقق من صلاحية المنبهات الدقيقة.", Toast.LENGTH_LONG).show()
        }
    }

    val openExactAlarmSettings: (Boolean) -> Unit = { enableAfterGrant ->
        pendingExactPermissionEnable = enableAfterGrant
        val opened = runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }.isSuccess
        if (!opened) {
            pendingExactPermissionEnable = false
            Toast.makeText(context, "تعذّر فتح إعدادات المنبهات الدقيقة.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(lifecycleOwner, context, canScheduleExact, enabled, pendingExactPermissionEnable) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val exactPermissionNow = HumanAzkarScheduler.canScheduleExactAlarms(context)
                val permissionChanged = exactPermissionNow != canScheduleExact
                canScheduleExact = exactPermissionNow

                if (pendingExactPermissionEnable) {
                    pendingExactPermissionEnable = false
                    if (exactPermissionNow) {
                        applyEnabled(true)
                    } else {
                        Toast.makeText(context, "لم تُمنح صلاحية المنبهات الدقيقة؛ لم يتم تفعيل الجدولة.", Toast.LENGTH_LONG).show()
                    }
                } else if (permissionChanged && enabled) {
                    val scheduled = HumanAzkarScheduler.rescheduleIfEnabled(context)
                    if (!scheduled && exactPermissionNow) {
                        Toast.makeText(context, "تعذّرت إعادة جدولة التسجيلات. أعد تفعيلها من الشاشة.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    "تضمّن التطبيق ${HumanAzkarAudioCatalog.tracks.size} تسجيلًا بشريًا قصيرًا من الملف المرفق. يمكنك معاينة كل مقطع أدناه، أو تفعيل تشغيلها بالتناوب على الفاصل الزمني الذي تختاره (دقائق أو ساعات، جاهزة أو يدوية). يتوقف التشغيل بعد كل مقطع ثم يُشغَّل المقطع التالي بعد الفاصل المحدد.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("تشغيل دوري للتسجيلات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                when {
                                    enabled && canScheduleExact -> "يُشغّل مقطعًا بشريًا واحدًا كل ${intervalLabel(intervalMinutes)}، ثم يتوقف حتى الموعد التالي."
                                    enabled -> "الإعداد محفوظ، لكن التشغيل متوقف مؤقتًا حتى استعادة صلاحية المنبهات الدقيقة."
                                    else -> "التشغيل الدوري متوقف إلى أن تفعّله."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    applyEnabled(false)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                                    openExactAlarmSettings(true)
                                } else {
                                    applyEnabled(true)
                                }
                            }
                        )
                    }

                    Text(
                        "اختر الفاصل بين كل ذكر والذي يليه (من 1 دقيقة إلى 1440 دقيقة)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 5, 10, 30, 60, 120, 180).forEach { minutes ->
                            FilterChip(
                                selected = intervalMinutes == minutes,
                                onClick = {
                                    intervalMinutes = minutes
                                    HumanAzkarScheduler.setIntervalMinutes(context, minutes)
                                },
                                label = { Text(intervalLabel(minutes)) }
                            )
                        }
                    }
                    Text(
                        "الذكر الحالي: كل ${intervalLabel(intervalMinutes)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !customUnitIsHours,
                            onClick = { customUnitIsHours = false },
                            label = { Text("دقائق") }
                        )
                        FilterChip(
                            selected = customUnitIsHours,
                            onClick = { customUnitIsHours = true },
                            label = { Text("ساعات") }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customInterval,
                            onValueChange = { value ->
                                if (value.length <= 4 && value.all { it.isDigit() || it in '٠'..'٩' || it in '۰'..'۹' }) {
                                    customInterval = value
                                }
                            },
                            label = { Text(if (customUnitIsHours) "فاصل مخصص (ساعات)" else "فاصل مخصص (دقائق)") },
                            placeholder = { Text(if (customUnitIsHours) "مثال: 2" else "مثال: 45") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val entered = parseIntervalMinutes(customInterval)
                            val minutes = entered?.let { if (customUnitIsHours) it * 60 else it }
                            if (minutes == null || minutes !in HumanAzkarScheduler.MIN_INTERVAL_MINUTES..HumanAzkarScheduler.MAX_INTERVAL_MINUTES) {
                                Toast.makeText(
                                    context,
                                    if (customUnitIsHours)
                                        "أدخل عددًا صحيحًا من الساعات بحيث لا يتجاوز 24 ساعة."
                                    else
                                        "أدخل عددًا صحيحًا بين 1 و1440 دقيقة.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                intervalMinutes = minutes
                                HumanAzkarScheduler.setIntervalMinutes(context, minutes)
                                customInterval = ""
                            }
                        }) { Text("تطبيق") }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            if (canScheduleExact) "صلاحية المنبهات الدقيقة متاحة؛ يستخدمها التطبيق لبدء خدمة تشغيل التسجيل عند الموعد. قد يؤخر Android بعض المواعيد في وضع توفير الطاقة."
                            else "صلاحية المنبهات الدقيقة غير متاحة؛ لن يبدأ التشغيل الدوري للتسجيلات في الخلفية. امنح هذه الصلاحية أولًا؛ لا يُستبدل تشغيل المقطع بإشعار يُعرض وكأنه تشغيل ناجح.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!canScheduleExact) {
                            OutlinedButton(
                                onClick = { openExactAlarmSettings(false) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("فتح إعدادات المنبهات الدقيقة") }
                        }
                    }
                    Text(
                        "قد يسلّم Android منبهات «السماح أثناء الخمول» مرة واحدة تقريبًا في الدقيقة في الظروف العادية، لكنه قد يباعد بينها إلى نحو 15 دقيقة أو أكثر في وضع Doze. لذلك لا يمكن ضمان فواصل 1–5 دقائق أثناء السكون. السماح بإشعارات التطبيق يُظهر إشعار الخدمة المستمر، لكنه ليس شرطًا لبدء تشغيل التسجيل. إزالة التطبيق من التطبيقات الحديثة لا توقف المنبه عادةً؛ أما «فرض الإيقاف» أو قيود البطارية/التشغيل التلقائي لدى بعض الشركات فقد توقفه حتى إعادة السماح به.",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text("معاينة التسجيلات البشرية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        items(HumanAzkarAudioCatalog.tracks, key = { it.resourceName }) { track ->
            Card(shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.Medium)
                        Text("تسجيل صوتي بشري", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        startHumanRecording(context, track.resourceName, track.title)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل ${track.title}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun intervalLabel(minutes: Int): String {
    if (minutes < 60) return "$minutes د"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "$hours س" else "$hours س $rest د"
}

private fun parseIntervalMinutes(input: String): Int? {
    val asciiDigits = buildString {
        input.forEach { character ->
            when (character) {
                in '٠'..'٩' -> append(('0'.code + (character.code - '٠'.code)).toChar())
                in '۰'..'۹' -> append(('0'.code + (character.code - '۰'.code)).toChar())
                else -> append(character)
            }
        }
    }
    return asciiDigits.trim().toIntOrNull()
}

private fun startTextPlayback(context: Context, title: String, text: String) {
    val intent = Intent(context, AudioAzkarPlaybackService::class.java).apply {
        putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, title)
        putExtra(AudioAzkarPlaybackService.EXTRA_TEXT, text)
    }
    runCatching { ContextCompat.startForegroundService(context, intent) }
        .onFailure { Toast.makeText(context, "تعذّر بدء الصوت في الخلفية: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
}

private fun startHumanRecording(context: Context, resourceName: String, title: String) {
    val intent = Intent(context, AudioAzkarPlaybackService::class.java).apply {
        putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, title)
        putExtra(AudioAzkarPlaybackService.EXTRA_AUDIO_RESOURCE_NAME, resourceName)
    }
    runCatching { ContextCompat.startForegroundService(context, intent) }
        .onFailure { Toast.makeText(context, "تعذّر تشغيل التسجيل: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
}

private fun stopBackgroundPlayback(context: Context) {
    context.stopService(Intent(context, AudioAzkarPlaybackService::class.java))
}
