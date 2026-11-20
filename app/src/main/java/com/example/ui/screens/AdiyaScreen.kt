package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdiyaData
import com.example.data.DuaItem
import com.example.data.TtsManager
import com.example.data.tashkeelText
import com.example.ui.components.SmartReaderBus
import com.example.util.ShareUtil

/**
 * شاشة الأدعية المستقلة: جوامع الدعاء، الأدعية القرآنية، أدعية الأنبياء، الأربعون النبوية.
 * تحتوي على تبويات، قراءة آلية بالروبوت لكل نص، مشاركة، والتحكم بحجم الخط (صغير/متوسط/كبير).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdiyaScreen(
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val categories = remember { AdiyaData.categories() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val category = categories[selectedTab]
    val list = remember(category) { AdiyaData.byCategory(category) }
    var expandedId by remember { mutableIntStateOf(-1) }

    // نشر نص القسم الحالي للقارئ الذكي
    LaunchedEffect(selectedTab) {
        val body = list.joinToString("\n\n") { "${it.title}. ${it.text}" }
        if (body.isNotBlank()) {
            SmartReaderBus.publish("الأدعية النبوية — $category", body)
        }
    }
    var localFontIndex by remember { mutableIntStateOf(fontSizeIndex) }

    val textSize = when (localFontIndex) {
        0 -> 16.sp
        2 -> 24.sp
        else -> 20.sp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الأدعية والأربعون النبوية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${AdiyaData.items.size} نصًّا مأثورًا", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (TtsManager.isSpeaking) TtsManager.stop()
                            else TtsManager.speak(AdiyaData.fullText(category))
                        }
                    ) {
                        Icon(
                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "اقرأ القسم كاملًا"
                        )
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
            TabRow(selectedTabIndex = selectedTab) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index; expandedId = -1 },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // شريط التحكم بحجم الخط داخل شاشة القراءة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("حجم الخط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                listOf("صغير", "متوسط", "كبير").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = localFontIndex == idx,
                        onClick = { localFontIndex = idx },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "اضغط على أي دعاء لقراءته بالروبوت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "أو استخدم زر التشغيل أعلى الشاشة لقراءة القسم كاملًا",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    AdiyaCard(
                        item = item,
                        expanded = expandedId == item.id,
                        textSize = textSize,
                        onToggleExpand = {
                            expandedId = if (expandedId == item.id) -1 else item.id
                        },
                        onSpeak = { TtsManager.toggle("${item.title}. ${item.text}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdiyaCard(
    item: DuaItem,
    expanded: Boolean,
    textSize: androidx.compose.ui.unit.TextUnit,
    onToggleExpand: () -> Unit,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item.id.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSpeak) {
                    Icon(
                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "قراءة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tashkeelText(item.text),
                        fontSize = textSize,
                        lineHeight = (textSize.value * 1.9f).sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.source.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            item.source,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onSpeak) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("اقرأ بالروبوت", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                ShareUtil.share(
                                    context,
                                    "${item.title}\n\n${item.text}\n\n${item.source}",
                                    item.title
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
