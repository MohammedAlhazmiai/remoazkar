package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay

private const val SPLASH_SECONDS = 5

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        EmeraldPrimary,
                        BackgroundDark
                    )
                )
            )
            .clickable {
                if (!finished) {
                    finished = true
                    onEnterClick()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // الصورة المرفقة بحجمها الطبيعي (نسبة الأبعاد الأصلية) دون تكبير
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1659f / 948f),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_entry),
                    contentDescription = "شعار ريمواذكار",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ريمواذكار",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "التطبيق الشامل للأذكار والأدعية وحصن المسلم",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )
        }

        // شريط التقدم الرقمي أسفل شاشة الدخول
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp),
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
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = GoldAccent,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "اضغط في أي مكان للدخول مباشرة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
