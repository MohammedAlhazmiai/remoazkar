package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AzkarRepeatStore
import com.example.data.HisnRepository
import com.example.data.TtsManager
import com.example.data.tashkeelText
import com.example.ui.components.SmartReaderBus
import com.example.ui.components.buildReadableText
import com.example.util.FavKeys
import com.example.util.ShareUtil
import com.example.util.VibrationHelper

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
    val chapter = remember(chapterId, HisnRepository.isLoaded) {
        HisnRepository.ensureLoaded(context)
        HisnRepository.chapter(chapterId)
    }
    val counters = remember(chapterId) { mutableStateMapOf<Int, Int>() }
    // أعداد التكرار المعدّلة من المستخدم (فوق القيم الافتراضية في الملف)
    val repeatOverrides = remember(chapterId) { mutableStateMapOf<Int, Int>() }
    var editingIndex by remember { mutableIntStateOf(-1) }

    // نشر نص الباب للقارئ الذكي (الزر العائم)
    LaunchedEffect(chapter?.id, chapter?.items?.size) {
        val c = chapter ?: return@LaunchedEffect
        SmartReaderBus.publish(
            c.title,
            buildReadableText(c.title, c.items.map { it.text })
        )
    }

    var localFontIndex by remember { mutableIntStateOf(fontSizeIndex) }
    val textSize = when (localFontIndex) {
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

    // عدّ نقرة واحدة على بطاقة الذكر، مع الاهتزاز عند إتمام العدد.
    fun increment(index: Int, target: Int) {
        val current = counters[index] ?: 0
        if (current + 1 >= target) {
            counters[index] = 0
            VibrationHelper.completed(context)
        } else {
            counters[index] = current + 1
        }
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
            item {
                com.example.ui.components.ReadingFontSizeRow(
                    selectedIndex = localFontIndex,
                    onSelect = { localFontIndex = it }
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "اضغط في أي مكان داخل بطاقة الذكر لعدّ التكرار، ويمكنك تعديل العدد من أيقونة التعديل ✎. يهتز الهاتف عند إتمام العدد.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            itemsIndexed(chapter.items) { index, item ->
                val favKey = FavKeys.hisn(chapter.id, index)
                val isFav = favorites.contains(favKey)
                val count = counters[index] ?: 0
                val target = repeatOverrides[index]
                    ?: AzkarRepeatStore.effective(context, chapter.id, index, item.repeat)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { increment(index, target) },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = tashkeelText(item.text),
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
                            AssistChip(
                                onClick = { increment(index, target) },
                                label = { Text("$count / $target") }
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { editingIndex = index }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل عدد التكرار",
                                    tint = MaterialTheme.colorScheme.primary
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

    if (editingIndex >= 0 && editingIndex < chapter.items.size) {
        val item = chapter.items[editingIndex]
        val initial = repeatOverrides[editingIndex]
            ?: AzkarRepeatStore.effective(context, chapter.id, editingIndex, item.repeat)
        RepeatCountDialog(
            initial = initial,
            defaultValue = item.repeat,
            onDismiss = { editingIndex = -1 },
            onSave = { value ->
                AzkarRepeatStore.set(context, chapter.id, editingIndex, value)
                repeatOverrides[editingIndex] = value
                counters[editingIndex] = 0
                editingIndex = -1
            },
            onReset = {
                AzkarRepeatStore.reset(context, chapter.id, editingIndex)
                repeatOverrides.remove(editingIndex)
                counters[editingIndex] = 0
                editingIndex = -1
            }
        )
    }
}

@Composable
private fun RepeatCountDialog(
    initial: Int,
    defaultValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onReset: () -> Unit
) {
    var valueText by remember { mutableStateOf(initial.toString()) }
    val parsed = valueText.toIntOrNull()?.coerceIn(1, 10_000) ?: 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عدد تكرار الذكر") },
        text = {
            Column {
                Text(
                    "حدّد عدد المرات التي تريد عدّها لهذا الذكر.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(onClick = {
                        val next = (parsed - 1).coerceAtLeast(1)
                        valueText = next.toString()
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "إنقاص")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) valueText = input
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedIconButton(onClick = {
                        val next = (parsed + 1).coerceAtMost(10_000)
                        valueText = next.toString()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "زيادة")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "القيمة الافتراضية لهذا الذكر: $defaultValue",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(parsed) }) { Text("حفظ") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("الافتراضي") }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )
}
