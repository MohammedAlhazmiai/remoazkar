package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomAzkarStore
import com.example.data.HisnRepository
import com.example.data.QuranRepository
import com.example.data.TtsManager

private data class FavEntry(
    val key: String,
    val label: String,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val entries = remember(favorites, QuranRepository.isLoaded, HisnRepository.isLoaded) {
        QuranRepository.ensureLoaded(context)
        HisnRepository.ensureLoaded(context)
        val out = ArrayList<FavEntry>()
        favorites.forEach { key ->
            when {
                key.startsWith("ayah:") -> {
                    val parts = key.removePrefix("ayah:").split(":")
                    val s = parts.getOrNull(0)?.toIntOrNull()
                    val n = parts.getOrNull(1)?.toIntOrNull()
                    if (s != null && n != null) {
                        val ayah = QuranRepository.ayahsOf(s).firstOrNull { it.number == n }
                        if (ayah != null) {
                            out.add(FavEntry(key, "سورة $s • آية $n", ayah.text))
                        }
                    }
                }
                key.startsWith("zikr:") -> {
                    val parts = key.removePrefix("zikr:").split(":")
                    val c = parts.getOrNull(0)?.toIntOrNull()
                    val i = parts.getOrNull(1)?.toIntOrNull()
                    if (c != null && i != null) {
                        val chapter = HisnRepository.chapter(c)
                        val item = chapter?.items?.getOrNull(i)
                        if (chapter != null && item != null) {
                            out.add(FavEntry(key, chapter.title, item.text))
                        }
                    }
                }
                key.startsWith("custom:") -> {
                    val id = key.removePrefix("custom:").toLongOrNull()
                    val zikr = CustomAzkarStore.load(context).firstOrNull { it.id == id }
                    if (zikr != null) out.add(FavEntry(key, zikr.title, zikr.text))
                }
            }
        }
        out
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المفضلة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد عناصر في المفضلة بعد\nاضغط النجمة بجانب أي آية أو ذكر.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries.size) { index ->
                    val entry = entries[index]
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                entry.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(entry.text, fontSize = 16.sp, lineHeight = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { TtsManager.toggle(entry.text) }) {
                                    Icon(
                                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "قراءة"
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { onToggleFavorite(entry.key) }) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "إزالة من المفضلة",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
