package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZapyaXDarkColorScheme = darkColorScheme(
    primary = ZapyaOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A1800),
    onPrimaryContainer = ZapyaOrangeGlow,
    secondary = ZapyaPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = ZapyaPurpleGlow,
    tertiary = EmeraldShield,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF063B2C),
    onTertiaryContainer = EmeraldShield,
    background = VoidDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1F163D),
    onSurfaceVariant = TextSecondary,
    error = NeonCoral,
    onError = Color.White,
    outline = BorderSubtle,
    outlineVariant = Color(0x33FF6B00)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZapyaXDarkColorScheme,
        typography = Typography,
        content = content
    )
}
