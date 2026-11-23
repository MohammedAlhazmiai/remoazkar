package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TtsManager
import com.example.ui.viewmodel.IslamicAiViewModel
import com.example.util.ShareUtil
import kotlinx.coroutines.launch

private data class QuickCapabilityPrompt(
    val category: String,
    val prompt: String
)

private val QUICK_PROMPTS = listOf(
    QuickCapabilityPrompt("رسالة دينية", "اكتب رسالة دينية للتهنئة بحلول رمضان"),
    QuickCapabilityPrompt("خاطرة إيمانية", "اكتب خاطرة إيمانية عن التوكل والفرج بعد الضيق"),
    QuickCapabilityPrompt("خطبة جمعة", "اكتب خطبة جمعة كاملة عن تقوى الله وبر الوالدين"),
    QuickCapabilityPrompt("محاضرة دعوية", "أعد محاضرة منظمة عن تزكية النفس واغتنام العمر"),
    QuickCapabilityPrompt("فتوى فقهية", "ما حكم بخاخ الربو وقطرة العين في نهار رمضان؟"),
    QuickCapabilityPrompt("تفسير حلم", "حلمت أني أطير تحت المطر وقرأت القرآن"),
    QuickCapabilityPrompt("سجود السهو", "متى يكون سجود السهو قبل السلام ومتى يكون بعده؟"),
    QuickCapabilityPrompt("كفارة اليمين", "ما كفارة اليمين بالله ومتى يجوز صيام ثلاثة أيام؟")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IslamicAiScreen(
    onBackClick: () -> Unit,
    viewModel: IslamicAiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val submitCurrent: () -> Unit = {
        val text = inputText.trim()
        if (text.isNotEmpty() && !state.isGenerating) {
            inputText = ""
            viewModel.sendMessage(text)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AiConversationsDrawerSheet(
                conversations = state.conversations,
                activeConversationId = state.activeConversationId,
                onNewChatClick = {
                    viewModel.startNewConversation()
                    scope.launch { drawerState.close() }
                },
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("المساعد الإسلامي الذكي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "نموذج لغوي محلي • بدون إنترنت",
                                fontSize = 11.sp,
                                color = Color(0xFFD9EBDF)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("ai_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.startNewConversation() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("new_chat_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "محادثة جديدة")
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_chat_drawer_button")
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = "القائمة الجانبية وسجل المحادثات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF123B31),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                if (state.messages.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xEE154436), Color(0xE81C312B))
                                            )
                                        )
                                        .padding(18.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x33FFD36A)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD36A)
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "مساعدك الإسلامي الشخصي",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.OfflinePin,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFD36A),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        "مدرّب محليًا ويعمل دون إنترنت (${state.trainedEntriesCount} محورًا + القرآن والسنة)",
                                                        color = Color(0xFFDCECE2),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "يؤلف الرسائل الدينية والخواطر والخطب المنبرية والمحاضرات، ويجيب على الفتاوى والمسائل الفقهية بالأدلة القرآنية والنبوية، ويفسر الأحلام وفق قواعد ابن سيرين والنابلسي، ويحفظ محادثاتك تلقائيًا في القائمة الجانبية.",
                                            color = Color(0xFFF3F8F5),
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "أمثلة سريعة للبدء:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QUICK_PROMPTS.forEach { sample ->
                                    Card(
                                        modifier = Modifier
                                            .clickable { viewModel.sendMessage(sample.prompt) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                        )
                                    ) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Text(
                                                sample.category,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                sample.prompt,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            AiMessageBubble(
                                message = msg,
                                onCopy = { content ->
                                    clipboard.setText(AnnotatedString(content))
                                    Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                                },
                                onSpeak = { content -> TtsManager.toggle(content) },
                                onShare = { content -> ShareUtil.share(context, content) },
                                onFollowUpClick = { prompt -> viewModel.sendMessage(prompt) }
                            )
                        }
                        if (state.isGenerating) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text("جارٍ الصياغة والاستدلال محليًا...", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom chat input bar
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input"),
                            placeholder = { Text("اسأل في الفقه، تفسير حلم، خطبة، خاطرة، أو رسالة...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitCurrent() })
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = submitCurrent,
                            enabled = inputText.isNotBlank() && !state.isGenerating,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("ai_send_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال"
                            )
                        }
                    }
                }
            }
        }
    }
}
