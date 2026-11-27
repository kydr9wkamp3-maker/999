package com.chromecore.camera.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * CHROMECORE X — Obsidian Chrome Design System
 * 
 * Visual identity: liquid chrome, polished metal, reflective surfaces.
 * Black, graphite, silver, cold white, subtle cyan accent.
 */
object ChromecoreColors {

    // Core blacks
    val ObsidianBlack = Color(0xFF0A0A0C)
    val ObsidianDeep = Color(0xFF050507)

    // Graphite
    val GraphiteDark = Color(0xFF1A1A1F)
    val GraphiteMid = Color(0xFF2C2C33)
    val GraphiteLight = Color(0xFF3A3A42)

    // Chrome & Metal
    val ChromeSilver = Color(0xFFC8CCD0)
    val ChromeBright = Color(0xFFE8EAED)
    val Platinum = Color(0xFFD4D7DC)
    val TitaniumGrey = Color(0xFF4A4A52)
    val ColdWhite = Color(0xFFF5F5F7)

    // Accent
    val CyanAccent = Color(0xFF4DD0E1)
    val CyanGlow = Color(0xFF80DEEA)
    val CyanDeep = Color(0xFF00838F)

    // Status
    val ErrorRed = Color(0xFFFF5252)
    val SuccessGreen = Color(0xFF69F0AE)
    val WarningAmber = Color(0xFFFFD740)

    // Chrome gradients (for brush use)
    val ChromeGradientStart = Color(0xFF2C2C33)
    val ChromeGradientEnd = Color(0xFF1A1A1F)
    val MetalHighlight = Color(0xFFFFFFFF)
    val MetalShadow = Color(0xFF0D0D10)

    // Surface colors for Material
    val Surface = Color(0xFF121215)
    val SurfaceVariant = Color(0xFF1E1E24)
    val SurfaceHigh = Color(0xFF252530)
    val OnSurface = ColdWhite
    val OnSurfaceVariant = ChromeSilver
}
