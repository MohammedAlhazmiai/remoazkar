package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendar
import com.example.data.HisnChapter
import com.example.data.HisnRepository
import com.example.data.TtsManager
import com.example.ui.components.SmartReaderBus

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
    onAudioAzkarClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val hijri = remember { HijriCalendar.today() }
    val featured = remember(HisnRepository.isLoaded) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.featuredIds().mapNotNull { HisnRepository.chapter(it) }
    }
    val daily = remember(featured) { featured.firstOrNull() }

    // إشعار حقوق المطور: يظهر عند فتح الشاشة الرئيسية ويختفي تلقائيًا
    var showCopyright by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        showCopyright = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ريمواذكار", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(hijri.formatted, fontSize = 11.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Default.Star, contentDescription = "المفضلة")
                    }
                    IconButton(onClick = onAdvancedSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                HeroCard(daily)
            }

            item {
                Text("الأقسام", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "القرآن الكريم", "114 سورة كاملة", Icons.Default.MenuBook,
                            Modifier.weight(1f), onQuranClick
                        )
                        FeatureCard(
                            "حصن المسلم", "132 بابًا", Icons.Default.Security,
                            Modifier.weight(1f), onHisnClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "المسبحة", "عدّ أذكارك", Icons.Default.TouchApp,
                            Modifier.weight(1f), onTasbeehClick
                        )
                        FeatureCard(
                            "تذكيراتي", "أوقات تحددها", Icons.Default.Alarm,
                            Modifier.weight(1f), onRemindersClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "التقويم الهجري", "المناسبات", Icons.Default.CalendarMonth,
                            Modifier.weight(1f), onHijriClick
                        )
                        FeatureCard(
                            "أذكاري الخاصة", "أضف وعدّل", Icons.Default.EditNote,
                            Modifier.weight(1f), onCustomAzkarClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "المساعد الصوتي", "الروبوت يقرأ", Icons.Default.RecordVoiceOver,
                            Modifier.weight(1f), onAiRobotClick
                        )
                        FeatureCard(
                            "الرفيق الروحي", "حسب حالتك", Icons.Default.Favorite,
                            Modifier.weight(1f), onSpiritualCompanionClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "الأذكار الصوتية", "استمع للأذكار", Icons.Default.Headphones,
                            Modifier.weight(1f), onAudioAzkarClick
                        )
                        FeatureCard(
                            "حصن المسلم", "أذكار وأدعية", Icons.Default.Security,
                            Modifier.weight(1f), onHisnClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "خطة الورد اليومي", "تابع تقدّمك وسلسلتك", Icons.Default.CheckCircle,
                            Modifier.weight(1f), onWirdClick
                        )
                        FeatureCard(
                            "الرقية الشرعية", "30 نصًّا ثابتًا", Icons.Default.HealthAndSafety,
                            Modifier.weight(1f), onRuqyahClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "الأدعية والأربعون النبوية", "80 دعاءً مأثورًا", Icons.Default.MenuBook,
                            Modifier.weight(1f), onAdiyaClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "خطة ختم القرآن", "604 صفحة ÷ المدة", Icons.Default.EventAvailable,
                            Modifier.weight(1f), onKhatmahClick
                        )
                        FeatureCard(
                            "أسماء الله الحسنى", "99 اسمًا بمعانيها", Icons.Default.AutoAwesome,
                            Modifier.weight(1f), onNamesOfAllahClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureCard(
                            "أدعية الأنبياء", "25 دعاءً قرآنيًا", Icons.Default.Book,
                            Modifier.weight(1f), onProphetDuasClick
                        )
                    }
                }
            }

            item {
                Text(
                    "أبواب مميزة من حصن المسلم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
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

            AnimatedVisibility(
                visible = showCopyright,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                CopyrightBanner()
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
private fun HeroCard(chapter: HisnChapter?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = chapter?.preview ?: "سبحان الله وبحمده، سبحان الله العظيم.",
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
            .height(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
