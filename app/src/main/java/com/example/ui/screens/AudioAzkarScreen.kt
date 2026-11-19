package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.HisnChapter
import com.example.data.HisnRepository
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
                        Icon(Icons.Default.Stop, contentDescription = "إيقاف القراءة")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    "يُنطق النص بواسطة محرّك الصوت العربي المثبّت على جهازك. هذه قراءة آلية وليست تسجيلًا لقارئ بشري، وقد تختلف جودتها بحسب الجهاز.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (chapters.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("تعذّر تحميل الأذكار الصوتية.")
                }
            } else {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        FilterChip(
                            selected = chapter.id == selected?.id,
                            onClick = { selectedId = chapter.id },
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
                            startBackgroundPlayback(context, chapter.title, text)
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
                                        Text("التكرار: ${item.repeat} مرات", modifier = Modifier.padding(top = 8.dp),
                                            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    if (item.footnote.isNotBlank()) {
                                        Text(item.footnote, modifier = Modifier.padding(top = 6.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { startBackgroundPlayback(context, chapter.title, item.text) }) {
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
}

private fun startBackgroundPlayback(context: Context, title: String, text: String) {
    val intent = Intent(context, AudioAzkarPlaybackService::class.java).apply {
        putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, title)
        putExtra(AudioAzkarPlaybackService.EXTRA_TEXT, text)
    }
    runCatching { ContextCompat.startForegroundService(context, intent) }
        .onFailure { android.widget.Toast.makeText(context, "تعذّر بدء الصوت في الخلفية: ${it.localizedMessage}", android.widget.Toast.LENGTH_LONG).show() }
}

private fun stopBackgroundPlayback(context: Context) {
    context.stopService(Intent(context, AudioAzkarPlaybackService::class.java))
}
