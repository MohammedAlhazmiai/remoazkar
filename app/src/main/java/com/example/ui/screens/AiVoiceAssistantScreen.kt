package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisnRepository
import com.example.data.TtsManager

/**
 * المساعد الصوتي "روبوت ريمو":
 * - يقرأ أي نص يكتبه أو يلصقه المستخدم بصوت فصيح.
 * - يقرأ أبوابًا كاملة من حصن المسلم بضغطة واحدة.
 * - لا يستخدم أي ملفات تلاوة مسجّلة، بل محرّك النطق العربي في الجهاز.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVoiceAssistantScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    HisnRepository.ensureLoaded(context)

    val speaking = TtsManager.isSpeaking
    var freeText by remember { mutableStateOf("") }
    var selectedText by remember {
        mutableStateOf("أهلًا بك في روبوت ريمواذكار. اضغط على أي نص في التطبيق ليقرأه لك بصوت عربي فصيح وواضح، أو اكتب ما تريد هنا ثم اضغط اقرأ.")
    }

    val featured = remember { HisnRepository.featuredIds().mapNotNull { HisnRepository.chapter(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المساعد الصوتي", fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (speaking) "الروبوت يقرأ الآن…" else "جاهز للقراءة الصوتية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = selectedText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(14.dp))
                        if (speaking) {
                            Button(
                                onClick = { TtsManager.stop() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("إيقاف القراءة", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    selectedText = freeText.ifBlank { selectedText }
                                    TtsManager.speak(selectedText)
                                }
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("إعادة القراءة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = freeText,
                    onValueChange = { freeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اكتب أو الصق أي نص ليقرأه الروبوت") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        val t = freeText.trim()
                        if (t.isNotEmpty()) {
                            selectedText = t
                            TtsManager.speak(t)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = freeText.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("اقرأ النص المكتوب", fontWeight = FontWeight.Bold)
                }
            }

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
                    chapter.items.joinToString("\n\n") { item ->
                        val rep = if (item.repeat > 1) " (تُكرَّر ${item.repeat} مرات)" else ""
                        item.text + rep
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedText = chapter.title
                            TtsManager.speak(chapter.title + ". " + fullText)
                        },
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
        }
    }
}
