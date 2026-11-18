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
import com.example.data.RuqyahCondition
import com.example.data.RuqyahConditionsData
import com.example.data.RuqyahData
import com.example.data.RuqyahItem
import com.example.data.TtsManager
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuqyahScreen(
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val conditionTabLabel = "حسب الحالة"
    val categories = remember { RuqyahData.categories() + conditionTabLabel }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isConditions = selectedTab == categories.lastIndex
    val category = if (isConditions) "" else categories[selectedTab]
    val list = remember(category, isConditions) {
        if (isConditions) emptyList() else RuqyahData.byCategory(category)
    }
    var expandedId by remember { mutableIntStateOf(-1) }

    val textSize = when (fontSizeIndex) {
        0 -> 16.sp
        2 -> 24.sp
        else -> 20.sp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الرقية الشرعية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${RuqyahData.items.size} نصًّا ثابتًا", fontSize = 11.sp)
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
                            else TtsManager.speak(
                                if (isConditions)
                                    RuqyahConditionsData.conditions.joinToString(" ") {
                                        RuqyahConditionsData.fullText(it)
                                    }
                                else RuqyahData.fullText(category)
                            )
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
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            if (isConditions) "اختر حالتك لعرض النصوص والإرشادات المناسبة"
                            else "اضغط على أي نص ليقرأه الروبوت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            if (isConditions) "مع إمكانية قراءة الحالة كاملة بالروبوت"
                            else "أو استخدم زر التشغيل أعلى الشاشة لقراءة القسم كاملًا",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            com.example.ui.components.ReadingFontSizeRow(
                selectedIndex = localFontIndex,
                onSelect = { localFontIndex = it },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConditions) {
                    items(RuqyahConditionsData.conditions, key = { it.id }) { condition ->
                        ConditionCard(
                            condition = condition,
                            expanded = expandedId == condition.id,
                            textSize = textSize,
                            onToggleExpand = {
                                expandedId = if (expandedId == condition.id) -1 else condition.id
                            },
                            onSpeak = { TtsManager.toggle(RuqyahConditionsData.fullText(condition)) }
                        )
                    }
                } else {
                    items(list, key = { it.id }) { item ->
                        RuqyahCard(
                            item = item,
                            expanded = expandedId == item.id,
                            textSize = textSize,
                            onToggleExpand = {
                                expandedId = if (expandedId == item.id) -1 else item.id
                            },
                            onSpeak = {
                                val speakText = if (item.repeat > 1)
                                    "${item.title}. ${item.text} تُقرأ ${item.repeat} مرات."
                                else "${item.title}. ${item.text}"
                                TtsManager.toggle(speakText)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuqyahCard(
    item: RuqyahItem,
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
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (item.repeat > 1) {
                        Text(
                            "تُقرأ ${item.repeat} مرات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                        item.text,
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
                        TextButton(onClick = { ShareUtil.share(context, "${item.title}\n\n${item.text}\n\n${item.source}", item.title) }) {
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

@Composable
private fun ConditionCard(
    condition: RuqyahCondition,
    expanded: Boolean,
    textSize: androidx.compose.ui.unit.TextUnit,
    onToggleExpand: () -> Unit,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    val related = remember(condition.id) {
        condition.itemIds.mapNotNull { RuqyahData.item(it) }
    }
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
                        condition.id.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    condition.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSpeak) {
                    Icon(
                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "قراءة الحالة",
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
                        condition.description,
                        fontSize = textSize,
                        lineHeight = (textSize.value * 1.8f).sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "النصوص المقترحة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    related.forEach { item ->
                        Text(
                            "• ${item.title}",
                            fontSize = (textSize.value - 3f).sp,
                            lineHeight = (textSize.value * 1.5f).sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        condition.advice,
                        fontSize = (textSize.value - 2f).sp,
                        lineHeight = (textSize.value * 1.6f).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onSpeak) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("اقرأ الحالة بالروبوت", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                ShareUtil.share(
                                    context,
                                    RuqyahConditionsData.fullText(condition),
                                    condition.title
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
