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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.data.QuranReaderPrefs
import com.example.data.QuranRepository
import com.example.data.TtsManager
import kotlinx.coroutines.launch

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
    onBackClick: () -> Unit,
    onResumeClick: (Int, Int) -> Unit = { _, page -> onPageClick(page) }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesManager(context) }
    val readerPrefs by prefs.quranReaderPrefsFlow.collectAsState(initial = QuranReaderPrefs())
    var mode by remember { mutableIntStateOf(0) }
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

    val parsedBookmarks = remember(readerPrefs.bookmarks) {
        readerPrefs.bookmarks.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            val page = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val label = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "صفحة $page"
            page to label
        }.sortedBy { it.first }
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
                val bookmarkTabLabel = if (parsedBookmarks.isEmpty()) "العلامات" else "العلامات (${parsedBookmarks.size})"
                val tabs = listOf("السور", "المصحف PDF", "الأجزاء", "الأحزاب", bookmarkTabLabel, "تلاوة 604 صفحة")
                tabs.forEachIndexed { index, label ->
                    FilterChip(
                        selected = mode == index,
                        onClick = { mode = index },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            if (readerPrefs.lastPdfPage > 0) {
                val lastSurahName = remember(readerPrefs.lastSurah, surahs) {
                    surahs.firstOrNull { it.number == readerPrefs.lastSurah }?.name
                }
                val resumeSubtitle = buildString {
                    if (!lastSurahName.isNullOrBlank()) {
                        append(lastSurahName)
                        append(" • ")
                    }
                    append("صفحة ${readerPrefs.lastPdfPage}")
                    if (readerPrefs.textMode) append(" (النص المكبّر)")
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onResumeClick(readerPrefs.lastSurah, readerPrefs.lastPdfPage) }
                        .testTag("quran_continue_reading_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "متابعة آخر قراءة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = resumeSubtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(
                            onClick = { onResumeClick(readerPrefs.lastSurah, readerPrefs.lastPdfPage) },
                            modifier = Modifier.testTag("quran_continue_reading_button")
                        ) {
                            Text("متابعة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            when (mode) {
                0 -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("quran_surah_search_input"),
                        placeholder = { Text("ابحث باسم السورة أو رقمها") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.testTag("quran_surah_search_clear")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text(
                        text = "114 سورة • 6236 آية • مصحف مكبّر مع وضع ملء الشاشة",
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

                4 -> {
                    if (parsedBookmarks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "لا توجد علامات مرجعية بعد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "اضغط على أيقونة العلامة المرجعية أثناء القراءة لحفظ الصفحة والعودة إليها سريعًا.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parsedBookmarks, key = { it.first }) { (page, label) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPageClick(page) }
                                        .testTag("quran_bookmark_item_$page"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "اضغط للانتقال إلى صفحة $page",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch { prefs.toggleQuranBookmark(page, label) }
                                            },
                                            modifier = Modifier.testTag("quran_bookmark_remove_$page")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف العلامة المرجعية",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
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
