package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

private val DarkColorScheme =
    darkColorScheme(
        primary = EmeraldPrimaryDark,
        onPrimary = BackgroundDark,
        secondary = GoldAccentDark,
        onSecondary = BackgroundDark,
        background = BackgroundDark,
        surface = SurfaceDark,
        onBackground = TextLight,
        onSurface = TextLight,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = EmeraldPrimary,
        onPrimary = SurfaceLight,
        secondary = GoldAccent,
        onSecondary = TextDark,
        background = BackgroundLight,
        surface = SurfaceLight,
        onBackground = TextDark,
        onSurface = TextDark,
    )

/**
 * يبني نظام الطباعة انطلاقًا من الخط الافتراضي ثم يستبدل عائلة الخط في كل الأنماط.
 * بهذا ينتقل التطبيق كاملًا إلى الخط العربي المختار دون الحاجة لتعديل كل شاشة.
 */
fun typographyFor(family: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

/**
 * @param pageColor لون خلفية الصفحات الذي يختاره المستخدم (null = اللون الافتراضي).
 * @param fontColor لون الخطوط الذي يختاره المستخدم (null = اللون الافتراضي).
 * @param transparentBackground عند تفعيل خلفية صورة للصفحة الحالية نجعل لون الخلفية شفافًا
 *        حتى تظهر الصورة خلف كل المحتوى.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontFamily: FontFamily = AppFonts.Default,
    pageColor: Color? = null,
    fontColor: Color? = null,
    transparentBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    var scheme = baseScheme

    // ألوان الصفحات: نشتق الأسطح من لون الصفحة نفسه بدرجة اختلاف بسيطة حتى تبقى البطاقات مميّزة.
    if (pageColor != null) {
        val tint = if (darkTheme) Color.White else Color.Black
        scheme = scheme.copy(
            background = pageColor,
            surface = lerp(pageColor, tint, 0.06f),
            surfaceVariant = lerp(pageColor, tint, 0.12f),
        )
    }

    // ألوان الخطوط: تُطبّق على النصوص الأساسية والثانوية في كل الشاشات.
    if (fontColor != null) {
        scheme = scheme.copy(
            onBackground = fontColor,
            onSurface = fontColor,
            onSurfaceVariant = fontColor.copy(alpha = 0.78f),
        )
    }

    val colorScheme =
        if (transparentBackground) {
            scheme.copy(
                background = Color.Transparent,
                // أسطح البطاقات تبقى شبه معتمة لضمان وضوح النصوص فوق الصورة.
                surface = scheme.surface.copy(alpha = 0.94f),
                surfaceVariant = scheme.surfaceVariant.copy(alpha = 0.94f),
            )
        } else {
            scheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(fontFamily),
        content = content,
    )
}
