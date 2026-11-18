package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
      typography = Typography,
      content = content,
  )
}
