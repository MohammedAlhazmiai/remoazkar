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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontFamily: FontFamily = AppFonts.Default,
    content: @Composable () -> Unit,
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  MaterialTheme(
      colorScheme = colorScheme,
      typography = typographyFor(fontFamily),
      content = content,
  )
}
