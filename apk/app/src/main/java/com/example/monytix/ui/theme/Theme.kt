package com.example.monytix.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
                    colors = listOf(BackgroundGradientTop, BackgroundGradientBottom)
                )
            )
    ) {
        content()
    }
}

private val MonytixDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentMuted,
    onPrimaryContainer = AccentPrimary,
    secondary = AccentPrimary.copy(alpha = 0.9f),
    onSecondary = Color.White,
    tertiary = AccentSecondary,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = GlassCard,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = Error,
    outline = Color.White.copy(alpha = 0.12f)
)

private val MonytixLightColorScheme = lightColorScheme(
    primary = Color(0xFFB8860B),
    onPrimary = Color.White,
    primaryContainer = AccentMuted,
    onPrimaryContainer = Color(0xFF2E2E2E),
    secondary = Color(0xFFB8860B),
    onSecondary = Color.White,
    tertiary = AccentSecondary,
    onTertiary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = Error,
    outline = Color(0xFF79747E)
)

@Composable
fun MonytixTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> MonytixDarkColorScheme
        else -> MonytixLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MonytixShapes,
        content = content
    )
}
