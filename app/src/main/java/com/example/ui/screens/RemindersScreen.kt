package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
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
import com.example.data.Reminder
import com.example.data.ReminderScheduler
import com.example.data.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(ReminderScheduler.load(context)) }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    fun reload() {
        items = ReminderScheduler.load(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تذكيراتي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("تذكير جديد") }
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
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "تذكير الجمعة بسورة الكهف (9:00 ص) وتذكير الملك كل ليلة (9:30 م) يعملان تلقائيًا.",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                TtsManager.toggle(
                                    "تذكير الجمعة بسورة الكهف الساعة التاسعة صباحًا، " +
                                        "وتذكير الملك كل ليلة الساعة التاسعة والنصف مساءً، يعملان تلقائيًا."
                                )
                            }
                        ) {
                            Icon(
                                if (TtsManager.isSpeaking) Icons.Default.Stop
                                else Icons.Default.VolumeUp,
                                contentDescription = "اسمع التذكيرات التلقائية"
                            )
                        }
                    }
                }
            }

            items(items, key = { it.id }) { reminder ->
                val speakText = buildString {
                    append("${reminder.title}. ")
                    append("الوقت ${reminder.timeLabel()}، ${reminder.daysLabel()}. ")
                    if (reminder.body.isNotBlank()) append(reminder.body)
                }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "${reminder.timeLabel()} • ${reminder.daysLabel()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { TtsManager.toggle(speakText) }) {
                                Icon(
                                    if (TtsManager.isSpeaking) Icons.Default.Stop
                                    else Icons.Default.VolumeUp,
                                    contentDescription = "اسمع التذكير",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { checked ->
                                    ReminderScheduler.upsert(context, reminder.copy(enabled = checked))
                                    reload()
                                }
                            )
                        }
                        if (reminder.body.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                reminder.body,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            IconButton(onClick = {
                                editing = reminder
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل")
                            }
                            IconButton(onClick = {
                                ReminderScheduler.remove(context, reminder.id)
                                reload()
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ReminderEditorDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { reminder ->
                ReminderScheduler.upsert(context, reminder)
                reload()
                showDialog = false
            }
        )
    }
}

@Composable
private fun ReminderEditorDialog(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var hour by remember { mutableStateOf(initial?.hour ?: 8) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }
    var daysMask by remember { mutableStateOf(initial?.daysMask ?: 0) }

    val dayNames = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تذكير جديد" else "تعديل التذكير") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("نص التذكير") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("الوقت: ${String.format("%02d:%02d", hour, minute)}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("−س") }
                    Spacer(Modifier.width(6.dp))
                    Text("$hour", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+س") }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { minute = (minute + 55) % 60 }) { Text("−د") }
                    Spacer(Modifier.width(6.dp))
                    Text("$minute", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+د") }
                }
                Spacer(Modifier.height(10.dp))
                Text("الأيام (اتركها فارغة لكل يوم)", fontSize = 12.sp)
                Row {
                    dayNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = daysMask and (1 shl index) != 0,
                            onClick = { daysMask = daysMask xor (1 shl index) },
                            label = { Text(name, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = initial ?: Reminder(
                        id = (System.currentTimeMillis() % 100000).toInt(),
                        title = title,
                        body = body,
                        hour = hour,
                        minute = minute,
                        daysMask = daysMask,
                        enabled = true
                    )
                    onSave(
                        base.copy(
                            title = title,
                            body = body,
                            hour = hour,
                            minute = minute,
                            daysMask = daysMask,
                            enabled = true
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
