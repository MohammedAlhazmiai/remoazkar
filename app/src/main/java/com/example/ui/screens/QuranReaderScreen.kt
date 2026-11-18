package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ayah
import com.example.data.QuranRepository
import com.example.data.TtsManager
import com.example.util.FavKeys
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    pageNumber: Int,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val ayahs = remember(surahNumber, pageNumber, QuranRepository.isLoaded) {
        QuranRepository.ensureLoaded(context)
        if (surahNumber > 0) QuranRepository.ayahsOf(surahNumber)
        else QuranRepository.byPage(pageNumber)
    }
    val surah = remember(surahNumber, QuranRepository.isLoaded) {
        if (surahNumber > 0) QuranRepository.surah(surahNumber) else null
    }

    var localFontIndex by remember { mutableIntStateOf(fontSizeIndex) }
    val textSize = when (localFontIndex) {
        0 -> 18.sp
        2 -> 28.sp
        else -> 23.sp
    }

    val title = surah?.name ?: "صفحة $pageNumber"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            text = if (surah != null)
                                "${surah.revelationType} • ${surah.ayahCount} آية"
                            else "صفحة $pageNumber من المصحف",
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val full = ayahs.joinToString(" ") { it.text }
                        TtsManager.toggle(full)
                    }) {
                        Icon(
                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "قراءة كاملة"
                        )
                    }
                    IconButton(onClick = {
                        ShareUtil.share(
                            context,
                            title + "\n\n" + ayahs.joinToString("\n") { "${it.text} (${it.surah}:${it.number})" }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ayahs, key = { it.key }) { ayah ->
                val favKey = FavKeys.ayah(ayah.surah, ayah.number)
                val isFav = favorites.contains(favKey)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = ayah.text,
                            fontSize = textSize,
                            lineHeight = textSize * 1.9f,
                            textAlign = TextAlign.Justify
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${ayah.surah}:${ayah.number}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "جزء ${ayah.juz} • صفحة ${ayah.page}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { TtsManager.toggle(ayah.text) }) {
                                Icon(
                                    if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "قراءة الآية",
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
                                ShareUtil.share(context, "${ayah.text}\n\n[${ayah.surah}:${ayah.number}] — ريمواذكار")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة")
                            }
                        }
                    }
                }
            }
        }
    }
}
