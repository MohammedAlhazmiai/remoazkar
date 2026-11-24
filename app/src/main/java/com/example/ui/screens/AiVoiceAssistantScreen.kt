package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.HisnRepository
import com.example.data.TtsManager
import com.example.ui.components.SmartReaderBus
import com.example.ui.components.buildReadableText
import com.example.ui.theme.GoldAccent

/**
 * «القارئ الذكي» — النسخة الموسَّعة من روبوت القراءة الصوتية.
 *
 * المزايا:
 *  - يقرأ نص الشاشة الحالية التي يراها المستخدم (عبر [SmartReaderBus]).
 *  - يقرأ أي نص من الحافظة بضغطة واحدة.
 *  - يقرأ أي نص يكتبه أو يلصقه المستخدم.
 *  - يقرأ أبوابًا كاملة من حصن المسلم.
 *  - يحتفظ بسجل بآخر النصوص المقروءة ليُعيد قراءتها لاحقًا.
 *  - يعمل من زر عائم موجود في كل صفحات التطبيق، لا من هذه الشاشة فقط.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVoiceAssistantScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadVoiceOnResume by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && reloadVoiceOnResume) {
                reloadVoiceOnResume = false
                TtsManager.init(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    HisnRepository.ensureLoaded(context)

    var speaking by remember { mutableStateOf(TtsManager.isSpeaking) }
    var freeText by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    val featured = remember(HisnRepository.isLoaded) {
        HisnRepository.featuredIds().mapNotNull { HisnRepository.chapter(it) }
    }
    val history = SmartReaderBus.history

    fun read(text: String, label: String) {
        val clean = text.trim()
        if (clean.isEmpty()) {
            notice = "لا يوجد نص للقراءة"
            return
        }
        TtsManager.speak(clean)
        SmartReaderBus.remember(context, clean)
        speaking = true
        notice = label
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("القارئ الذكي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            TtsManager.stop()
                            onBackClick()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- بطاقة الهوية ----------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1B4D3E), Color(0xFF2E7D5B), Color(0xFF3E8E6E))
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_smart_reader),
                                    contentDescription = "القارئ الذكي",
                                    modifier = Modifier.size(52.dp),
                                    tint = Color.Unspecified
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (speaking) "القارئ الذكي يقرأ الآن…" else "القارئ الذكي جاهز",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "يقرأ لك أي نص تختاره: نص الشاشة، أو من الحافظة، أو ما تكتبه بنفسك — بصوت عربي فصيح وواضح.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (speaking || TtsManager.isSpeaking) {
                                    Button(
                                        onClick = { TtsManager.pause(); speaking = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldAccent,
                                            contentColor = Color(0xFF1B1B1B)
                                        )
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("إيقاف مؤقت", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { TtsManager.stop(); speaking = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFB3261E)
                                        )
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("إنهاء", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            TtsManager.resume()
                                            speaking = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldAccent,
                                            contentColor = Color(0xFF1B1B1B)
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("متابعة", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- إجراءات سريعة ----------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("اختيار صوت القارئ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "يعتمد النطق على محرك تحويل النص إلى كلام في جهازك، ويحاول التطبيق اختيار صوت عربي رجالي تلقائيًا لنطق فصيح وأقرب للصوت البشري. النطق اصطناعي وليس تسجيل تلاوة بشرية، ولا يضمن أحكام التجويد. يمكنك تثبيت صوت عربي رجالي أو تغييره من إعدادات الجهاز إذا كان متاحًا.",
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                TtsManager.stop()
                                TtsManager.shutdown()
                                reloadVoiceOnResume = true
                                runCatching {
                                    context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                                }.onFailure {
                                    reloadVoiceOnResume = false
                                    TtsManager.init(context)
                                    notice = "تعذّر فتح إعدادات الصوت على هذا الجهاز."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("إعداد أصوات النطق على الجهاز")
                        }
                    }
                }
            }
            item {
                Text("اقرأ بضغطة واحدة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "نص الشاشة",
                        subtitle = SmartReaderBus.screenTitle.ifBlank { "لا يوجد" },
                        icon = R.drawable.ic_smart_reader,
                        enabled = SmartReaderBus.screenText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        read(SmartReaderBus.screenText, "قراءة نص الشاشة")
                    }
                    QuickActionCard(
                        title = "من الحافظة",
                        subtitle = "لصق وقراءة",
                        icon = R.drawable.ic_smart_reader,
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        val clip = clipboard.getText()?.text.orEmpty()
                        if (clip.isBlank()) {
                            notice = "الحافظة فارغة"
                        } else {
                            freeText = clip
                            read(clip, "قراءة من الحافظة")
                        }
                    }
                }
            }

            // ---------- نص مخصص ----------
            item {
                OutlinedTextField(
                    value = freeText,
                    onValueChange = { freeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اكتب أو الصق أي نص ليقرأه القارئ الذكي") },
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { read(freeText, "قراءة النص المكتوب") },
                        modifier = Modifier.weight(1f),
                        enabled = freeText.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("اقرأ النص", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) freeText = clip
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("لصق")
                    }
                }
            }

            notice?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            msg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ---------- سجل القراءة ----------
            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("سجل القراءة", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { SmartReaderBus.clearHistory(context) }) {
                            Text("مسح", fontSize = 12.sp)
                        }
                    }
                }
                items(history, key = { it }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { read(item, "قراءة من السجل") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { SmartReaderBus.remove(context, item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                            }
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ---------- أبواب جاهزة ----------
            item {
                Text(
                    "أبواب جاهزة للقراءة الكاملة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(featured, key = { it.id }) { chapter ->
                val fullText = remember(chapter.id) {
                    buildReadableText(
                        chapter.title,
                        chapter.items.map { item ->
                            if (item.repeat > 1) "${item.text} (تُكرَّر ${item.repeat} مرات)" else item.text
                        }
                    )
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { read(fullText, chapter.title) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(chapter.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${chapter.items.size} ذكرًا • ${chapter.totalRepeats} تكرارًا",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ---------- كيف يعمل ----------
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("كيف تستخدم القارئ الذكي في كل صفحة؟", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        GuideLine("١", "افتح أي صفحة في التطبيق — القرآن، حصن المسلم، الرقية، الأدعية، الأذكار.")
                        GuideLine("٢", "اضغط الزر العائم الأخضر في أسفل الشاشة.")
                        GuideLine("٣", "اختر «قراءة نص الشاشة الحالية» أو «قراءة من الحافظة» أو اكتب نصك.")
                        GuideLine("٤", "يمكنك الإيقاف المؤقت أو الإنهاء من الشريط السفلي في أي وقت.")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideLine(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
    }
}
