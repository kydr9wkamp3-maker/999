package com.chromecore.camera.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Chromecore X Material3 Theme
 * Always dark — the identity is obsidian chrome.
 */
private val ChromecoreColorScheme = darkColorScheme(
    primary = ChromecoreColors.CyanAccent,
    onPrimary = ChromecoreColors.ObsidianBlack,
    primaryContainer = ChromecoreColors.CyanDeep,
    onPrimaryContainer = ChromecoreColors.ColdWhite,
    secondary = ChromecoreColors.ChromeSilver,
    onSecondary = ChromecoreColors.ObsidianBlack,
    secondaryContainer = ChromecoreColors.GraphiteMid,
    onSecondaryContainer = ChromecoreColors.ColdWhite,
    tertiary = ChromecoreColors.Platinum,
    onTertiary = ChromecoreColors.ObsidianBlack,
    background = ChromecoreColors.ObsidianBlack,
    onBackground = ChromecoreColors.ColdWhite,
    surface = ChromecoreColors.Surface,
    onSurface = ChromecoreColors.OnSurface,
    surfaceVariant = ChromecoreColors.SurfaceVariant,
    onSurfaceVariant = ChromecoreColors.OnSurfaceVariant,
    surfaceTint = ChromecoreColors.CyanAccent,
    outline = ChromecoreColors.GraphiteLight,
    outlineVariant = ChromecoreColors.GraphiteMid,
    error = ChromecoreColors.ErrorRed,
    onError = Color.White,
    scrim = ChromecoreColors.ObsidianDeep
)

private val ChromecoreTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 2.sp,
        color = ChromecoreColors.ColdWhite
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Chromecore shapes — sharp, industrial edges.
 */
private val ChromecoreShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ChromecoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChromecoreColorScheme,
        typography = ChromecoreTypography,
        shapes = ChromecoreShapes,
        content = content
    )
}

/**
 * Chrome gradient brush for decorative elements.
 */
fun chromeBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        ChromecoreColors.ChromeBright,
        ChromecoreColors.ChromeSilver,
        ChromecoreColors.TitaniumGrey,
        ChromecoreColors.GraphiteDark
    )
)

/**
 * Radial glow for focus indicators and buttons.
 */
fun cyanGlowBrush(): Brush = Brush.radialGradient(
    colors = listOf(
        ChromecoreColors.CyanAccent.copy(alpha = 0.3f),
        ChromecoreColors.CyanAccent.copy(alpha = 0.0f)
    )
)


