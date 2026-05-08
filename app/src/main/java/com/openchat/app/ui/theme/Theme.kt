package com.openchat.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealGreen,
    secondary = TealGreenDark,
    tertiary = TealGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = ErrorRed,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealGreen,
    secondary = TealGreenDark,
    tertiary = TealGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = BackgroundLight,
    onSecondary = BackgroundLight,
    onTertiary = BackgroundLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = ErrorRed,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun OpenChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
