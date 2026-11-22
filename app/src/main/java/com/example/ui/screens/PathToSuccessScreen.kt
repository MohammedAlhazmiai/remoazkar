package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PathPassage
import com.example.data.PathToSuccessArticles
import com.example.data.PathToSuccessData
import com.example.data.TtsManager
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathToSuccessScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf(PathToSuccessData.sections.first().id) }
    val currentSection = PathToSuccessData.sections.firstOrNull { it.id == selectedId }
        ?: PathToSuccessData.sections.first()
    val passages = remember(currentSection.id) { PathToSuccessArticles.passagesFor(currentSection.id) }
    // دمج النص المُرسل داخل الصفحة نفسها: مقدمة ← محطات ← آية ← نصوص موثقة ← حديث ← رسالة ختامية
    val closingPassage = passages.lastOrNull()?.takeIf { it.heading.contains("ختامية") }
    val bodyPassages = if (closingPassage != null) passages.dropLast(1) else passages
    val introPassage = bodyPassages.firstOrNull()
    val stationPassages = bodyPassages.drop(1)

    // النص الكامل المدمج (المحتوى التحريري الجديد + النصوص الموثقة القديمة) يُستخدم للمشاركة والقراءة الصوتية.
    fun buildFullText(): String {
        val articleText = passages.joinToString("\n\n") { passage ->
            if (passage.heading.isBlank()) passage.body else "${passage.heading}\n${passage.body}"
        }
        val paragraphSources = currentSection.paragraphs.joinToString("\n") {
            "المصدر: ${it.source} — ${it.url}"
        }
        val hadithSource =
            "\n\nالحديث: ${currentSection.hadith.text}\nالمصدر: ${currentSection.hadith.source} — ${currentSection.hadith.url}"
        return "${currentSection.title}\n\n${currentSection.subtitle}\n\n${currentSection.summary}\n\n" +
            articleText +
            "\n\nالآية (${currentSection.quranReference}): ${currentSection.quranAyah}" +
            "\nالمصدر: ${currentSection.quranUrl}\n\n" +
            currentSection.paragraphs.joinToString("\n\n") { it.text } +
            "\n\n$paragraphSources$hadithSource"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طريق الناجحين", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { ShareUtil.share(context, buildFullText()) }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = PathToSuccessData.sections.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                PathToSuccessData.sections.forEach { section ->
                    Tab(
                        selected = section.id == selectedId,
                        onClick = { selectedId = section.id },
                        text = {
                            Text(
                                text = section.title.substringAfter(". ", section.title),
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (section.id == selectedId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "hero-${currentSection.id}") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF174B3B))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = currentSection.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD36A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentSection.subtitle,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = currentSection.summary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { TtsManager.toggle(buildFullText()) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFFD36A),
                                    contentColor = Color(0xFF123B31)
                                )
                            ) {
                                Icon(
                                    if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (TtsManager.isSpeaking) "إيقاف القراءة" else "استمع للموضوع صوتياً")
                            }
                        }
                    }
                }

                // مقدمة الصفحة المدمجة
                introPassage?.let { passage ->
                    item(key = "intro-${currentSection.id}") {
                        ArticlePassageCard(passage)
                    }
                }

                // محطات الرحلة المدمجة داخل الصفحة نفسها
                items(
                    items = stationPassages,
                    key = { "passage-${currentSection.id}-${it.heading.hashCode()}-${it.body.hashCode()}" }
                ) { passage ->
                    ArticlePassageCard(passage)
                }

                item(key = "ayah-${currentSection.id}") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "﴿ ${currentSection.quranAyah} ﴾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 28.sp
                            )
                            SourceLink(
                                label = "المصدر: القرآن الكريم — ${currentSection.quranReference}",
                                url = currentSection.quranUrl
                            )
                        }
                    }
                }

                items(
                    items = currentSection.paragraphs,
                    key = { "${currentSection.id}-${it.source.hashCode()}-${it.text.hashCode()}" }
                ) { paragraph ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = paragraph.text,
                                fontSize = 16.sp,
                                lineHeight = 30.sp,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            SourceLink(label = "المصدر: ${paragraph.source}", url = paragraph.url)
                        }
                    }
                }

                item(key = "hadith-${currentSection.id}") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "حديث نبوي موثق",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = currentSection.hadith.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 24.sp
                            )
                            SourceLink(
                                label = "المصدر والرقم: ${currentSection.hadith.source}",
                                url = currentSection.hadith.url
                            )
                        }
                    }
                }
                closingPassage?.let { passage ->
                    item(key = "closing-${currentSection.id}") {
                        ArticlePassageCard(passage)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticlePassageCard(passage: PathPassage) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (passage.heading.isNotBlank()) {
                Text(
                    text = passage.heading,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = passage.body,
                fontSize = 16.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (passage.source.isNotBlank()) {
                SourceLink(label = "المصدر: ${passage.source}", url = passage.url)
            }
        }
    }
}

@Composable
private fun SourceLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable { if (url.isNotBlank()) runCatching { uriHandler.openUri(url) } },
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center
    )
}
