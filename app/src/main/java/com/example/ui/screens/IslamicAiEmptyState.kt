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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class QuickCapabilityPrompt(
    val category: String,
    val prompt: String
)

private val QUICK_PROMPTS = listOf(
    QuickCapabilityPrompt("تصميم بطاقة", "صمم لي بطاقة دعوية إسلامية عن جمعة مباركة"),
    QuickCapabilityPrompt("رسالة دينية", "اكتب رسالة دينية للتهنئة بحلول رمضان"),
    QuickCapabilityPrompt("خاطرة إيمانية", "اكتب خاطرة إيمانية عن التوكل والفرج بعد الضيق"),
    QuickCapabilityPrompt("خطبة جمعة", "اكتب خطبة جمعة كاملة عن تقوى الله وبر الوالدين"),
    QuickCapabilityPrompt("فتوى فقهية", "ما حكم بخاخ الربو وقطرة العين في نهار رمضان؟"),
    QuickCapabilityPrompt("تفسير حلم", "حلمت أني أطير تحت المطر وقرأت القرآن"),
    QuickCapabilityPrompt("سجود السهو", "متى يكون سجود السهو قبل السلام ومتى يكون بعده؟"),
    QuickCapabilityPrompt("كفارة اليمين", "ما كفارة اليمين بالله ومتى يجوز صيام ثلاثة أيام؟")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiEmptyStateView(
    modifier: Modifier = Modifier,
    onPromptClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
                                        "مدرّب ومقتصر حصريًا على العلوم الدينية والشرعية",
                                        color = Color(0xFFDCECE2),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Text(
                            "يؤلف الرسائل والخطب والخواطر، ويجيب عن الفتاوى بالأدلة القرآنية والنبوية، ويفسر الأحلام، ويصمم بطاقات دعوية بدقة عالية، ويقرأ الصور والتسجيلات الصوتية مع حفظ الردود للهاتف.",
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
                "أمثلة ونماذج للبدء:",
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
                        modifier = Modifier.clickable { onPromptClick(sample.prompt) },
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
}
