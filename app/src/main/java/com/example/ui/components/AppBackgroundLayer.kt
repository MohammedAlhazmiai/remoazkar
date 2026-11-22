package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.data.AppBackgroundStore

/**
 * طبقة خلفية التطبيق: تعرض الصورة التي اختارها المستخدم من معرض الهاتف خلف كل الشاشات،
 * مع طبقة تعتيم متدرّجة تحافظ على وضوح النصوص في الوضعين الفاتح والداكن.
 */
@Composable
fun AppBackgroundLayer(darkMode: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val revision = AppBackgroundStore.revision
    val enabled = AppBackgroundStore.isEnabled(context)
    val path = remember(revision, enabled) {
        if (enabled) AppBackgroundStore.imagePath(context) else null
    }
    if (path == null) return

    val scrim = AppBackgroundStore.scrimPercent(context) / 100f

    val bitmap = remember(path) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > 2200) sample *= 2
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )?.asImageBitmap()
        }.getOrNull()
    } ?: return

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // تعتيم لضمان قراءة النصوص فوق الصورة.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkMode) Color.Black.copy(alpha = scrim)
                    else Color.White.copy(alpha = scrim)
                )
        )
        // تدرّج خفيف أسفل الشاشة يعطي عمقًا بصريًا دون إخفاء الصورة.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to (if (darkMode) Color.Black else Color.White).copy(alpha = 0.22f)
                    )
                )
        )
    }
}
