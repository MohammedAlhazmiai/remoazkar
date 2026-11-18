package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendar
import com.example.data.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriScreen(onBackClick: () -> Unit) {
    val today = remember { HijriCalendar.today() }
    val occasions = remember { HijriCalendar.upcomingOccasions(today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقويم الهجري والمناسبات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    val allText = buildString {
                        append("اليوم الهجري الحالي: ${today.formatted}. ")
                        append("المناسبات الإسلامية القادمة: ")
                        occasions.forEach {
                            append("${it.title}، ${it.description}، ")
                            append(if (it.daysUntil == 0) "اليوم. " else "بعد ${it.daysUntil} يوم. ")
                        }
                    }
                    IconButton(onClick = { TtsManager.toggle(allText) }) {
                        Icon(
                            if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = "قراءة الصفحة بالروبوت"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            today.formatted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "اليوم الهجري الحالي",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(10.dp))
                        FilledTonalIconButton(
                            onClick = {
                                TtsManager.toggle(
                                    "اليوم الهجري الحالي: ${today.formatted}"
                                )
                            }
                        ) {
                            Icon(
                                if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "اسمع التاريخ"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "المناسبات الإسلامية القادمة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(occasions) { occasion ->
                val speakText = buildString {
                    append("${occasion.title}. ")
                    append("${occasion.description}. ")
                    append(if (occasion.daysUntil == 0) "هي اليوم." else "بعد ${occasion.daysUntil} يوم.")
                }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(occasion.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                occasion.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { TtsManager.toggle(speakText) }) {
                            Icon(
                                if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "اسمع المناسبة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (occasion.daysUntil == 0) "اليوم" else "بعد ${occasion.daysUntil} يوم",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
