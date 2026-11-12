package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VibeDarkColorScheme = darkColorScheme(
    primary = VibeSocialBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A2A3D),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = VibeMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF331826),
    onSecondaryContainer = Color(0xFFFFD8E8),
    tertiary = VibeAiViolet,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryInk,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = VibeCoralRed,
    onError = Color.White
)

private val VibeLightColorScheme = lightColorScheme(
    primary = VibeSocialBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF00325B),
    secondary = VibeMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color(0xFF560027),
    tertiary = VibeAiViolet,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryInk,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = VibeCoralRed,
    onError = Color.White
)

val VibeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VibeDarkColorScheme else VibeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VibeTypography,
        shapes = VibeShapes,
        content = content
    )
}
