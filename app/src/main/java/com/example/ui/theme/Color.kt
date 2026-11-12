package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Palette — Clean Instagram Gallery
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceVariant = Color(0xFFF2F2F5)
val LightOutline = Color(0xFFE6E6EA)
val LightInk = Color(0xFF121212)
val LightSecondaryInk = Color(0xFF737373)

// Dark Palette — OLED Obsidian
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF121214)
val DarkSurfaceVariant = Color(0xFF1E1E24)
val DarkOutline = Color(0xFF28282F)
val DarkInk = Color(0xFFF5F5F7)
val DarkSecondaryInk = Color(0xFFA1A1AA)

// Signature Vibe & Instagram-Inspired Accents
val VibeSocialBlue = Color(0xFF0095F6)
val VibeCoralRed = Color(0xFFFF3040)
val VibeAmber = Color(0xFFF58529)
val VibeMagenta = Color(0xFFDD2A7B)
val VibeViolet = Color(0xFF8134AF)
val VibeIndigo = Color(0xFF515BD4)
val VibeAiViolet = Color(0xFF8B5CF6)
val VibeEmerald = Color(0xFF10B981)

val VibeStoryGradient = Brush.linearGradient(
    colors = listOf(VibeAmber, VibeMagenta, VibeViolet, VibeIndigo)
)

val VibeStudioGradient = Brush.linearGradient(
    colors = listOf(VibeMagenta, VibeAiViolet, VibeSocialBlue)
)
