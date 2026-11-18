package com.example.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendar
import com.example.data.HisnChapter
import com.example.data.HisnRepository
import com.example.data.TtsManager

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
    onAdvancedSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onChapterClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val hijri = remember { HijriCalendar.today() }
    val featured = remember {
        HisnRepository.ensureLoaded(context)
        HisnRepository.featuredIds().mapNotNull { HisnRepository.chapter(it) }
    }
    val daily = remember(featured) { featured.firstOrNull() }

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
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
                            "خطة الورد اليومي", "تابع تقدّمك وسلسلتك", Icons.Default.CheckCircle,
                            Modifier.weight(1f), onWirdClick
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
                        Text(if (TtsManager.isSpeaking) "إيقاف" else "اسمع الروبوت")
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
        shape = RoundedCornerShape(18.dp)
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
