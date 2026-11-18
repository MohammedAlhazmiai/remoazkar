package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisnRepository
import com.example.data.TtsManager
import com.example.util.FavKeys
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisnChapterScreen(
    chapterId: Int,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val chapter = remember(chapterId) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.chapter(chapterId)
    }
    val counters = remember(chapterId) { mutableStateMapOf<Int, Int>() }

    val textSize = when (fontSizeIndex) {
        0 -> 16.sp
        2 -> 24.sp
        else -> 20.sp
    }

    if (chapter == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على الباب")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        chapter.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        TtsManager.toggle(chapter.items.joinToString("\n\n") { it.text })
                    }) {
                        Icon(
                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "قراءة الباب كاملًا"
                        )
                    }
                    IconButton(onClick = {
                        ShareUtil.share(
                            context,
                            chapter.title + "\n\n" + chapter.items.joinToString("\n\n") { it.text }
                        )
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
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
            itemsIndexed(chapter.items) { index, item ->
                val favKey = FavKeys.hisn(chapter.id, index)
                val isFav = favorites.contains(favKey)
                val count = counters[index] ?: 0
                val target = item.repeat

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = item.text,
                            fontSize = textSize,
                            lineHeight = textSize * 1.7f
                        )
                        if (item.footnote.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = item.footnote,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { TtsManager.toggle(item.text) }) {
                                Icon(
                                    if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "قراءة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onToggleFavorite(favKey) }) {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "مفضلة",
                                    tint = if (isFav) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                ShareUtil.share(context, item.text + "\n\n— ريمواذكار")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة")
                            }
                            Spacer(Modifier.weight(1f))
                            if (target > 1) {
                                AssistChip(
                                    onClick = {
                                        counters[index] = if (count >= target) 0 else count + 1
                                    },
                                    label = { Text("$count / $target") }
                                )
                            }
                        }
                        if (target > 1) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
