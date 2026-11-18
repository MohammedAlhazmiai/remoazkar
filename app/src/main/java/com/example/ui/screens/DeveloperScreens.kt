package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.tr
import com.example.ui.theme.GoldAccent
import com.example.util.ShareUtil

/* ------------------------------------------------------------------ */
/*  بيانات المطور — منقولة من تطبيق المطور «ريمو بلاير»                */
/* ------------------------------------------------------------------ */

object DeveloperInfo {
    const val NAME = "محمد عبده علي حسن الحزمي"
    const val SHORT_NAME = "محمد الحزمي"
    const val CREDIT = "برمجة وتطوير المطور محمد الحزمي"
    const val TRANSFER_NUMBER = "752490"
    const val WALLET = "محفظة جيب"
    const val TELEGRAM = "https://t.me/moh_alymani1"
    const val BLOG = "https://mohammedalhzmi.blogspot.com"
    const val PLATFORM = "https://mohammed-alhazmi-ai-complete-1.vercel.app/"
    const val APP_LINK = ShareUtil.APP_LINK
}

/* ------------------------------------------------------------------ */
/*  عن التطبيق                                                         */
/* ------------------------------------------------------------------ */

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onSupportClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onBlogClick: () -> Unit,
    onPlatformClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = tr("about_app"), onBackClick = onBackClick)

            // بطاقة التطبيق
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tr("app_name"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "الإصدار 1.0 · نسخة الإصدار",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "تطبيق إسلامي شامل يجمع القرآن الكريم كاملًا، وأذكار الصباح والمساء،" +
                            " والأدعية والرقية الشرعية، مع قارئ ذكي يقرأ لك أي نص تحدده،" +
                            " وميزات حصرية لا توجد في تطبيق آخر.",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ما يميّز التطبيق
            SectionCard(title = "ما يميّز التطبيق") {
                FeatureLine("القرآن الكريم كاملًا — 114 سورة و6236 آية دون اتصال")
                FeatureLine("حصن المسلم — 132 بابًا و291 ذكرًا مع التشكيل")
                FeatureLine("القارئ الذكي — يقرأ أي نص في أي صفحة")
                FeatureLine("الرقية الشرعية حسب الحالة، والأدعية، والوِرد اليومي")
                FeatureLine("ميزات حصرية: بصمة النور، مسبحة الحركة، الذكر الآن")
                FeatureLine("16 لغة، 8 خطوط عربية، ووضعان نهاري وليلي")
            }

            Spacer(Modifier.height(16.dp))

            // المطور
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF1B4D3E), GoldAccent)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "المطوّر",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                DeveloperInfo.SHORT_NAME,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        DeveloperInfo.CREDIT,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // صفحات المطور
            SectionCard(title = tr("developer")) {
                DeveloperRow(
                    icon = Icons.Default.Favorite,
                    title = tr("support_developer"),
                    subtitle = "دعم اختياري عبر محفظة جيب",
                    onClick = onSupportClick
                )
                DeveloperRow(
                    icon = Icons.Default.Verified,
                    title = tr("telegram_channel"),
                    subtitle = "t.me/moh_alymani1",
                    onClick = onTelegramClick
                )
                DeveloperRow(
                    icon = Icons.Default.Article,
                    title = tr("developer_blog"),
                    subtitle = "mohammedalhzmi.blogspot.com",
                    onClick = onBlogClick
                )
                DeveloperRow(
                    icon = Icons.Default.AutoAwesome,
                    title = tr("developer_platform"),
                    subtitle = "منصة محمد الحزمي للذكاء الاصطناعي",
                    onClick = onPlatformClick
                )
            }

            Spacer(Modifier.height(16.dp))

            // مشاركة التطبيق
            Button(
                onClick = { ShareUtil.share(context, ShareUtil.inviteMessage()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("share_app"), fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(DeveloperInfo.APP_LINK))
                    Toast.makeText(context, "تم نسخ رابط التطبيق", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("نسخ رابط التطبيق", fontSize = 13.sp)
            }

            Text(
                text = "© 2025 ريمواذكار — جميع الحقوق محفوظة\n${DeveloperInfo.CREDIT}",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  ادعم المطور                                                        */
/* ------------------------------------------------------------------ */

@Composable
fun SupportDeveloperScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(
                title = tr("support_developer"),
                subtitle = "${DeveloperInfo.SHORT_NAME} · ${DeveloperInfo.WALLET}",
                onBackClick = onBackClick
            )

            // بطاقة الشكر
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102A42))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF382333), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF7A79),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "شكرًا لدعمك",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "دعمك اختياري ويساعد في تطوير ريمواذكار. اختر الطريقة الأنسب لك لإتمام العملية خارج التطبيق.",
                        color = Color(0xFFC5D6E8),
                        fontSize = 13.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // رمز QR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.jeeb_support_qr),
                        contentDescription = "رمز محفظة جيب لدعم المطور",
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10263B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF44D7A8),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                DeveloperInfo.WALLET,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // خطوات الدعم
            SectionCard(title = "كيفية تقديم الدعم") {
                StepLine("1", "بعد تسجيل الدخول إلى حسابك في جيب، امسح رمز المحفظة لدعم المطور محمد الحزمي بأي مبلغ تختاره.")
                StepLine("2", "أو افتح «تحويلات مالية»، ثم اختر «تحويل إلى مشترك»، وأدخل الرقم التالي في خانة رقم الموبايل:")

                // رقم التحويل
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(DeveloperInfo.TRANSFER_NUMBER))
                            copied = true
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "رقم التحويل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                DeveloperInfo.TRANSFER_NUMBER,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            if (copied) "تم النسخ إلى الحافظة" else "اضغط لنسخ الرقم",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                StepLine("3", "أدخل المبلغ ثم اضغط «استمرار»، وتأكد قبل الإتمام أن اسم المستلم الظاهر هو:")

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            DeveloperInfo.NAME,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(Modifier.padding(14.dp)) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "لا يجمع ريمواذكار بيانات الدفع ولا يعالج أي تحويلات مالية. راجع المبلغ واسم المستلم داخل محفظة جيب قبل التأكيد.",
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
        }
    }
}

