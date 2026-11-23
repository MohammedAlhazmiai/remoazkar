package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiConversationEntity
import com.example.data.ai.AiMessageEntity
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
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onShare: (String) -> Unit,
    onFollowUpClick: (String) -> Unit,
    onSupportDeveloperClick: () -> Unit = {},
    onShareApp: () -> Unit = {}
) {
    val isUser = message.role == AiChatRepository.ROLE_USER
    val followUps = remember(message.followUpsSerialized) {
        AiChatRepository.parseFollowUps(message.followUpsSerialized)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 350.dp),
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
                if (!isUser && message.categoryTag.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = message.categoryTag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.OfflinePin,
                            contentDescription = "إجابة محلية بدون إنترنت",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                SelectionContainer {
                    Text(
                        text = message.content,
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
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onCopy(message.content) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "نسخ الرد",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onSpeak(message.content) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "استماع للرد",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onShare(message.content) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "مشاركة الرد",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (followUps.isNotEmpty()) {
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

                    Spacer(Modifier.height(10.dp))
                    DeveloperSupportPromptFooter(
                        onSupportDeveloperClick = onSupportDeveloperClick,
                        onShareApp = onShareApp
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperSupportPromptFooter(
    onSupportDeveloperClick: () -> Unit,
    onShareApp: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedSupportNotice = remember(primaryColor) {
        buildAnnotatedString {
            append(
                "إذا أعجبك تطبيقنا وخدماتنا يمكنك دعم المطور حسب ما تستطيع ليستطيع مواصلة تطوير التطبيق، " +
                    "وذلك من خلال الدعم الموجود أسفل الإعدادات أو "
            )
            withLink(
                LinkAnnotation.Clickable(
                    tag = "support_developer_link",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = { onSupportDeveloperClick() }
                )
            ) {
                append("الضغط هنا")
            }
            append(
                "، وهو أمرٌ ليس إلزاميًا، وأرجو منك مشاركة التطبيق مع كل من تحب، " +
                    "ولا تنسَ الدعاء للمطور محمد الحزمي جزاك الله خيرًا."
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = annotatedSupportNotice,
                fontSize = 11.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSupportDeveloperClick,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_support_developer_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFD84359)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("ادعم المطور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onShareApp,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_share_app_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("مشاركة التطبيق", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
