package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppBackgroundStore
import com.example.data.HijriCalendar
import com.example.data.HisnChapter
import com.example.data.HisnRepository
import com.example.data.PrayerStore
import com.example.data.PreferencesManager
import com.example.data.PreferencesSnapshot
import com.example.data.QuranReaderPrefs
import com.example.data.QuranRepository
import com.example.data.RuqyahData
import com.example.data.TtsManager
import com.example.ui.components.HomeOnboardingTourDialog
import com.example.ui.components.SmartReaderBus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onQuranClick: () -> Unit,
    onHisnClick: () -> Unit,
    onTasbeehClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onHijriClick: () -> Unit,
    onCustomAzkarClick: () -> Unit,
    onAiRobotClick: () -> Unit,
    onSpiritualCompanionClick: () -> Unit,
    onWirdClick: () -> Unit,
    onRuqyahClick: () -> Unit,
    onAdiyaClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onChapterClick: (Int) -> Unit,
    onMenuClick: () -> Unit = {},
    onKhatmahClick: () -> Unit = {},
    onNamesOfAllahClick: () -> Unit = {},
    onProphetDuasClick: () -> Unit = {},
    onAudioAzkarClick: () -> Unit = {},
    onPathToSuccessClick: () -> Unit = {},
    onAgeCalculatorClick: () -> Unit = {},
    onFloatingTasbeehClick: () -> Unit = {},
    onPrayerTimesClick: () -> Unit = {},
    onQiblaClick: () -> Unit = {},
    onIslamicAiClick: () -> Unit = {},
    onResumeQuranClick: (Int, Int) -> Unit = { _, _ -> onQuranClick() },
    khatmahPagesDone: Int = 0,
    khatmahActive: Boolean = false
) {
    val context = LocalContext.current
    val hijri = remember(HijriCalendar.offsetDays) { HijriCalendar.today() }
    val prayerRevision = PrayerStore.revision
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val nextPrayer = remember(prayerRevision, nowMillis / 30_000L) {
        PrayerStore.nextPrayer(context, nowMillis)
    }
    val prayerLocationName = remember(prayerRevision) { PrayerStore.locationName(context) }
    val featured = remember(HisnRepository.isLoaded) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.featuredIds().mapNotNull { HisnRepository.chapter(it) }
    }
    val daily = remember(featured) { featured.firstOrNull() }
    val morningChapter = remember(HisnRepository.isLoaded) {
        HisnRepository.chapters.firstOrNull {
            HisnRepository.normalize(it.title) == HisnRepository.normalize("أذكار الصباح")
        }
    }
    val eveningChapter = remember(featured) {
        HisnRepository.chapters.firstOrNull {
            HisnRepository.normalize(it.title) == HisnRepository.normalize("أذكار المساء")
        }
    }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val homeTourCompleted by prefs.homeTourCompletedFlow.collectAsState(initial = null as Boolean?)
    val readerPrefs by prefs.quranReaderPrefsFlow.collectAsState(initial = QuranReaderPrefs())
    val homeSnapshot by prefs.snapshotFlow.collectAsState(initial = PreferencesSnapshot())
    val lastSurahName = remember(readerPrefs.lastSurah, QuranRepository.isLoaded) {
        if (readerPrefs.lastSurah > 0) {
            QuranRepository.ensureLoaded(context)
            QuranRepository.surahs.firstOrNull { it.number == readerPrefs.lastSurah }?.name
        } else {
            null
        }
    }

    if (homeTourCompleted == false) {
        HomeOnboardingTourDialog(
            onDismiss = {
                scope.launch { prefs.setHomeTourCompleted(true) }
            },
            onOpenAudioAzkar = {
                scope.launch { prefs.setHomeTourCompleted(true) }
                onAudioAzkarClick()
            },
            onOpenSettings = {
                scope.launch { prefs.setHomeTourCompleted(true) }
                onAdvancedSettingsClick()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("زاد المسلم", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(hijri.formatted, fontSize = 11.sp)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onQiblaClick,
                        modifier = Modifier.testTag("qibla_icon_button")
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = "اتجاه القبلة")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Default.Star, contentDescription = "المفضلة")
                    }
                    IconButton(onClick = onAdvancedSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF123B31),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val bgRevision = AppBackgroundStore.revision
            val customHomePath = remember(bgRevision) {
                if (AppBackgroundStore.isEnabled(context, "home")) {
                    AppBackgroundStore.imagePath(context, "home")
                } else {
                    null
                }
            }
            val customBitmap = remember(customHomePath) {
                customHomePath?.let { path ->
                    runCatching {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(path, bounds)
                        var sample = 1
                        val longest = maxOf(bounds.outWidth, bounds.outHeight)
                        while (longest / sample > 2200) sample *= 2
                        BitmapFactory.decodeFile(
                            path,
                            BitmapFactory.Options().apply { inSampleSize = sample }
                        )?.asImageBitmap()
                    }.getOrNull()
                }
            }

            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val homeBgDrawable = AppBackgroundStore.currentHomeDrawable()
                Crossfade(
                    targetState = homeBgDrawable,
                    animationSpec = tween(durationMillis = 600),
                    label = "home_background_crossfade"
                ) { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            val activeSeason = if (customBitmap == null) AppBackgroundStore.currentHomeSeason() else null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x1C112720),
                                Color(0x4D112720),
                                Color(0x8A111D18)
                            )
                        )
                    )
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (activeSeason != null) {
                item {
                    SeasonalOccasionBanner(
                        title = activeSeason.title,
                        subtitle = activeSeason.subtitle
                    )
                }
            }
            item {
                HeroCard(daily)
            }

            item {
                NextPrayerHomeCard(
                    prayerName = nextPrayer?.prayer?.type?.arabicName ?: "—",
                    prayerTime = nextPrayer?.prayer?.formatted12() ?: "",
                    locationName = prayerLocationName,
                    remainingMillis = ((nextPrayer?.triggerMillis ?: nowMillis) - nowMillis).coerceAtLeast(0L),
                    onPrayerTimesClick = onPrayerTimesClick,
                    onQiblaClick = onQiblaClick
                )
            }

            item {
                IslamicAiHomeCard(onClick = onIslamicAiClick)
            }

            item {
                DailyProgressHomeCard(
                    lastSurahName = lastSurahName,
                    lastPdfPage = readerPrefs.lastPdfPage,
                    tasbeehTotal = homeSnapshot.tasbeehTotal,
                    khatmahPagesDone = if (khatmahActive) khatmahPagesDone else homeSnapshot.khatmahPagesDone,
                    khatmahActive = khatmahActive || homeSnapshot.khatmahActive,
                    onQuranActionClick = {
                        if (readerPrefs.lastPdfPage > 0) {
                            onResumeQuranClick(readerPrefs.lastSurah, readerPrefs.lastPdfPage)
                        } else {
                            onQuranClick()
                        }
                    },
                    onTasbeehClick = onTasbeehClick,
                    onKhatmahClick = onKhatmahClick
                )
            }

            item { HomeSectionTitle("الصلاة والقبلة") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard("مواقيت الصلاة", "الصلوات الخمس والتنبيهات", Icons.Default.Schedule, Modifier.weight(1f), onPrayerTimesClick)
                    FeatureCard("اتجاه القبلة", "بوصلة القبلة حسب موقعك", Icons.Default.Explore, Modifier.weight(1f), onQiblaClick)
                }
            }

            item { HomeSectionTitle("أذكار اليوم") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard("أذكار الصباح", "ورد الصباح", Icons.Default.WbSunny, Modifier.weight(1f)) {
                        morningChapter?.let { onChapterClick(it.id) } ?: onHisnClick()
                    }
                    FeatureCard("أذكار المساء", "ورد المساء", Icons.Default.Bedtime, Modifier.weight(1f)) {
                        eveningChapter?.let { onChapterClick(it.id) } ?: onHisnClick()
                    }
                }
            }
            item { HomeSectionTitle("القرآن الكريم") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureCard("القرآن الكريم", "114 سورة", Icons.Default.MenuBook, Modifier.weight(1f), onQuranClick)
                    FeatureCard("خطة ختم القرآن", "604 صفحة ÷ المدة", Icons.Default.EventAvailable, Modifier.weight(1f), onKhatmahClick)
                }
            }
            item { HomeSectionTitle("الذكر والاستماع") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("المسبحة", "عدّاد وهدف يومي", Icons.Default.TouchApp, Modifier.weight(1f), onTasbeehClick)
                        FeatureCard("حصن المسلم", "${HisnRepository.chapters.size} بابًا", Icons.Default.Security, Modifier.weight(1f), onHisnClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("الأذكار الصوتية", "تسجيلات بشرية", Icons.Default.Headphones, Modifier.weight(1f), onAudioAzkarClick)
                        FeatureCard("الرقية الشرعية", "${RuqyahData.items.size} نصًا", Icons.Default.HealthAndSafety, Modifier.weight(1f), onRuqyahClick)
                    }
                }
            }
            item { HomeSectionTitle("الأدعية والمعرفة") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("الأدعية والأحاديث", "82 نصًا مأثورًا", Icons.Default.MenuBook, Modifier.weight(1f), onAdiyaClick)
                        FeatureCard("أدعية الأنبياء", "أدعية قرآنية", Icons.Default.Book, Modifier.weight(1f), onProphetDuasClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("أسماء الله الحسنى", "معاني الأسماء", Icons.Default.AutoAwesome, Modifier.weight(1f), onNamesOfAllahClick)
                        FeatureCard("خطة الورد اليومي", "تقدّمك وسلسلتك", Icons.Default.CheckCircle, Modifier.weight(1f), onWirdClick)
                    }
                }
            }
            item { HomeSectionTitle("أدوات وتنظيم") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("تذكيراتي", "أوقات تحددها", Icons.Default.Alarm, Modifier.weight(1f), onRemindersClick)
                        FeatureCard("أذكاري الخاصة", "أضف وعدّل", Icons.Default.EditNote, Modifier.weight(1f), onCustomAzkarClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("التقويم الهجري", "التاريخ والمناسبات", Icons.Default.CalendarMonth, Modifier.weight(1f), onHijriClick)
                        FeatureCard("القارئ الذكي", "استمع للنص المحدد", Icons.Default.RecordVoiceOver, Modifier.weight(1f), onAiRobotClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("طريق الناجحين", "طريق الخلود ورحلة الآخرة", Icons.Default.TrendingUp, Modifier.weight(1f), onPathToSuccessClick)
                        FeatureCard("الرفيق الروحي", "إرشاد يومي", Icons.Default.Favorite, Modifier.weight(1f), onSpiritualCompanionClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard("حاسبة العمر", "هجري وميلادي وتحويل التاريخ", Icons.Default.Calculate, Modifier.weight(1f), onAgeCalculatorClick)
                        FeatureCard("المسبحة العائمة", "تظهر فوق شاشة الهاتف", Icons.Default.Layers, Modifier.weight(1f), onFloatingTasbeehClick)
                    }
                }
            }

            item {
                Text(
                    "أبواب مميزة من حصن المسلم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFFFFF5D9),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(featured, key = { it.id }) { chapter ->
                ChapterMiniRow(chapter) { onChapterClick(chapter.id) }
            }

            item {
                DeveloperFooter()
            }
            }

        }
    }
}

