package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisnChapter
import com.example.data.HisnRepository
import com.example.data.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisnScreen(
    onChapterClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val chapters = remember(HisnRepository.isLoaded) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.chapters.toList()
    }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, chapters) {
        if (query.isBlank()) chapters
        else {
            val q = HisnRepository.normalize(query)
            chapters.filter {
                HisnRepository.normalize(it.title).contains(q) ||
                    it.items.any { item -> HisnRepository.normalize(item.text).contains(q) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حصن المسلم كاملًا", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("ابحث في 132 بابًا من الأذكار…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "عدد الأبواب: ${filtered.size} • عدد الأذكار: ${HisnRepository.totalItems()}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { chapter ->
                    HisnChapterRow(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter.id) },
                        onSpeak = { TtsManager.speak(chapter.items.joinToString("\n\n") { it.text }) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HisnChapterRow(
    chapter: HisnChapter,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${chapter.items.size} ذكرًا • ${chapter.totalRepeats} تكرارًا",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chapter.preview.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = chapter.preview,
                        fontSize = 13.sp,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onSpeak) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "قراءة الباب",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
