package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.HumanAzkarScheduler
import com.example.service.FloatingTasbeehService

private data class TourPageInfo(
    val title: String,
    val badge: String,
    val icon: ImageVector,
    val description: String,
    val bullets: List<String>
)

/**
 * دليل تعريفي يظهر عند فتح الشاشة الرئيسية لأول مرة فقط:
 * يعرّف المستخدم بميزات صفحات التطبيق ثم يشرح تعليمات تفعيل الأذكار الصوتية البشرية في الخلفية.
 */
@Composable
fun HomeOnboardingTourDialog(
    onDismiss: () -> Unit,
    onOpenAudioAzkar: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val pages = remember {
        listOf(
            TourPageInfo(
                title = "القرآن الكريم وخطة الختمة",
                badge = "صفحة 1 من 7",
                icon = Icons.Default.MenuBook,
                description = "مصحف كامل ميسّر القراءة بالرسم العثماني مع أدوات متابعة يومية:",
                bullets = listOf(
                    "قراءة الـ 114 سورة و604 صفحات مع البحث والمفضلة.",
                    "تخصيص نوع الخط وحجمه وألوان الصفحات أو الخلفيات الإسلامية.",
                    "خطة ختم القرآن الذكية لحساب وردك اليومي وتتبّع تقدّمك."
                )
            ),
            TourPageInfo(
                title = "حصن المسلم والأذكار والرقية",
                badge = "صفحة 2 من 7",
                icon = Icons.Default.Security,
                description = "موسوعة متكاملة للأذكار اليومية والتحصين والأدعية المأثورة:",
                bullets = listOf(
                    "أذكار الصباح والمساء والنوم وجميع أبواب حصن المسلم.",
                    "الرقية الشرعية الكاملة وأدعية الأنبياء وأسماء الله الحسنى.",
                    "صفحة أذكارك الخاصة وتذكيراتك المجدولة والورد اليومي."
                )
            ),
            TourPageInfo(
                title = "مواقيت الصلاة والأذان والقبلة",
                badge = "صفحة 3 من 7",
                icon = Icons.Default.Mosque,
                description = "دقة في حساب الأوقات وتنبيه تلقائي بصوت الأذان للصلوات الخمس:",
                bullets = listOf(
                    "حساب مواقيت الصلوات الخمس تلقائيًا حسب موقعك أو يدويًا.",
                    "تنبيه تلقائي عند دخول الوقت بصوت الأذان في الخلفية وقفل الشاشة.",
                    "بوصلة اتجاه القبلة الدقيقة لتحديد القبلة في أي مكان."
                )
            ),
            TourPageInfo(
                title = "المساعد الإسلامي الذكي والقارئ الصوتي",
                badge = "صفحة 4 من 7",
                icon = Icons.Default.Psychology,
                description = "رفيق علمي وإيماني يجيبك بوضوح بالفصحى واللهجات العامية:",
                bullets = listOf(
                    "الفتاوى والمسائل الفقهية، تفسير الأحلام، الخطب، المحاضرات، الرسائل والخواطر.",
                    "يدعم الكتابة والتسجيل الصوتي وإرفاق الصور والبطاقات الدعوية.",
                    "القارئ الذكي العائم للاستماع الصوتي لأي نص في صفحات التطبيق."
                )
            ),
            TourPageInfo(
                title = "المسبحة الإلكترونية والمسبحة العائمة",
                badge = "صفحة 5 من 7",
                icon = Icons.Default.TouchApp,
                description = "تسبيح يومي مريح داخل التطبيق وفوق شاشة الهاتف:",
                bullets = listOf(
                    "عدّاد تسبيح متطور بأهداف (33 / 100 / 500 / 1000) وحفظ المجموع.",
                    "مسبحة عائمة تظهر فوق التطبيقات الأخرى في الخلفية للعدّ في كل وقت."
                )
            ),
            TourPageInfo(
                title = "التقويم الهجري وطريق الناجحين والأدوات",
                badge = "صفحة 6 من 7",
                icon = Icons.Default.CalendarMonth,
                description = "أدوات إضافية تعين المسلم في يومه وحياته:",
                bullets = listOf(
                    "التقويم الهجري والمناسبات الإسلامية وحاسبة العمر وتحويل التاريخ.",
                    "صفحات الرفيق الروحي وطريق الناجحين لرفع الهمة وتزكية النفس."
                )
            ),
            TourPageInfo(
                title = "تعليمات تفعيل الأذكار الصوتية البشرية بالخلفية",
                badge = "خطوة مهمة • 7 من 7",
                icon = Icons.Default.Headphones,
                description = "لتشغيل التسجيلات الصوتية البشرية تلقائيًا في الخلفية وقفل الشاشة دون انقطاع:",
                bullets = listOf(
                    "1. افتح «الأذكار الصوتية ← تبويب تسجيلات بشرية» أو «الإعدادات المتقدمة ← المسبحة والتنبيهات».",
                    "2. فعّل «تشغيل دوري للتسجيلات» وحدد الفاصل الزمني (بالدقائق أو الساعات)، وتأكد من تفعيل «صوت الإشعارات».",
                    "3. امنح التطبيق صلاحية «المنبهات الدقيقة» واضغط «استثناء التطبيق من تحسين البطارية» ليعمل بالخلفية حتى عند إغلاق الشاشة."
                )
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val page = pages[currentIndex]
    val isLastStep = currentIndex == pages.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp)
                .testTag("home_onboarding_tour_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF123B31), Color(0xFF1B5544))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0x33FFD36A)
                            ) {
                                Text(
                                    text = page.badge,
                                    color = Color(0xFFFFE082),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("tour_skip_button")
                            ) {
                                Text("تخطي", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFD36A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = page.icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD36A),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = page.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = page.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 21.sp
                    )

                    page.bullets.forEach { bullet ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bullet,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // مؤشر الصفحات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.indices.forEach { idx ->
                            val active = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(8.dp)
                                    .width(if (active) 22.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    if (isLastStep) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenAudioAzkar,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tour_open_audio_azkar_button")
                            ) {
                                Text("الأذكار الصوتية", fontSize = 12.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tour_open_settings_button")
                            ) {
                                Text("الإعدادات", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentIndex > 0) {
                            OutlinedButton(
                                onClick = { currentIndex-- },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("tour_prev_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("السابق")
                            }
                        } else {
                            Spacer(Modifier.width(8.dp))
                        }

                        Button(
                            onClick = {
                                if (isLastStep) onDismiss() else currentIndex++
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tour_next_button")
                        ) {
                            Text(if (isLastStep) "ابدأ الآن" else "التالي")
                            if (!isLastStep) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * دليل إرشادي يشرح طريقة تفعيل المسبحة العائمة وظهورها فوق شاشة الهاتف في الخلفية.
 */
@Composable
fun TasbeehOverlayGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val overlayGranted = FloatingTasbeehService.canDrawOverlays(context)
    val batteryExempt = HumanAzkarScheduler.isIgnoringBatteryOptimizations(context)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp)
                .testTag("tasbeeh_overlay_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF144235), Color(0xFF1E5D49))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFD36A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFFFFD36A),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "طريقة تفعيل المسبحة فوق شاشة الهاتف بالخلفية",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                            Text(
                                "تبقى معك أثناء تصفحك لأي تطبيق آخر",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val steps = listOf(
                        "1. اضغط على زر «تفعيل المسبحة العائمة» بالأسفل (أو افتح «الإعدادات المتقدمة ← المسبحة العائمة والتنبيهات»).",
                        "2. فعّل إذن «الظهور فوق التطبيقات الأخرى» للتطبيق من إعدادات النظام عند فتح الصفحة تلقائيًا.",
                        "3. فعّل «الاستثناء من تحسين البطارية» حتى لا يغلق النظام المسبحة العائمة أثناء العمل في الخلفية.",
                        "4. للتشغيل بعد الظهور: انقر على المسبحة للتسبيح، اسحبها بإصبع واحد لأي موضع، كبّرها أو صغّرها بإصبعين، واضغط ✕ لإغلاقها."
                    )

                    steps.forEach { stepText ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stepText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (FloatingTasbeehService.canDrawOverlays(context)) {
                                FloatingTasbeehService.setEnabled(context, true)
                                val intent = Intent(context, FloatingTasbeehService::class.java)
                                    .setAction(FloatingTasbeehService.ACTION_START)
                                runCatching { ContextCompat.startForegroundService(context, intent) }
                                Toast.makeText(
                                    context,
                                    "تم تشغيل المسبحة العائمة فوق الشاشة بنجاح",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "يرجى منح إذن الظهور فوق التطبيقات من إعدادات الهاتف.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("tasbeeh_activate_floating_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overlayGranted) "تشغيل المسبحة العائمة الآن"
                            else "منح إذن الظهور فوق التطبيقات",
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!batteryExempt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("tasbeeh_battery_exempt_button")
                        ) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("استثناء من تحسين البطارية (للخلفية)")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = 48.dp)
                            .testTag("tasbeeh_guide_dismiss_button")
                    ) {
                        Text("فهمت")
                    }
                }
            }
        }
    }
}