@Composable
private fun CopyrightBanner() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "هذا التطبيق من برمجة وتطوير المطور محمد الحزمي",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "جميع الحقوق محفوظة للمطور 2026",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DeveloperFooter() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "هذا التطبيق صدقة جارية لوجه الله من المطور يبتغي فيه وجه الله والدعاء منكم",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "جميع الحقوق محفوظة للمطور محمد الحزمي 2026",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFFD36A))
        )
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFFF4D4))
    }
}

@Composable
private fun HeroCard(chapter: HisnChapter?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xEE174B3B),
                            Color(0xE61B342D)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "ذكر اليوم",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE29A)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = chapter?.preview ?: "سبحان الله وبحمده، سبحان الله العظيم.",
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            TtsManager.toggle(chapter?.preview ?: "سبحان الله وبحمده، سبحان الله العظيم.")
                        }
                    ) {
                        Icon(
                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (TtsManager.isSpeaking) "إيقاف" else "استمع للذكر")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        chapter?.title ?: "من أذكار المسلم",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .heightIn(min = 108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6173D33)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFD36A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFFFD36A),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = Color.White)
                    Text(
                        subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFFD8E5DD),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterMiniRow(chapter: HisnChapter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(chapter.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "${chapter.items.size} ذكرًا • ${chapter.totalRepeats} تكرارًا",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
    }
}

