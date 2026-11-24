package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TtsManager
import com.example.ui.viewmodel.IslamicAiViewModel
import com.example.util.MediaExportHelper
import com.example.util.ShareUtil
import kotlinx.coroutines.launch
import java.io.File



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IslamicAiScreen(
    onBackClick: () -> Unit,
    onSupportDeveloperClick: () -> Unit = {},
    viewModel: IslamicAiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by rememberSaveable { mutableStateOf("") }
    var pendingAttachmentType by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAttachmentUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingAttachmentName by rememberSaveable { mutableStateOf<String?>(null) }

    // لاقط الصوت لتحويل الكلام العامي والفصيح إلى نص
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val attachType = pendingAttachmentType.orEmpty()
                val attachUri = pendingAttachmentUri.orEmpty()
                pendingAttachmentType = null
                pendingAttachmentUri = null
                pendingAttachmentName = null
                inputText = ""
                viewModel.sendMessage(spoken, attachType, attachUri)
            }
        }
    }

    // لاقط الصور من الهاتف (Photo Picker القياسي بدون أذونات موسعة)
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingAttachmentType = "image"
            pendingAttachmentUri = uri.toString()
            pendingAttachmentName = MediaExportHelper.getFileName(context, uri)
        }
    }

    // لاقط الملفات الصوتية من ذاكرة الهاتف
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAttachmentType = "audio"
            pendingAttachmentUri = uri.toString()
            pendingAttachmentName = MediaExportHelper.getFileName(context, uri)
        }
    }

    val onMicClick: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ar"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تفضل بالتحدث بسؤالك أو طلبك الديني...")
        }
        runCatching {
            speechLauncher.launch(intent)
        }.onFailure {
            Toast.makeText(context, "ميزة التعرف على الصوت غير متاحة على هذا الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

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
        val attachType = pendingAttachmentType.orEmpty()
        val attachUri = pendingAttachmentUri.orEmpty()
        if ((text.isNotEmpty() || attachUri.isNotEmpty()) && !state.isGenerating) {
            inputText = ""
            pendingAttachmentType = null
            pendingAttachmentUri = null
            pendingAttachmentName = null
            viewModel.sendMessage(
                rawInput = text,
                attachmentType = attachType,
                attachmentUri = attachUri
            )
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
                                "مقتصر على العلوم الدينية • ذكاء سحابي ومحلي بدون إنترنت",
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
                    AiEmptyStateView(
                        modifier = Modifier.weight(1f),
                        onPromptClick = { prompt -> viewModel.sendMessage(prompt) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(state.messages, key = { _, msg -> msg.id }) { index, msg ->
                            val showSupport = IslamicAiViewModel.shouldShowSupportPrompt(
                                messages = state.messages,
                                messageIndex = index,
                                dismissedMessageIds = state.dismissedSupportMessageIds
                            )
                            AiMessageBubble(
                                message = msg,
                                showSupportPrompt = showSupport,
                                onDismissSupportPrompt = { viewModel.dismissSupportPrompt(msg.id) },
                                onCopy = { content ->
                                    val copied = ShareUtil.copyToClipboard(context, content)
                                    val notice = if (copied) "تم نسخ رد المساعد" else "تعذر نسخ النص"
                                    Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
                                },
                                onSpeak = { content -> TtsManager.toggle(content) },
                                onShare = { content -> ShareUtil.share(context, content) },
                                onShareWhatsapp = { content -> ShareUtil.shareWhatsapp(context, content) },
                                onShareBluetooth = { content -> ShareUtil.shareBluetooth(context, content) },
                                onSaveToPhone = { targetMsg ->
                                    if (targetMsg.attachmentType == "card" || targetMsg.attachmentType == "image") {
                                        val file = File(targetMsg.attachmentUri)
                                        val res = MediaExportHelper.saveImageToGallery(context, file, targetMsg.categoryTag)
                                        val toast = res.fold(
                                            onSuccess = { "تم حفظ البطاقة بنجاح في $it" },
                                            onFailure = { "تعذر حفظ الصورة: ${it.localizedMessage}" }
                                        )
                                        Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                    } else {
                                        val res = MediaExportHelper.saveTextToDownloads(
                                            context = context,
                                            title = targetMsg.categoryTag.ifBlank { "رد المساعد الإسلامي" },
                                            category = targetMsg.categoryTag,
                                            content = targetMsg.content,
                                            references = targetMsg.referencesSummary
                                        )
                                        val toast = res.fold(
                                            onSuccess = { "تم حفظ الرد في $it بنجاح" },
                                            onFailure = { "تعذر حفظ الملف: ${it.localizedMessage}" }
                                        )
                                        Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onFollowUpClick = { prompt -> viewModel.sendMessage(prompt) },
                                onSupportDeveloperClick = onSupportDeveloperClick,
                                onShareApp = { ShareUtil.share(context, ShareUtil.inviteMessage()) }
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
                                            Text("جارٍ الفهم والصياغة...", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // شريط إدخال الرسائل والمرفقات والمايك
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        if (pendingAttachmentUri != null) {
                            SelectedAttachmentChip(
                                attachment = PendingAttachment(
                                    type = pendingAttachmentType.orEmpty(),
                                    uri = Uri.parse(pendingAttachmentUri),
                                    displayName = pendingAttachmentName.orEmpty()
                                ),
                                onRemove = {
                                    pendingAttachmentType = null
                                    pendingAttachmentUri = null
                                    pendingAttachmentName = null
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AttachmentOptionsButton(
                                onPickImage = {
                                    pickPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickAudio = {
                                    pickAudioLauncher.launch("audio/*")
                                },
                                onDesignCardPrompt = {
                                    inputText = "صمم لي بطاقة دعوية إسلامية"
                                }
                            )

                            IconButton(
                                onClick = onMicClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ai_mic_button")
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "التسجيل الصوتي وسماع الطلب باللهجة العامية أو الفصحى",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_input"),
                                placeholder = { Text("اكتب أو تحدث بالمايك...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { submitCurrent() })
                            )
                            Spacer(Modifier.width(6.dp))
                            FilledIconButton(
                                onClick = submitCurrent,
                                enabled = (inputText.isNotBlank() || pendingAttachmentUri != null) && !state.isGenerating,
                                modifier = Modifier
                                    .size(48.dp)
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
}