/* ------------------------------------------------------------------ */
/*  صفحات منصات المطور                                                 */
/* ------------------------------------------------------------------ */

@Composable
fun DeveloperLinkScreen(
    type: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val (title, subtitle, url, icon, body) = when (type) {
        "telegram" -> DevLink(
            tr("telegram_channel"),
            "قناة المطور على تلجرام",
            DeveloperInfo.TELEGRAM,
            Icons.Default.Verified,
            "تابع قناة المطور على تلجرام لتصلك تحديثات ريمواذكار أولًا بأول،" +
                " وملاحظات الإصدارات الجديدة، والإضافات القادمة، وطرق التواصل المباشر مع المطور."
        )
        "blog" -> DevLink(
            tr("developer_blog"),
            "مدونة المطور على بلوجر",
            DeveloperInfo.BLOG,
            Icons.Default.Article,
            "مدونة المطور محمد الحزمي على بلوجر: مقالات وشروحات ومواضيع تقنية" +
                " وتحديثات المشاريع والتطبيقات التي يطوّرها."
        )
        else -> DevLink(
            tr("developer_platform"),
            "منصة الذكاء الاصطناعي",
            DeveloperInfo.PLATFORM,
            Icons.Default.AutoAwesome,
            "منصة محمد الحزمي للذكاء الاصطناعي — مساحة تجريبية لأدوات ذكية" +
                " وتطبيقات مولّدة بالذكاء الاصطناعي، ومنها هذا التطبيق."
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = title, subtitle = subtitle, onBackClick = onBackClick)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1B4D3E), GoldAccent)),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        body,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            url,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = { ShareUtil.openUrl(context, url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("فتح الرابط", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(url))
                    Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("نسخ الرابط", fontSize = 13.sp)
            }

            Spacer(Modifier.height(26.dp))
        }
    }
}

private data class DevLink(
    val title: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector,
    val body: String
)

/* ------------------------------------------------------------------ */
/*  عناصر مشتركة                                                       */
/* ------------------------------------------------------------------ */

@Composable
fun ScreenHeader(
    title: String,
    onBackClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = tr("back"))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StepLine(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DeveloperRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
    }
}
