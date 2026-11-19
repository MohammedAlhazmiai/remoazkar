package com.example.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppLocale
import com.example.data.TtsManager
import com.example.data.tr
import com.example.ui.theme.GoldAccent

/**
 * ناقل مشترك بين الشاشات وبين «القارئ الذكي».
 *
 * أي شاشة تستدعي [publish] لتُعلن النص المعروض حاليًا، فيستطيع القارئ الذكي قراءته
 * من الزر العائم الموجود في كل صفحات التطبيق. كما يحتفظ بسجل آخر النصوص المقروءة.
 */
object SmartReaderBus {

    /** عنوان الشاشة الحالية. */
    var screenTitle by mutableStateOf("")
        private set

    /** النص القابل للقراءة في الشاشة الحالية. */
    var screenText by mutableStateOf("")
        private set

    /** سجل آخر النصوص التي قرأها المستخدم (الأحدث أولًا). */
    val history = mutableStateListOf<String>()

    private const val PREFS = "rim_smart_reader"
    private const val KEY_HISTORY = "history"

    /** تُستدعى من كل شاشة عند تغيّر محتواها. */
    fun publish(title: String, text: String) {
        screenTitle = title
        screenText = text
    }

    fun clearScreen() {
        screenTitle = ""
        screenText = ""
    }

    fun remember(context: Context, text: String) {
        val clean = text.trim()
        if (clean.length < 3) return
        history.remove(clean)
        history.add(0, clean)
        while (history.size > 20) history.removeAt(history.lastIndex)
        persist(context)
    }

    fun remove(context: Context, text: String) {
        history.remove(text)
        persist(context)
    }

    fun clearHistory(context: Context) {
        history.clear()
        persist(context)
    }

    private fun persist(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putStringSet(KEY_HISTORY, LinkedHashSet(history)).apply()
    }

    /** يُحمَّل السجل عند بدء التطبيق. */
    fun load(context: Context) {
        if (history.isNotEmpty()) return
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val saved = sp.getStringSet(KEY_HISTORY, emptySet()).orEmpty()
            history.clear()
            history.addAll(saved)
        } catch (error: Exception) {
            // السجل ميزة اختيارية؛ بيانات قديمة أو من نوع غير متوقع لا يجوز أن
            // تمنع إطلاق التطبيق.
            history.clear()
            Log.w("SmartReaderBus", "Ignoring unreadable smart-reader history", error)
        }
    }
}

/**
 * الزر العائم للقارئ الذكي — يظهر في كل شاشات التطبيق.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReaderFab(
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val speakingScale by animateFloatAsState(
        targetValue = if (TtsManager.isSpeaking) 1.06f else 1f,
        animationSpec = tween(600),
        label = "smartReaderPulse"
    )

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { open = true },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(58.dp)
                .scale(speakingScale)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF1B4D3E), Color(0xFF2E7D5B), GoldAccent)
                    ),
                    shape = CircleShape
                )
                .border(1.5.dp, Color(0x66D4AF37), CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_smart_reader),
                contentDescription = "القارئ الذكي",
                modifier = Modifier.size(30.dp),
                tint = Color.White
            )
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 26.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1B4D3E), GoldAccent)),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_smart_reader),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = tr("smart_reader"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يقرأ لك أي نص تحدده في أي صفحة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = { open = false }) {
                        Icon(Icons.Default.Close, contentDescription = tr("close"))
                    }
                }

                val hasScreenText = SmartReaderBus.screenText.isNotBlank()
                ReaderActionCard(
                    title = tr("read_screen"),
                    subtitle = if (hasScreenText) {
                        "${SmartReaderBus.screenTitle} · ${SmartReaderBus.screenText.take(70)}…"
                    } else {
                        "لم تُنشر نصوص في هذه الصفحة — استخدم الحافظة أو النص المخصص"
                    },
                    icon = Icons.Default.GraphicEq,
                    enabled = hasScreenText,
                    onClick = {
                        val text = SmartReaderBus.screenText
                        if (text.isNotBlank()) {
                            TtsManager.speak(text)
                            SmartReaderBus.remember(context, text)
                            open = false
                        }
                    }
                )

                ReaderActionCard(
                    title = tr("read_clipboard"),
                    subtitle = "يقرأ آخر نص نسخته من أي تطبيق آخر",
                    icon = Icons.Default.ContentPaste,
                    enabled = true,
                    onClick = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrBlank()) {
                            TtsManager.speak(text)
                            SmartReaderBus.remember(context, text)
                            open = false
                        } else {
                            open = false
                            onOpenReader()
                        }
                    }
                )

                ReaderActionCard(
                    title = "نص مخصص / صفحة القارئ",
                    subtitle = "اكتب أو الصق أي نص ليقرأه القارئ الذكي",
                    icon = Icons.Default.Edit,
                    enabled = true,
                    onClick = {
                        open = false
                        onOpenReader()
                    }
                )

                AnimatedVisibility(visible = TtsManager.isSpeaking || TtsManager.isPaused) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = TtsManager.currentSnippet.take(40),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { TtsManager.togglePlayPause() }) {
                                Icon(
                                    if (TtsManager.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { TtsManager.stop() }) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = tr("stop_reading"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (SmartReaderBus.history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل القراءات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { SmartReaderBus.clearHistory(context) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SmartReaderBus.history) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .width(180.dp)
                                    .heightIn(min = 54.dp)
                                    .clickable {
                                        TtsManager.speak(item)
                                        open = false
                                    }
                            ) {
                                Text(
                                    text = item.take(80),
                                    fontSize = 11.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f * alpha),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha)
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
        }
    }
}

/** نص جاهز للقراءة يُبنى من قائمة عناصر (يُستخدم لنشر نص الشاشات). */
fun buildReadableText(title: String, items: List<String>): String {
    if (items.isEmpty()) return title
    return buildString {
        append(title)
        append(". ")
        items.forEach { append(it); append(". ") }
    }
}

/** يُستخدم لعرض اسم اللغة الأصلية في القوائم. */
fun languageLabel(code: String): String = AppLocale.languageFor(code).nativeName
