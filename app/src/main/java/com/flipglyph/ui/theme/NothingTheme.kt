package com.flipglyph.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Nothing OS's own visual language, not its actual assets: near-monochrome black/white with a
 * single sparse red accent, monospace for anything numeric, tracked-out uppercase labels. The
 * Ndot/NType fonts themselves are proprietary and not bundled — FontFamily.Monospace gets the
 * same "technical readout" feel without shipping a font we don't have rights to.
 */
object NothingColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceVariant = Color(0xFF1C1C1C)
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFFA0A0A0)
    val Divider = Color(0xFF2A2A2A)
    val Red = Color(0xFFD2001A)
}

val NothingDarkColorScheme = darkColorScheme(
    primary = NothingColors.Red,
    onPrimary = NothingColors.White,
    secondary = NothingColors.Gray,
    onSecondary = NothingColors.Black,
    background = NothingColors.Black,
    onBackground = NothingColors.White,
    surface = NothingColors.Surface,
    onSurface = NothingColors.White,
    surfaceVariant = NothingColors.SurfaceVariant,
    onSurfaceVariant = NothingColors.Gray,
    outline = NothingColors.Divider,
    outlineVariant = NothingColors.Divider,
    error = NothingColors.Red,
)

val NothingMono = FontFamily.Monospace

val NothingTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = NothingMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        titleLarge = titleLarge.copy(fontFamily = NothingMono, letterSpacing = 3.sp),
        titleMedium = titleMedium.copy(letterSpacing = 1.5.sp),
        labelLarge = labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = NothingMono),
    )
}
