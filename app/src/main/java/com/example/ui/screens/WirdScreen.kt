package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyWirdStore
import com.example.data.TtsManager
import com.example.data.WirdTask

/**
 * شاشة "خطة الورد اليومي":
 * تقدّم مرئي لكل مهمة، سلسلة أيام متتالية، وقراءة صوتية للتذكير.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirdScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(DailyWirdStore.todayProgress(context)) }
    var percent by remember { mutableIntStateOf(DailyWirdStore.percent(context)) }
    var streak by remember { mutableIntStateOf(DailyWirdStore.streak(context)) }

    fun refresh() {
        progress = DailyWirdStore.todayProgress(context)
        percent = DailyWirdStore.percent(context)
        streak = DailyWirdStore.streak(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("خطة الورد اليومي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "إنجاز اليوم",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("$streak يوم متتالٍ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (percent >= 100) "ما شاء الله، أكملت ورد اليوم بالكامل"
                            else "أكملت $percent٪ من ورد اليوم — واصل، القليل الدائم خير من الكثير المنقطع.",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(DailyWirdStore.tasksToday(), key = { it.id }) { task ->
                val value = progress[task.id] ?: 0
                val done = value >= task.target
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (done) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    task.hint,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { TtsManager.toggle("${task.title}. ${task.hint}") }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "قراءة")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$value / ${task.target} ${task.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            FilledIconButton(
                                onClick = {
                                    DailyWirdStore.increment(context, task.id, -step(task))
                                    refresh()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "إنقاص")
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    DailyWirdStore.increment(context, task.id, step(task))
                                    refresh()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        "الورد يُصفَّر تلقائيًا كل يوم جديد، وتبقى سلسلتك محفوظة ما دمت تكمل وردك في اليوم التالي.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun step(task: WirdTask): Int = if (task.target >= 100) 10 else 1
