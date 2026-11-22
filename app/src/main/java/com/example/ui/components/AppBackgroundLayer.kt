package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import com.example.data.AppBackgroundStore

/**
 * طبقة خلفية صفحة واحدة:
 * - تعرض أولًا الصورة المخصصة التي اختارها المستخدم من معرض الهاتف إن وُجدت وفُعّلت.
 * - وإلا تعرض الخلفية الإسلامية الدينية الملائمة للصفحة عند عدم وجود لون صفحة صلب مخصص.
 * - تحافظ على وضوح النصوص عبر طبقة تعتيم متدرجة في الوضعين الفاتح والداكن.
 */
@Composable
fun AppBackgroundLayer(
    pageKey: String?,
    darkMode: Boolean,
    hasCustomPageColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (pageKey == null || pageKey == "home") return

    val context = LocalContext.current
    val revision = AppBackgroundStore.revision

    val customPath = remember(revision, pageKey) {
        if (AppBackgroundStore.isEnabled(context, pageKey)) {
            AppBackgroundStore.imagePath(context, pageKey)
        } else {
            null
        }
    }

    val scrim = remember(revision) { AppBackgroundStore.scrimPercent(context) / 100f }

    if (customPath != null) {
        val bitmap = remember(customPath) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(customPath, bounds)
                var sample = 1
                val longest = maxOf(bounds.outWidth, bounds.outHeight)
                while (longest / sample > 2200) sample *= 2
                BitmapFactory.decodeFile(
                    customPath,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )?.asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Box(modifier = modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                BackgroundContrastScrim(darkMode = darkMode, alpha = scrim)
            }
            return
        }
    }

    if (hasCustomPageColor) return

    val builtInEnabled = remember(revision) {
        AppBackgroundStore.isIslamicBackgroundsEnabled(context)
    }
    if (!builtInEnabled) return

    val builtInRes = remember(revision, pageKey) {
        AppBackgroundStore.effectiveBuiltInDrawableForPage(context, pageKey)
    } ?: return

    val adaptiveScrim = if (darkMode) maxOf(scrim, 0.56f) else maxOf(scrim, 0.62f)

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = builtInRes,
            animationSpec = tween(durationMillis = 450),
            label = "page_islamic_bg"
        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        BackgroundContrastScrim(darkMode = darkMode, alpha = adaptiveScrim)
    }
}

@Composable
private fun BackgroundContrastScrim(
    darkMode: Boolean,
    alpha: Float
) {
    val veil = if (darkMode) Color(0xFF0A1410) else Color(0xFFF8FBF9)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(veil.copy(alpha = alpha))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to veil.copy(alpha = 0.14f),
                    0.5f to Color.Transparent,
                    1f to veil.copy(alpha = 0.24f)
                )
            )
    )
}
