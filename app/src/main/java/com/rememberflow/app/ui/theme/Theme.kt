package com.rememberflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberflow.app.R

/** Palette: midnight/navy surfaces with electric-violet and cyan accents. */
object Palette {
    val Midnight = Color(0xFF070B1A)
    val Navy = Color(0xFF0E1530)
    val NavyRaised = Color(0xFF141C3D)
    val NavyHigh = Color(0xFF1B2552)
    val Outline = Color(0xFF2A3566)

    val Violet = Color(0xFF8B5CF6)
    val VioletSoft = Color(0xFFB69CFF)
    val Cyan = Color(0xFF22D3EE)
    val CyanSoft = Color(0xFF8DEBFB)

    val TextHigh = Color(0xFFF2F4FF)
    val TextMid = Color(0xFFA9B2D6)
    val TextLow = Color(0xFF6D77A3)

    val Amber = Color(0xFFFBBF24)
    val Rose = Color(0xFFFB7185)
    val Mint = Color(0xFF34D399)
}

object Gradients {
    val Accent = Brush.linearGradient(listOf(Palette.Violet, Palette.Cyan))
    val AccentVertical = Brush.verticalGradient(listOf(Palette.Violet, Palette.Cyan))
    val Hero = Brush.linearGradient(listOf(Color(0xFF1C1458), Color(0xFF0F2250), Color(0xFF0B3A57)))
    val Background = Brush.verticalGradient(listOf(Color(0xFF0B1030), Palette.Midnight))
    val Card = Brush.linearGradient(listOf(Palette.NavyHigh, Palette.NavyRaised))
}

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppColors: ColorScheme = darkColorScheme(
    primary = Palette.Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E2370),
    onPrimaryContainer = Palette.TextHigh,
    secondary = Palette.Cyan,
    onSecondary = Palette.Midnight,
    secondaryContainer = Color(0xFF0E3B4D),
    onSecondaryContainer = Palette.CyanSoft,
    tertiary = Palette.Amber,
    background = Palette.Midnight,
    onBackground = Palette.TextHigh,
    surface = Palette.Navy,
    onSurface = Palette.TextHigh,
    surfaceVariant = Palette.NavyRaised,
    onSurfaceVariant = Palette.TextMid,
    surfaceContainer = Palette.Navy,
    surfaceContainerHigh = Palette.NavyRaised,
    surfaceContainerHighest = Palette.NavyHigh,
    outline = Palette.Outline,
    outlineVariant = Color(0xFF1F2850),
    error = Palette.Rose,
    onError = Palette.Midnight
)

@Composable
fun RememberFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
