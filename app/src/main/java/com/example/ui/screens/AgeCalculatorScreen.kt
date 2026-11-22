package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendar
import com.example.data.HijriDate
import java.util.Calendar

/**
 * حاسبة العمر: تبويب لحساب العمر بالسنوات والأشهر والأيام من تاريخ هجري أو ميلادي،
 * وتبويب لتحويل التاريخ بين الهجري والميلادي. الحسابات حقيقية عبر خوارزمية اليوم اليولياني.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(onBackClick: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حاسبة العمر", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("حساب العمر", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("تحويل التاريخ", fontWeight = FontWeight.Bold) }
                )
            }
            if (selectedTab == 0) AgeTab() else ConvertTab()
        }
    }
}

/* ------------------------------------------------------------------ */
/*  حساب العمر                                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun AgeTab() {
    val context = LocalContext.current
    var isHijri by rememberSaveable { mutableStateOf(false) }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<AgeResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val now = remember { Calendar.getInstance() }
    val todayG = Triple(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH)
    )
    val todayH = remember { HijriCalendar.today() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "أدخل تاريخ ميلادك",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isHijri,
                            onClick = { isHijri = false },
                            label = { Text("ميلادي") }
                        )
                        FilterChip(
                            selected = isHijri,
                            onClick = { isHijri = true },
                            label = { Text("هجري") }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = day,
                            onValueChange = { day = it.filter(Char::isDigit).take(2) },
                            label = { Text("اليوم") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = month,
                            onValueChange = { month = it.filter(Char::isDigit).take(2) },
                            label = { Text("الشهر") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit).take(4) },
                            label = { Text("السنة") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isHijri) "التاريخ الهجري الحالي: ${todayH.formatted}"
                        else "التاريخ الميلادي الحالي: ${todayG.third} / ${todayG.second} / ${todayG.first}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            error = null
                            result = null
                            val d = day.toIntOrNull()
                            val m = month.toIntOrNull()
                            val y = year.toIntOrNull()
                            if (d == null || m == null || y == null) {
                                error = "يرجى إدخال اليوم والشهر والسنة بالأرقام."
                                return@Button
                            }
                            if (isHijri) {
                                if (m !in 1..12 || d !in 1..30 || y !in 1300..1500) {
                                    error = "تاريخ هجري غير صالح. تأكد من اليوم (1-30) والشهر (1-12)."
                                    return@Button
                                }
                                val g = HijriCalendar.toGregorian(y, m, d)
                                if (g == null) {
                                    error = "تعذّر تحويل التاريخ الهجري."
                                    return@Button
                                }
                                val cmp = compareG(g.year, g.month, g.day, todayG.first, todayG.second, todayG.third)
                                if (cmp > 0) {
                                    error = "تاريخ الميلاد لا يمكن أن يكون في المستقبل."
                                    return@Button
                                }
                                val age = ageHijri(y, m, d, todayH.year, todayH.month, todayH.day)
                                val days = HijriCalendar.hijriToJD(todayH.year, todayH.month, todayH.day) -
                                    HijriCalendar.hijriToJD(y, m, d)
                                result = AgeResult(
                                    calendarLabel = "هجري",
                                    years = age.first,
                                    months = age.second,
                                    days = age.third,
                                    totalDays = days,
                                    birthFormatted = "$d ${HijriCalendar.HIJRI_MONTHS[m - 1]} $y هـ",
                                    gregorianEquivalent = "${g.day} / ${g.month} / ${g.year} م",
                                    hijriEquivalent = "$d ${HijriCalendar.HIJRI_MONTHS[m - 1]} $y هـ",
                                    nextBirthdayDays = nextBirthdayHijri(y, m, d, todayH)
                                )
                            } else {
                                if (m !in 1..12 || d !in 1..31 || y !in 1900..2100) {
                                    error = "تاريخ ميلادي غير صالح. تأكد من اليوم والشهر والسنة."
                                    return@Button
                                }
                                if (d > daysInGregorianMonth(y, m)) {
                                    error = "اليوم $d غير موجود في هذا الشهر."
                                    return@Button
                                }
                                val cmp = compareG(y, m, d, todayG.first, todayG.second, todayG.third)
                                if (cmp > 0) {
                                    error = "تاريخ الميلاد لا يمكن أن يكون في المستقبل."
                                    return@Button
                                }
                                val age = ageGregorian(y, m, d, todayG.first, todayG.second, todayG.third)
                                val days = HijriCalendar.gregorianToJD(todayG.first, todayG.second, todayG.third) -
                                    HijriCalendar.gregorianToJD(y, m, d)
                                val h = HijriCalendar.fromGregorian(y, m, d)
                                result = AgeResult(
                                    calendarLabel = "ميلادي",
                                    years = age.first,
                                    months = age.second,
                                    days = age.third,
                                    totalDays = days,
                                    birthFormatted = "$d / $m / $y م",
                                    gregorianEquivalent = "$d / $m / $y م",
                                    hijriEquivalent = "${h.day} ${h.monthName} ${h.year} هـ",
                                    nextBirthdayDays = nextBirthdayGregorian(y, m, d, todayG)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("احسب العمر")
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            }
        }

        result?.let { r ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("نتيجة حساب العمر", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AgeUnit(r.years.toString(), "سنة")
                            AgeUnit(r.months.toString(), "شهر")
                            AgeUnit(r.days.toString(), "يوم")
                        }
                        Spacer(Modifier.height(14.dp))
                        InfoRow("التقويم المستخدم", r.calendarLabel)
                        InfoRow("تاريخ الميلاد", r.birthFormatted)
                        InfoRow("المقابل الميلادي", r.gregorianEquivalent)
                        InfoRow("المقابل الهجري", r.hijriEquivalent)
                        InfoRow("مجموع الأيام", "${r.totalDays} يومًا")
                        InfoRow("المتبقي لعيد الميلاد", "${r.nextBirthdayDays} يومًا")
                        Spacer(Modifier.height(12.dp))
                        val shareText = buildString {
                            append("عمري الآن: ${r.years} سنة و${r.months} شهرًا و${r.days} يومًا\n")
                            append("تاريخ الميلاد: ${r.birthFormatted}\n")
                            append("مجموع الأيام: ${r.totalDays} يومًا\n")
                            append("— تطبيق رِم الأذكار")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("حساب العمر", shareText))
                                    Toast.makeText(context, "تم نسخ النتيجة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("نسخ")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة النتيجة"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("مشاركة")
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AgeResult(
    val calendarLabel: String,
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Int,
    val birthFormatted: String,
    val gregorianEquivalent: String,
    val hijriEquivalent: String,
    val nextBirthdayDays: Int
)

@Composable
private fun AgeUnit(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

/* ------------------------------------------------------------------ */
/*  تحويل التاريخ                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun ConvertTab() {
    var hijriToGregorian by rememberSaveable { mutableStateOf(true) }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var resultBody by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "اتجاه التحويل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = hijriToGregorian,
                            onClick = { hijriToGregorian = true },
                            label = { Text("هجري ← ميلادي") }
                        )
                        FilterChip(
                            selected = !hijriToGregorian,
                            onClick = { hijriToGregorian = false },
                            label = { Text("ميلادي ← هجري") }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = day,
                            onValueChange = { day = it.filter(Char::isDigit).take(2) },
                            label = { Text("اليوم") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = month,
                            onValueChange = { month = it.filter(Char::isDigit).take(2) },
                            label = { Text("الشهر") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit).take(4) },
                            label = { Text("السنة") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            error = null
                            resultTitle = null
                            resultBody = null
                            val d = day.toIntOrNull()
                            val m = month.toIntOrNull()
                            val y = year.toIntOrNull()
                            if (d == null || m == null || y == null) {
                                error = "يرجى إدخال اليوم والشهر والسنة بالأرقام."
                                return@Button
                            }
                            if (hijriToGregorian) {
                                if (m !in 1..12 || d !in 1..30 || y !in 1300..1600) {
                                    error = "تاريخ هجري غير صالح."
                                    return@Button
                                }
                                val g = HijriCalendar.toGregorian(y, m, d)
                                if (g == null) {
                                    error = "تعذّر التحويل، تحقق من صحة التاريخ."
                                    return@Button
                                }
                                resultTitle = "النتيجة بالميلادي"
                                resultBody = "${g.day} / ${g.month} / ${g.year} م\n" +
                                    "الموافق: ${gregorianWeekday(g.year, g.month, g.day)}"
                            } else {
                                if (m !in 1..12 || d !in 1..31 || y !in 1900..2100) {
                                    error = "تاريخ ميلادي غير صالح."
                                    return@Button
                                }
                                if (d > daysInGregorianMonth(y, m)) {
                                    error = "اليوم $d غير موجود في هذا الشهر."
                                    return@Button
                                }
                                val h = HijriCalendar.fromGregorian(y, m, d)
                                resultTitle = "النتيجة بالهجري"
                                resultBody = "${h.day} ${h.monthName} ${h.year} هـ\n" +
                                    "الموافق: ${gregorianWeekday(y, m, d)}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تحويل التاريخ")
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            }
        }

        if (resultBody != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            resultTitle ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(resultBody ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "ملاحظة: التحويل يعتمد على خوارزمية التقويم الهجري الحسابية (الكويتية)، وقد يختلف يومًا واحدًا عن التقويم الرسمي في بعض الأشهر.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  حسابات مساعدة                                                      */
