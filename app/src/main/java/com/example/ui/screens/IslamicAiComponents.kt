package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiConversationEntity
import com.example.data.ai.AiMessageEntity
import com.example.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiConversationsDrawerSheet(
    conversations: List<AiConversationEntity>,
    activeConversationId: Long?,
    onNewChatClick: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("ar")) }

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F2E24), Color(0xFF1A4B3B))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                "سجل المحادثات المحفوظة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "حفظ محلي على جهازك بدون إنترنت",
                                color = Color(0xFFD5E8DD),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = onNewChatClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("drawer_new_chat_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD36A),
                            contentColor = Color(0xFF102A21)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("محادثة جديدة +", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "لا توجد محادثات محفوظة بعد",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "ابدأ سؤالك وسيتم حفظ المحادثة تلقائيًا هنا لتعود إليها في أي وقت.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { item ->
                        val selected = item.id == activeConversationId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectConversation(item.id) }
                                .testTag("drawer_conversation_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.previewText.isNotBlank()) {
                                        Text(
                                            text = item.previewText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.categoryTag,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "• ${dateFormat.format(Date(item.updatedAt))}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteConversation(item.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف المحادثة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMessageBubble(
    message: AiMessageEntity,
    showSupportPrompt: Boolean = false,
    onDismissSupportPrompt: () -> Unit = {},
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onShare: (String) -> Unit,
    onShareWhatsapp: (String) -> Unit = {},
    onShareBluetooth: (String) -> Unit = {},
    onFollowUpClick: (String) -> Unit,
    onSupportDeveloperClick: () -> Unit = {},
    onShareApp: () -> Unit = {}
) {
    val isUser = message.role == AiChatRepository.ROLE_USER
    val followUps = remember(message.followUpsSerialized) {
        AiChatRepository.parseFollowUps(message.followUpsSerialized)
    }
    val plainContent = remember(message.content) {
        ShareUtil.formatReadableText(message.content)
    }
    val styledContent = remember(message.content) {
        formatIslamicRichText(message.content)
    }
    var topShareMenuExpanded by remember(message.id) { mutableStateOf(false) }
    var bottomShareMenuExpanded by remember(message.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 355.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 4.dp else 18.dp,
                bottomEnd = if (isUser) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    Color(0xFF144537)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (message.categoryTag.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = message.categoryTag,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.OfflinePin,
                                contentDescription = "إجابة محلية بدون إنترنت",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // زر النسخ والمشاركة السريعة بجانب رأس الرد مباشرة
                        ReplyActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "نسخ",
                            contentDescription = "نسخ رد المساعد",
                            testTag = "ai_copy_reply_button",
                            onClick = { onCopy(plainContent) }
                        )
                        Box {
                            ReplyActionChip(
                                icon = Icons.Default.Share,
                                label = "مشاركة",
                                contentDescription = "مشاركة رد المساعد عبر واتساب أو البرامج أو البلوتوث",
                                testTag = "ai_share_reply_button",
                                onClick = { topShareMenuExpanded = true }
                            )
                            AiReplyShareDropdownMenu(
                                expanded = topShareMenuExpanded,
                                onDismiss = { topShareMenuExpanded = false },
                                onShareWhatsapp = { onShareWhatsapp(plainContent) },
                                onShareAllApps = { onShare(plainContent) },
                                onShareBluetooth = { onShareBluetooth(plainContent) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                SelectionContainer {
                    Text(
                        text = styledContent,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 23.sp
                    )
                }

                if (!isUser && message.referencesSummary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "المراجع: ${message.referencesSummary}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 16.sp
                    )
                }

                if (!isUser) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReplyActionChip(
                            icon = Icons.Default.VolumeUp,
                            label = "استماع",
                            contentDescription = "الاستماع إلى رد المساعد",
                            testTag = "ai_speak_reply_bottom",
                            onClick = { onSpeak(plainContent) }
                        )
                        ReplyActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "نسخ",
                            contentDescription = "نسخ الرد",
                            testTag = "ai_copy_reply_bottom",
                            onClick = { onCopy(plainContent) }
                        )
                        Box {
                            ReplyActionChip(
                                icon = Icons.Default.Share,
                                label = "مشاركة",
                                contentDescription = "خيارات مشاركة الرد",
                                testTag = "ai_share_reply_bottom",
                                onClick = { bottomShareMenuExpanded = true }
                            )
                            AiReplyShareDropdownMenu(
                                expanded = bottomShareMenuExpanded,
                                onDismiss = { bottomShareMenuExpanded = false },
                                onShareWhatsapp = { onShareWhatsapp(plainContent) },
                                onShareAllApps = { onShare(plainContent) },
                                onShareBluetooth = { onShareBluetooth(plainContent) }
                            )
                        }
                    }

                    if (followUps.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            followUps.forEach { suggestion ->
                                AssistChip(
                                    onClick = { onFollowUpClick(suggestion) },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showSupportPrompt,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            DeveloperSupportPromptFooter(
                                onSupportDeveloperClick = onSupportDeveloperClick,
                                onShareApp = onShareApp,
                                onDismiss = onDismissSupportPrompt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyActionChip(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
