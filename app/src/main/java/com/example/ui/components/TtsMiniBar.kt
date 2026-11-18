package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TtsManager

/**
 * مشغّل قراءة الروبوت العائم — يظهر في كل شاشات التطبيق أثناء القراءة.
 *
 * يحتوي على:
 *  - شريط تقدّم يمكن سحبه لتقديم القراءة أو تأخيرها فعليًا.
 *  - زر تشغيل/إيقاف مؤقت.
 *  - زر إيقاف نهائي.
 */
@Composable
fun TtsMiniBar(modifier: Modifier = Modifier) {
    val active = TtsManager.isSpeaking || TtsManager.isPaused
    if (!active) return

    // قيمة محلية للسحب حتى لا يقفز الشريط أثناء سحب المستخدم
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) dragValue else TtsManager.progress

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (TtsManager.isPaused) "الروبوت متوقف مؤقتًا" else "الروبوت يقرأ الآن…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = TtsManager.currentSnippet,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // تشغيل / إيقاف مؤقت
                FilledIconButton(
                    onClick = { TtsManager.togglePlayPause() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (TtsManager.isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (TtsManager.isSpeaking) "إيقاف مؤقت" else "متابعة",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(6.dp))

                // إيقاف نهائي
                FilledIconButton(
                    onClick = { TtsManager.stop() },
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إيقاف",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // شريط التقديم القابل للسحب
            Slider(
                value = shown,
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    TtsManager.seekTo(dragValue)
                    dragging = false
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                )
            )

            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "${(shown * 100).toInt()}٪",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "اسحب الشريط للتقديم أو التأخير",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
