package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay

/** مدة العدّاد الرقمي لشاشة الدخول بالثواني. */
private const val SPLASH_SECONDS = 5

/**
 * شاشة دخول التطبيق.
 *
 * تعرض الصورة العمودية المرفقة بنسبتها الأصلية 1:1 (بدون تكبير أو تشويه أو قص)،
 * فوقها خط دخول رقمي (عدّاد تنازلي + شريط تقدّم) وزر دخول نابض مطابق للزر
 * الموجود في الصورة نفسها.
 *
 * لا تُنفّذ أي عملية قراءة/كتابة قد ترمي استثناءً، لذلك لا يمكنها إسقاط التطبيق.
 */
@Composable
fun SplashScreen(
    onEnterClick: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(SPLASH_SECONDS) }
    var finished by remember { mutableStateOf(false) }

    // عدّاد تنازلي رقمي: عند الوصول إلى صفر ينتقل تلقائيًا للشاشة الرئيسية
    LaunchedEffect(Unit) {
        for (i in SPLASH_SECONDS downTo 1) {
            remaining = i
            delay(1000)
        }
        remaining = 0
        if (!finished) {
            finished = true
            onEnterClick()
        }
    }

    val progress by animateFloatAsState(
        targetValue = (SPLASH_SECONDS - remaining).toFloat() / SPLASH_SECONDS,
        animationSpec = tween(durationMillis = 900),
        label = "splash_progress"
    )

    val pulse by rememberInfiniteTransition(label = "entryPulse").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "entryPulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!finished) {
                    finished = true
                    onEnterClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // الصورة العمودية بحجمها الطبيعي 1:1 — لا تكبير ولا تشويه
            val artSize = minOf(maxWidth, maxHeight)

            Image(
                painter = painterResource(id = R.drawable.splash_portrait),
                contentDescription = "شاشة الدخول - ريمواذكار",
                modifier = Modifier
                    .width(artSize)
                    .height(artSize),
                contentScale = ContentScale.Fit
            )

            // زر الدخول النابض فوق الزر الذهبي المرسوم في الصورة نفسها
            val btnLeftFraction = 0.203f
            val btnTopFraction = 0.728f
            val btnWidthFraction = 0.598f
            val btnHeightFraction = 0.088f

            val offsetX = artSize * ((btnLeftFraction + btnWidthFraction / 2f) - 0.5f)
            val offsetY = artSize * ((btnTopFraction + btnHeightFraction / 2f) - 0.5f)

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .width(artSize * btnWidthFraction)
                    .height(artSize * btnHeightFraction)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Transparent)
                    .semantics {
                        role = Role.Button
                        contentDescription = "دخول التطبيق"
                    }
            ) {
                // إطار ذهبي نابض يلفت النظر إلى زر الدخول
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    GoldAccent.copy(alpha = pulse * 0.22f),
                                    GoldAccent.copy(alpha = pulse * 0.08f)
                                )
                            )
                        )
                )
            }
        }

        // خط الدخول الرقمي أسفل الشاشة
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (remaining > 0) "جارٍ الدخول… $remaining" else "مرحبًا بك",
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = GoldAccent,
                trackColor = Color.White.copy(alpha = 0.18f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "اضغط في أي مكان للدخول مباشرة",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
