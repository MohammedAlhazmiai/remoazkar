package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * يحوّل علامات `**العنوان**` إلى خط عريض حقيقي داخل فقاعة رد المساعد،
 * دون عرض نجوم برمجية أو التأثير على التحديد والنسخ.
 */
internal fun formatIslamicRichText(raw: String): AnnotatedString {
    if (!raw.contains("**")) return AnnotatedString(raw)
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < raw.length) {
            val start = raw.indexOf("**", cursor)
            if (start < 0) {
                append(raw.substring(cursor))
                break
            }
            val end = raw.indexOf("**", start + 2)
            if (end < 0) {
                append(raw.substring(cursor))
                break
            }
            if (start > cursor) {
                append(raw.substring(cursor, start))
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(raw.substring(start + 2, end))
            }
            cursor = end + 2
        }
    }
}

@Composable
internal fun AiReplyShareDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShareWhatsapp: () -> Unit,
    onShareAllApps: () -> Unit,
    onShareBluetooth: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Column {
                    Text("مشاركة عبر واتساب", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("إرسال مباشر لمحادثات واتساب", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color(0xFF16A34A)
                )
            },
            onClick = {
                onDismiss()
                onShareWhatsapp()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("ai_share_whatsapp_item")
        )
        DropdownMenuItem(
            text = {
                Column {
                    Text("تطبيقات التواصل والرسائل", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("تيليجرام، فيسبوك، بريد، أو أي تطبيق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onClick = {
                onDismiss()
                onShareAllApps()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("ai_share_all_apps_item")
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        DropdownMenuItem(
            text = {
                Column {
                    Text("مشاركة عبر البلوتوث", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("إرسال لاسلكي للأجهزة القريبة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color(0xFF2563EB)
                )
            },
            onClick = {
                onDismiss()
                onShareBluetooth()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("ai_share_bluetooth_item")
        )
    }
}

@Composable
internal fun DeveloperSupportPromptFooter(
    onSupportDeveloperClick: () -> Unit,
    onShareApp: () -> Unit,
    onDismiss: () -> Unit
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
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "دعم وتطوير التطبيق (اختياري)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hide_support_prompt_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إخفاء الرسالة ومواصلة المحادثة",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "إخفاء",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
