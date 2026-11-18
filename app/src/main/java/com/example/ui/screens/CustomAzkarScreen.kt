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
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.data.CustomZikr
import com.example.data.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAzkarScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(CustomAzkarStore.load(context)) }
    var editing by remember { mutableStateOf<CustomZikr?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    fun reload() {
        items = CustomAzkarStore.load(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أذكاري الخاصة", fontWeight = FontWeight.Bold) },
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
                text = { Text("ذكر جديد") }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("لم تُضف أذكارًا خاصة بعد", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "أضف أذكارك وأدعيتك الخاصة وحدد عدد التكرار،\nوسيقرأها الروبوت بصوت فصيح.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { zikr ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(zikr.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(zikr.text, fontSize = 15.sp, lineHeight = 24.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "التكرار: ${zikr.repeat}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { TtsManager.toggle(zikr.text) }) {
                                    Icon(
                                        if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "قراءة"
                                    )
                                }
                                IconButton(onClick = {
                                    editing = zikr
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                }
                                IconButton(onClick = {
                                    CustomAzkarStore.delete(context, zikr.id)
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
    }

    if (showDialog) {
        ZikrEditorDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { title, text, repeat ->
                val base = editing
                val zikr = if (base == null)
                    CustomZikr(CustomAzkarStore.newId(), title, text, repeat)
                else base.copy(title = title, text = text, repeat = repeat)
                CustomAzkarStore.upsert(context, zikr)
                reload()
                showDialog = false
            }
        )
    }
}

@Composable
private fun ZikrEditorDialog(
    initial: CustomZikr?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var repeat by remember { mutableStateOf(initial?.repeat ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ذكر جديد" else "تعديل الذكر") },
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
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("نص الذكر") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("التكرار: $repeat")
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { if (repeat > 1) repeat-- }) { Text("−") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { if (repeat < 1000) repeat++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank() && text.isNotBlank()) onSave(title, text, repeat) },
                enabled = title.isNotBlank() && text.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
