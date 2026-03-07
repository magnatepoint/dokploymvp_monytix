package com.example.monytix.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PremiumGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, SurfacePrimary)
                )
            )
    ) {
        content()
    }
}

private val MonytixDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    background = Background,
    surface = SurfacePrimary,
    onPrimary = Color(0xFF0B1220),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = Color.White,
    primaryContainer = CyanGlow,
    onPrimaryContainer = CyanPrimary,
    secondaryContainer = CyanGlow,
    onSecondaryContainer = CyanSecondary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    outline = BorderSubtle
)

@Composable
fun MonytixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonytixDarkColorScheme,
        typography = Typography,
        shapes = MonytixShapes,
        content = content
    )
}
