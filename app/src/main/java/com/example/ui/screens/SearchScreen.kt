package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.example.data.Ayah
import com.example.data.HisnChapter
import com.example.data.HisnItem
import com.example.data.HisnRepository
import com.example.data.QuranRepository
import com.example.data.TtsManager
import com.example.util.FavKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(0) } // 0: الكل، 1: القرآن، 2: الأذكار

    val quranResults: List<Ayah> = remember(query, mode, QuranRepository.isLoaded) {
        if (query.trim().length < 2 || mode == 2) emptyList()
        else {
            QuranRepository.ensureLoaded(context)
            QuranRepository.search(context, query, 150)
        }
    }
    val hisnResults: List<Pair<HisnChapter, HisnItem>> = remember(query, mode, HisnRepository.isLoaded) {
        if (query.trim().length < 2 || mode == 1) emptyList()
        else {
            HisnRepository.ensureLoaded(context)
            HisnRepository.search(context, query, 150)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البحث الشامل", fontWeight = FontWeight.Bold) },
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
                placeholder = { Text("ابحث في القرآن والأذكار…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "القرآن", "الأذكار").forEachIndexed { index, label ->
                    FilterChip(
                        selected = mode == index,
                        onClick = { mode = index },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (query.trim().length < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "اكتب حرفين على الأقل للبحث\nفي 6236 آية و291 ذكرًا",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (quranResults.isNotEmpty()) {
                    item {
                        Text(
                            "القرآن (${quranResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(quranResults, key = { "q-${it.key}" }) { ayah ->
                        val favKey = FavKeys.ayah(ayah.surah, ayah.number)
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(ayah.text, fontSize = 17.sp, lineHeight = 30.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "سورة ${ayah.surah} • آية ${ayah.number}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { TtsManager.toggle(ayah.text) }) {
                                        Icon(
                                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "قراءة"
                                        )
                                    }
                                    IconButton(onClick = { onToggleFavorite(favKey) }) {
                                        Icon(
                                            if (favorites.contains(favKey)) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "مفضلة"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (hisnResults.isNotEmpty()) {
                    item {
                        Text(
                            "الأذكار (${hisnResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(hisnResults.size) { index ->
                        val (chapter, item) = hisnResults[index]
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    chapter.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.text, fontSize = 16.sp, lineHeight = 28.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { TtsManager.toggle(item.text) }) {
                                        Icon(
                                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "قراءة"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (quranResults.isEmpty() && hisnResults.isEmpty()) {
                    item {
                        Text(
                            "لا توجد نتائج مطابقة",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
