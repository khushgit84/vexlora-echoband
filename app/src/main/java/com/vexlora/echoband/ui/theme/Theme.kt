package com.vexlora.echoband.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VexloraDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = VexloraDeepBlack,
    primaryContainer = ElectricBlueDim,
    onPrimaryContainer = TextPrimary,
    secondary = VioletAccent,
    onSecondary = VexloraDeepBlack,
    secondaryContainer = VioletAccentDim,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusSpeaking,
    background = VexloraDeepBlack,
    onBackground = TextPrimary,
    surface = VexloraSurface,
    onSurface = TextPrimary,
    surfaceVariant = VexloraSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = VexloraDeepBlack,
    outline = TextTertiary
)

@Composable
fun VexloraEchoBandTheme(content: @Composable () -> Unit) {
    val colorScheme = VexloraDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = VexloraDeepBlack.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = VexloraDeepBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VexloraTypography,
        content = content
    )
}