@Composable
private fun NextPrayerHomeCard(
    prayerName: String,
    prayerTime: String,
    locationName: String,
    remainingMillis: Long,
    onPrayerTimesClick: () -> Unit,
    onQiblaClick: () -> Unit
) {
    val totalMinutes = (remainingMillis / 60_000L).coerceAtLeast(0L)
    val h = totalMinutes / 60L
    val m = totalMinutes % 60L
    val remainingLabel = if (h > 0) "بعد $h س $m د" else "بعد $m دقيقة"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPrayerTimesClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB153A2F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFD36A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mosque,
                    contentDescription = null,
                    tint = Color(0xFFFFD36A),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "الصلاة القادمة: $prayerName ($prayerTime)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "$remainingLabel • $locationName",
                    color = Color(0xFFD6E6DD),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onQiblaClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = "اتجاه القبلة",
                    tint = Color(0xFFFFD36A)
                )
            }
        }
    }
}

@Composable
private fun SeasonalOccasionBanner(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_seasonal_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6103227)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFD36A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD36A),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color(0xFFFFD36A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    subtitle,
                    color = Color(0xFFE7F2EC),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun IslamicAiHomeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_islamic_ai_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xF0184D3D), Color(0xEB102E25))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x38FFD36A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFFFFD36A),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "المساعد الإسلامي الذكي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "ذكاء سحابي ومحلي • فتاوى، خطب، رسائل، خواطر، وتفسير أحلام",
                    color = Color(0xFFD8EBE0),
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "فتح المساعد الإسلامي الذكي",
                tint = Color(0xFFFFD36A)
            )
        }
    }
}

