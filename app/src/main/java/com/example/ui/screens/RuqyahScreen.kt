package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConditionPassageKind
import com.example.data.ConditionRuqyahPassage
import com.example.data.RuqyahCondition
import com.example.data.RuqyahConditionsData
import com.example.data.RuqyahData
import com.example.data.RuqyahItem
import com.example.data.TtsManager
import com.example.data.tashkeelText
import com.example.ui.components.SmartReaderBus
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuqyahScreen(
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val conditionTabLabel = "حسب الحالة"
    val categories = remember { RuqyahData.categories() + conditionTabLabel }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isConditions = selectedTab == categories.lastIndex
    val category = if (isConditions) "" else categories[selectedTab]
    val list = remember(category, isConditions) {
        if (isConditions) emptyList() else RuqyahData.byCategory(category)
    }
    var expandedId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(selectedTab, isConditions) {
        expandedId = -1
        val body = if (isConditions)
            RuqyahConditionsData.conditions.joinToString("\n\n") { RuqyahConditionsData.fullText(it) }
        else list.joinToString("\n\n") { "${it.title}. ${it.text}" }
        if (body.isNotBlank()) {
            SmartReaderBus.publish(
                if (isConditions) "الرقية الشرعية — حسب الحالة" else "الرقية الشرعية — $category",
                body
            )
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
                        Text("الرقية الشرعية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (isConditions) "${RuqyahConditionsData.conditions.size} حالات • رقية كاملة من الكتاب والسنة"
                            else "${RuqyahData.items.size} نصًّا ثابتًا",
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("ruqyah_back_button")
                    ) {
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
                        },
                        modifier = Modifier.testTag("ruqyah_play_all_button")
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
                        modifier = Modifier.testTag("ruqyah_tab_$index"),
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
                            if (isConditions) "اختر حالتك لعرض الرقية كاملةً بالآيات والأدعية"
                            else "اضغط على أي نص ليقرأه الروبوت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            if (isConditions) "آيات قرآنية مضبوطة وأدعية نبوية صحيحة وطريقة العلاج الشرعية"
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
                            onSpeakAll = { TtsManager.toggle(RuqyahConditionsData.fullText(condition)) }
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
    textSize: TextUnit,
    onToggleExpand: () -> Unit,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ruqyah_card_${item.id}")
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
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
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

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
            ) {
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
                                ShareUtil.share(context, "${item.title}\n\n${item.text}\n\n${item.source}", item.title)
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

@Composable
private fun ConditionCard(
    condition: RuqyahCondition,
    expanded: Boolean,
    textSize: TextUnit,
    onToggleExpand: () -> Unit,
    onSpeakAll: () -> Unit
) {
    val context = LocalContext.current
    val quranPassages = remember(condition.id) { RuqyahConditionsData.quranPassages(condition) }
    val sunnahPassages = remember(condition.id) { RuqyahConditionsData.sunnahPassages(condition) }
    val methodPassages = remember(condition.id) { RuqyahConditionsData.methodPassages(condition) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("condition_card_${condition.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        condition.id.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        condition.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "${quranPassages.size} من القرآن • ${sunnahPassages.size} من السنة • طريقة الرقية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSpeakAll,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("condition_speak_${condition.id}")
                ) {
                    Icon(
                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "قراءة الحالة كاملة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "طي" else "توسيع"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            condition.description,
                            modifier = Modifier.padding(12.dp),
                            fontSize = (textSize.value - 2f).sp,
                            lineHeight = (textSize.value * 1.65f).sp
                        )
                    }

                    if (quranPassages.isNotEmpty()) {
                        ConditionSectionHeader(
                            title = "أولًا: الآيات القرآنية الكاملة للرقية (${quranPassages.size})",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        quranPassages.forEachIndexed { idx, passage ->
                            ConditionPassageCard(
                                index = idx + 1,
                                passage = passage,
                                textSize = textSize,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (sunnahPassages.isNotEmpty()) {
                        ConditionSectionHeader(
                            title = "ثانيًا: الأدعية والأذكار النبوية الصحيحة (${sunnahPassages.size})",
                            icon = Icons.Default.Verified,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        sunnahPassages.forEachIndexed { idx, passage ->
                            ConditionPassageCard(
                                index = idx + 1,
                                passage = passage,
                                textSize = textSize,
                                accentColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (methodPassages.isNotEmpty()) {
                        ConditionSectionHeader(
                            title = "ثالثًا: الهدي والطريقة الشرعية للعلاج",
                            icon = Icons.Default.Lightbulb,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        methodPassages.forEachIndexed { idx, passage ->
                            ConditionPassageCard(
                                index = idx + 1,
                                passage = passage,
                                textSize = textSize,
                                accentColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "خلاصة التوجيه الشرعي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                condition.advice,
                                fontSize = (textSize.value - 2f).sp,
                                lineHeight = (textSize.value * 1.65f).sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSpeakAll,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("condition_read_all_${condition.id}")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("قراءة كاملة", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                copyTextToClipboard(
                                    context,
                                    condition.title,
                                    RuqyahConditionsData.fullText(condition)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("condition_copy_${condition.id}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("نسخ", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                ShareUtil.share(
                                    context,
                                    RuqyahConditionsData.fullText(condition),
                                    condition.title
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("condition_share_${condition.id}")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
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
private fun ConditionSectionHeader(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ConditionPassageCard(
    index: Int,
    passage: ConditionRuqyahPassage,
    textSize: TextUnit,
    accentColor: Color
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        index.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        passage.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (passage.repeat > 1) {
                        Text(
                            "التكرار المشروع: ${passage.repeat} مرات",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
                IconButton(
                    onClick = {
                        val speakStr = if (passage.repeat > 1)
                            "${passage.title}. ${passage.text} تُقرأ ${passage.repeat} مرات."
                        else "${passage.title}. ${passage.text}"
                        TtsManager.toggle(speakStr)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "قراءة ${passage.title}",
                        tint = accentColor
                    )
                }
                IconButton(
                    onClick = {
                        copyTextToClipboard(
                            context,
                            passage.title,
                            "${passage.title}\n\n${passage.text}\n\n${passage.source}"
                        )
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "نسخ ${passage.title}",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (passage.kind == ConditionPassageKind.METHOD) passage.text else tashkeelText(passage.text),
                fontSize = if (passage.kind == ConditionPassageKind.METHOD) (textSize.value - 2f).sp else textSize,
                lineHeight = if (passage.kind == ConditionPassageKind.METHOD) (textSize.value * 1.7f).sp else (textSize.value * 1.95f).sp,
                fontWeight = if (passage.kind == ConditionPassageKind.QURAN) FontWeight.Medium else FontWeight.Normal
            )

            if (passage.source.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "المصدر: ${passage.source}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "تم نسخ النص بنجاح", Toast.LENGTH_SHORT).show()
}
