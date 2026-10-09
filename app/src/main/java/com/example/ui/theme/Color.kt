package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ZapyaX Obsidian Dark Palette with Purple & Orange Hue
val VoidDark = Color(0xFF080512)
val ObsidianDark = Color(0xFF0E0A1F)
val SurfaceDark = Color(0xFF150F2C)
val SurfaceTranslucent = Color(0xCC150F2C)
val AcrylicCardBg = Color(0xB81A1238)
val AcrylicCardBorder = Color(0x33A855F7)
val AcrylicHighlight = Color(0x1AFFFFFF)

// ZapyaX Iconic Purple and Orange Gradient Accents
val ZapyaPurple = Color(0xFFA855F7)
val ZapyaPurpleDim = Color(0xFF7E22CE)
val ZapyaPurpleGlow = Color(0xFFC084FC)

val ZapyaOrange = Color(0xFFFF6B00)
val ZapyaOrangeDim = Color(0xFFEA580C)
val ZapyaOrangeGlow = Color(0xFFFF8A3D)

// Dual Channel Indicators: Channel 1 is Purple, Channel 2 is Orange
val ChannelOneColor = Color(0xFFA855F7) // Purple Stream
val ChannelTwoColor = Color(0xFFFF6B00) // Orange Stream

// Security
val EmeraldShield = Color(0xFF10B981)
val EmeraldShieldDim = Color(0xFF059669)
val NeonCoral = Color(0xFFFF3366)

// Theme compatibility aliases mapped to Purple & Orange aesthetic
val NeonCyan = ZapyaOrangeGlow
val NeonCyanDim = ZapyaOrangeDim
val CyberViolet = ZapyaPurple
val VoidBlack = VoidDark

// Text & Neutral
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFFCBD5E1)
val TextTertiary = Color(0xFF94A3B8)
val BorderSubtle = Color(0x33A855F7)

// Common Gradients
val ZapyaGradient = Brush.linearGradient(
    listOf(ZapyaPurple, ZapyaOrange)
)
val ZapyaRadialGlow = Brush.radialGradient(
    colors = listOf(
        Color(0x33A855F7),
        Color(0x1AFF6B00),
        Color.Transparent
    ),
    radius = 900f
)
