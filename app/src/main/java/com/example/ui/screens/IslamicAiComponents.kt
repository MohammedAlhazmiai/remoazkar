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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiMessageEntity
import com.example.util.ShareUtil
import java.io.File

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
    onSaveToPhone: (AiMessageEntity) -> Unit = {},
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
    var fullImagePreviewUrl by remember { mutableStateOf<String?>(null) }

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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (message.categoryTag.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = message.categoryTag,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
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
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // أزرار النسخ والحفظ والمشاركة السريعة بجانب رأس الرد مباشرة
                        ReplyActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "نسخ",
                            contentDescription = "نسخ رد المساعد",
                            testTag = "ai_copy_reply_button",
                            onClick = { onCopy(plainContent) }
                        )
                        ReplyActionChip(
                            icon = Icons.Default.Download,
                            label = "حفظ",
                            contentDescription = "حفظ الرد أو البطاقة في ذاكرة الهاتف",
                            testTag = "ai_save_reply_button",
                            onClick = { onSaveToPhone(message) }
                        )
                        Box {
                            ReplyActionChip(
                                icon = Icons.Default.Share,
                                label = "مشاركة",
                                contentDescription = "مشاركة الرد عبر واتساب وتطبيقات التواصل أو البلوتوث",
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

                // عرض المرفق إذا وُجد (صورة أو بطاقة إسلامية أو تسجيل صوتي)
                if (message.attachmentUri.isNotBlank()) {
                    if (message.attachmentType == "card" || message.attachmentType == "image") {
                        val imageSource = if (message.attachmentUri.startsWith("/") && !message.attachmentUri.startsWith("file://")) {
                            File(message.attachmentUri)
                        } else {
                            message.attachmentUri
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fullImagePreviewUrl = message.attachmentUri }
                                .padding(bottom = 8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageSource)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "معاينة البطاقة أو الصورة الإسلامية",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (message.attachmentType == "audio") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUser) Color(0x33FFFFFF) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isUser) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "تسجيل صوتي مرفق",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
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
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
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
                            icon = Icons.Default.Download,
                            label = "حفظ",
                            contentDescription = "حفظ الرد أو البطاقة في الهاتف",
                            testTag = "ai_save_reply_bottom",
                            onClick = { onSaveToPhone(message) }
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

    // نافذة تكبير الصورة عند الضغط عليها
    fullImagePreviewUrl?.let { previewPath ->
        Dialog(onDismissRequest = { fullImagePreviewUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val src = if (previewPath.startsWith("/") && !previewPath.startsWith("file://")) {
                        File(previewPath)
                    } else {
                        previewPath
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(src)
                            .crossfade(true)
                            .build(),
                        contentDescription = "عرض البطاقة بحجم كامل",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
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
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