/* ------------------------------------------------------------------ */

private fun daysInGregorianMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(year, month - 1, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun compareG(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Int =
    when {
        y1 != y2 -> y1.compareTo(y2)
        m1 != m2 -> m1.compareTo(m2)
        else -> d1.compareTo(d2)
    }

private fun ageGregorian(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Triple<Int, Int, Int> {
    var years = nY - bY
    var months = nM - bM
    var days = nD - bD
    if (days < 0) {
        months -= 1
        val prevMonth = if (nM == 1) 12 else nM - 1
        val prevYear = if (nM == 1) nY - 1 else nY
        days += daysInGregorianMonth(prevYear, prevMonth)
    }
    if (months < 0) {
        years -= 1
        months += 12
    }
    return Triple(years, months, days)
}

private fun ageHijri(bY: Int, bM: Int, bD: Int, nY: Int, nM: Int, nD: Int): Triple<Int, Int, Int> {
    var years = nY - bY
    var months = nM - bM
    var days = nD - bD
    if (days < 0) {
        months -= 1
        val prevMonth = if (nM == 1) 12 else nM - 1
        val prevYear = if (nM == 1) nY - 1 else nY
        days += HijriCalendar.hijriMonthLength(prevYear, prevMonth)
    }
    if (months < 0) {
        years -= 1
        months += 12
    }
    return Triple(years, months, days)
}

private fun nextBirthdayGregorian(bY: Int, bM: Int, bD: Int, today: Triple<Int, Int, Int>): Int {
    val todayJd = HijriCalendar.gregorianToJD(today.first, today.second, today.third)
    var year = today.first
    var jd = HijriCalendar.gregorianToJD(year, bM, bD.coerceAtMost(daysInGregorianMonth(year, bM)))
    if (jd < todayJd) {
        year += 1
        jd = HijriCalendar.gregorianToJD(year, bM, bD.coerceAtMost(daysInGregorianMonth(year, bM)))
    }
    return jd - todayJd
}

private fun nextBirthdayHijri(bY: Int, bM: Int, bD: Int, today: HijriDate): Int {
    val todayJd = HijriCalendar.hijriToJD(today.year, today.month, today.day)
    var year = today.year
    var jd = HijriCalendar.hijriToJD(year, bM, bD.coerceAtMost(HijriCalendar.hijriMonthLength(year, bM)))
    if (jd < todayJd) {
        year += 1
        jd = HijriCalendar.hijriToJD(year, bM, bD.coerceAtMost(HijriCalendar.hijriMonthLength(year, bM)))
    }
    return jd - todayJd
}

private fun gregorianWeekday(y: Int, m: Int, d: Int): String {
    val names = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    val jd = HijriCalendar.gregorianToJD(y, m, d)
    return names[((jd % 7) + 7) % 7]
}
