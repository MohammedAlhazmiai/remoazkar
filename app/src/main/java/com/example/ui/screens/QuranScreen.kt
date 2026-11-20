package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranRepository
import com.example.data.TtsManager

private const val BUNDLED_MUSHAF_PAGE_COUNT = 569

private fun approximatePdfPage(standardPage: Int): Int =
    (((standardPage - 1).coerceAtLeast(0).toDouble() * (BUNDLED_MUSHAF_PAGE_COUNT - 1)) / 603.0)
        .toInt().coerceIn(1, BUNDLED_MUSHAF_PAGE_COUNT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    onSurahClick: (Int) -> Unit,
    onPageClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    val surahs = remember(QuranRepository.isLoaded) {
        QuranRepository.ensureLoaded(context)
        QuranRepository.surahs.toList()
    }

    val filtered = remember(query, surahs) {
        if (query.isBlank()) surahs
        else surahs.filter {
            it.name.contains(query) || it.englishName.contains(query, true) ||
                it.number.toString() == query.trim()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("القرآن الكريم كاملًا", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "بحث في الآيات")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("السور", "المصحف PDF", "الأجزاء", "الأحزاب", "تلاوة 604 صفحة")
                tabs.forEachIndexed { index, label ->
                    FilterChip(
                        selected = mode == index,
                        onClick = { mode = index },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            when (mode) {
                0 -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("ابحث باسم السورة أو رقمها") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text(
                        text = "114 سورة • 6236 آية • ملف المصحف المرفق: 569 صفحة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.number }) { surah ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSurahClick(surah.number) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = surah.number.toString(),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(surah.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = "${surah.revelationType} • ${surah.ayahCount} آية",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            TtsManager.toggle(
                                                QuranRepository.ayahsOf(surah.number)
                                                    .joinToString(separator = " ") { it.text }
                                            )
                                        }
                                    ) {
                                        Icon(
                                            if (TtsManager.isSpeaking) Icons.Default.Stop
                                            else Icons.Default.VolumeUp,
                                            contentDescription = "اقرأ سورة ${surah.name} بصوت الجهاز",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Text(
                        text = "اختر صفحة من ملف المصحف (1 - 569)",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems((1..BUNDLED_MUSHAF_PAGE_COUNT).toList()) { page ->
                            NumberTile(page.toString()) { onPageClick(page) }
                        }
                    }
                }

                2 -> {
                    Text(
                        text = "الأجزاء الثلاثون",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..30).toList()) { juz ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .clickable { onPageClick(approximatePdfPage(QuranRepository.pageOfJuz(juz))) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الجزء $juz", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "صفحة تقريبية ${approximatePdfPage(QuranRepository.pageOfJuz(juz))}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Text(
                        text = "الأحزاب (240 حزبًا)",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gridItems((1..240).toList()) { hizb ->
                            NumberTile(hizb.toString()) {
                                onPageClick(approximatePdfPage(QuranRepository.pageOfHizb(hizb)))
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = "اضغط رقم الصفحة لقراءة نصها من بيانات القرآن المحلية (604 صفحات). الصوت من محرك الجهاز وليس تسجيل تلاوة بشرية.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems((1..QuranRepository.pageCount()).toList()) { page ->
                            NumberTile(page.toString()) {
                                val pageText = QuranRepository.byPage(page).joinToString(" ") { it.text }
                                if (pageText.isNotBlank()) TtsManager.toggle(pageText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberTile(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