@Composable
private fun DailyProgressHomeCard(
    lastSurahName: String?,
    lastPdfPage: Int,
    tasbeehTotal: Int,
    khatmahPagesDone: Int,
    khatmahActive: Boolean,
    onQuranActionClick: () -> Unit,
    onTasbeehClick: () -> Unit,
    onKhatmahClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_daily_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE613352B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD36A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "متابعة القراءة وإنجازك اليومي",
                    color = Color(0xFFFFF4D4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quranTitle = if (lastPdfPage > 0) "متابعة القرآن" else "المصحف"
                val quranSub = when {
                    lastPdfPage > 0 && !lastSurahName.isNullOrBlank() -> "$lastSurahName • ص $lastPdfPage"
                    lastPdfPage > 0 -> "صفحة $lastPdfPage"
                    else -> "ابدأ القراءة"
                }
                DailyStatChip(
                    title = quranTitle,
                    value = quranSub,
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_progress_quran_chip"),
                    onClick = onQuranActionClick
                )
                DailyStatChip(
                    title = "التسبيح",
                    value = "$tasbeehTotal تسبيحة",
                    icon = Icons.Default.TouchApp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_progress_tasbeeh_chip"),
                    onClick = onTasbeehClick
                )
                DailyStatChip(
                    title = "الختمة",
                    value = if (khatmahActive) "$khatmahPagesDone/604" else "ابدأ ختمة",
                    icon = Icons.Default.EventAvailable,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_progress_khatmah_chip"),
                    onClick = onKhatmahClick
                )
            }
        }
    }
}

@Composable
private fun DailyStatChip(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color(0x33FFFFFF),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFFFD36A),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color(0xFFD8EBE0),
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
