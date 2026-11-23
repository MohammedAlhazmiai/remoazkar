package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PendingAttachment(
    val type: String, // "image", "audio"
    val uri: Uri,
    val displayName: String
)

@Composable
fun AttachmentOptionsButton(
    onPickImage: () -> Unit,
    onPickAudio: () -> Unit,
    onDesignCardPrompt: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("ai_attach_button")
        ) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = "إرفاق صورة أو صوت أو طلب تصميم بطاقة",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text("رفع صورة من الهاتف", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onPickImage()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("attach_menu_image")
            )
            DropdownMenuItem(
                text = {
                    Text("رفع ملف صوتي من الهاتف", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onPickAudio()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("attach_menu_audio")
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            DropdownMenuItem(
                text = {
                    Text("تصميم بطاقة دعوية إسلامية", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                onClick = {
                    expanded = false
                    onDesignCardPrompt()
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("attach_menu_card")
            )
        }
    }
}

@Composable
fun SelectedAttachmentChip(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("selected_attachment_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (attachment.type == "image") Icons.Default.Image else Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = attachment.displayName,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "إلغاء المرفق",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
