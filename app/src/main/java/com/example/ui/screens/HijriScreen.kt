package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendar
import com.example.data.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        HijriCalendar.initOffset(context)
    }

    val offsetDays = HijriCalendar.offsetDays
    val today = remember(offsetDays) { HijriCalendar.today() }
    val gregorianToday = remember(offsetDays) { HijriCalendar.todayGregorianYemen(0) }
    val occasions = remember(today) { HijriCalendar.upcomingOccasions(today) }

    var dayInput by remember { mutableStateOf(today.day.toString()) }
    var selectedMonth by remember { mutableIntStateOf(today.month) }
    var yearInput by remember { mutableStateOf(today.year.toString()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(today) {
        dayInput = today.day.toString()
        selectedMonth = today.month
        yearInput = today.year.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقويم الهجري والمناسبات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    val allText = buildString {
                        append("اليوم الهجري الحالي بتوقيت اليمن: ${today.formatted}. ")
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (offsetDays == 0) {
                                "اليوم الهجري الحالي • بتوقيت اليمن"
                            } else {
                                val sign = if (offsetDays > 0) "+$offsetDays" else "$offsetDays"
                                "بتوقيت اليمن (تعديل يدوي: $sign يوم)"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "الميلادي (اليمن): ${gregorianToday.formatted}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(10.dp))
                        FilledTonalIconButton(
                            onClick = {
                                TtsManager.toggle(
                                    "اليوم الهجري الحالي بتوقيت اليمن: ${today.formatted}"
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
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "تحرير وتعديل التاريخ الهجري",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            "التاريخ مضبوط تلقائيًا على التقويم الهجري الفعلي بتوقيت اليمن، ويمكنك تقديمه أو تأخيره أو كتابة التاريخ يدويًا لمطابقة الرؤية المحلية.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    HijriCalendar.setOffsetDays(context, offsetDays - 1)
                                    isError = false
                                    statusMessage = "تم إنقاص يوم واحد"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hijri_minus_day_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("يوم")
                            }
                            OutlinedButton(
                                onClick = {
                                    HijriCalendar.setOffsetDays(context, 0)
                                    isError = false
                                    statusMessage = "تمت إعادة الضبط لتوقيت اليمن التلقائي"
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("hijri_reset_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تلقائي")
                            }
                            OutlinedButton(
                                onClick = {
                                    HijriCalendar.setOffsetDays(context, offsetDays + 1)
                                    isError = false
                                    statusMessage = "تمت زيادة يوم واحد"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hijri_plus_day_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("يوم")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dayInput,
                                onValueChange = { value ->
                                    dayInput = value.filter(Char::isDigit).take(2)
                                },
                                label = { Text("اليوم") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("hijri_day_input")
                            )
                            Box(modifier = Modifier.weight(1.4f)) {
                                ExposedDropdownMenuBox(
                                    expanded = monthMenuExpanded,
                                    onExpandedChange = { monthMenuExpanded = !monthMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = HijriCalendar.HIJRI_MONTHS[(selectedMonth - 1).coerceIn(0, 11)],
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الشهر") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded)
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("hijri_month_dropdown")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = monthMenuExpanded,
                                        onDismissRequest = { monthMenuExpanded = false }
                                    ) {
                                        HijriCalendar.HIJRI_MONTHS.forEachIndexed { index, monthName ->
                                            DropdownMenuItem(
                                                text = { Text("${index + 1} - $monthName") },
                                                onClick = {
                                                    selectedMonth = index + 1
                                                    monthMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = yearInput,
                                onValueChange = { value ->
                                    yearInput = value.filter(Char::isDigit).take(4)
                                },
                                label = { Text("السنة") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hijri_year_input")
                            )
                        }

                        Button(
                            onClick = {
                                val d = dayInput.toIntOrNull()
                                val y = yearInput.toIntOrNull()
                                if (d == null || y == null || d !in 1..30 || y !in 1300..1600) {
                                    isError = true
                                    statusMessage = "أدخل يومًا (1–30) وسنة صحيحة (1300–1600)"
                                } else if (HijriCalendar.setCustomToday(context, y, selectedMonth, d)) {
                                    isError = false
                                    statusMessage = "تم حفظ التاريخ الهجري: $d ${HijriCalendar.HIJRI_MONTHS[selectedMonth - 1]} $y هـ"
                                } else {
                                    isError = true
                                    statusMessage = "تعذّر ضبط هذا التاريخ، تحقق من القيم المدخلة"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hijri_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ التاريخ المعدّل")
                        }

                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
