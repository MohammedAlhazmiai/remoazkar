package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * خطة ختم القرآن الكريم — ميزة حصرية غير موجودة في التطبيقين المرجعيين.
 *
 * يحسب التطبيق الورد اليومي تلقائيًا (604 صفحة مصحف المدينة ÷ عدد الأيام)،
 * ويحفظ التقدّم في DataStore، ويعرض تاريخ الختام المتوقع.
 */
private const val TOTAL_PAGES = 604

private fun juzOfPage(page: Int): Int = ((page - 1) / 20) + 1

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatmahScreen(
    days: Int,
    startMillis: Long,
    pagesDone: Int,
    active: Boolean,
    onStart: (Int) -> Unit,
    onPagesDoneChange: (Int) -> Unit,
    onStop: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedDays by remember(days) { mutableIntStateOf(days) }
    val perDay = ceil(TOTAL_PAGES.toDouble() / selectedDays.toDouble()).toInt()
    val progress = (pagesDone.toFloat() / TOTAL_PAGES.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, label = "khatmahProgress")

    val todayIndex = if (active && startMillis > 0L) {
        val diff = System.currentTimeMillis() - startMillis
        (diff / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    } else 0

    val fromPage = (todayIndex * perDay + 1).coerceAtMost(TOTAL_PAGES)
    val toPage = ((todayIndex + 1) * perDay).coerceAtMost(TOTAL_PAGES)

    val finishDate = if (startMillis > 0L) startMillis + selectedDays * 24L * 60 * 60 * 1000 else 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("خطة ختم القرآن", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة التقدّم
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(150.dp),
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$pagesDone / $TOTAL_PAGES صفحة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (active) "خطتك جارية — واصل، فالقليل الدائم خير من الكثير المنقطع" else "ابدأ ختمة جديدة وحدّد المدة التي تناسبك",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            // اختيار المدة
            if (!active) {
                Text("اختر مدة الختمة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90, 120).forEach { option ->
                        FilterChip(
                            selected = selectedDays == option,
                            onClick = { selectedDays = option },
                            label = { Text("$option يومًا", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("الورد اليومي المطلوب", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "$perDay صفحة يوميًا (${String.format(Locale.US, "%.1f", perDay / 20.0)} جزء تقريبًا)",
                            fontSize = 14.sp
                        )
                        Text(
                            "تاريخ الختام المتوقع: ${formatDate(System.currentTimeMillis() + selectedDays * 24L * 60 * 60 * 1000)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Button(
                    onClick = { onStart(selectedDays) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ابدأ الختمة الآن", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // ورد اليوم
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ورد اليوم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "من صفحة $fromPage إلى صفحة $toPage",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "الجزء ${juzOfPage(fromPage)} — الجزء ${juzOfPage(toPage)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (finishDate > 0L) {
                            Text(
                                "الختام المتوقع: ${formatDate(finishDate)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                val canComplete = pagesDone + perDay <= TOTAL_PAGES
                Button(
                    onClick = {
                        onPagesDoneChange(
                            if (canComplete) pagesDone + perDay else TOTAL_PAGES
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = pagesDone < TOTAL_PAGES
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (pagesDone >= TOTAL_PAGES) "أتممت الختمة، تقبّل الله" else "أنجزت ورد اليوم",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بدء ختمة جديدة")
                }
            }

            // إحصاءات
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "المتبقي",
                    value = "${(TOTAL_PAGES - pagesDone).coerceAtLeast(0)} صفحة"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "الأجزاء المتبقية",
                    value = "${ceil((TOTAL_PAGES - pagesDone).toDouble() / 20.0).toInt()}"
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("فضل ختم القرآن", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "«اقرؤوا القرآن فإنه يأتي يوم القيامة شفيعًا لأصحابه» — رواه مسلم",
                        fontSize = 13.sp
                    )
                    Text(
                        "«من قرأ حرفًا من كتاب الله فله به حسنة، والحسنة بعشر أمثالها» — رواه الترمذي",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
