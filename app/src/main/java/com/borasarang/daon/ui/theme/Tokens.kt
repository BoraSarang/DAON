package com.borasarang.daon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 디자인 토큰 — docs/DESIGN.md 단일 출처. 다크 전용(v0.1).
 */
@Immutable
data class KioskColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val rule: Color,
    val accent: Color,
    val accentSoft: Color,
    val ok: Color,
    val danger: Color,
    val term: Color,
    val sunday: Color,
    val saturday: Color,
    val rain: Color,
)

val DarkKioskColors = KioskColors(
    bg = Color(0xFF0B0D10),
    surface = Color(0xFF12151A),
    surfaceAlt = Color(0xFF171B21),
    textPrimary = Color(0xFFF2F4F7),
    textSecondary = Color(0xFFA7AFBB),
    textMuted = Color(0xFF6E7683),
    rule = Color(0xFF232932),
    accent = Color(0xFFE8B45A),
    accentSoft = Color(0x26E8B45A),
    ok = Color(0xFF57C99B),
    danger = Color(0xFFE2695C),
    term = Color(0xFF9ADB8A),
    sunday = Color(0xFFF07A6A),
    saturday = Color(0xFF82B4FF),
    rain = Color(0xFF7FB0FF),
)

object Kiosk {
    val color = DarkKioskColors

    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    val screenPadding = 40.dp
    val panelGap = 32.dp

    val radiusSm = 10.dp
    val radiusMd = 16.dp
    val radiusLg = 24.dp
    val hairline = 1.dp
    val targetMin = 48.dp
}

private const val TABULAR = "tnum"

val KioskTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 132.sp,
        lineHeight = 132.sp,
        letterSpacing = (-4).sp,
        fontFeatureSettings = TABULAR,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 88.sp,
        lineHeight = 92.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = TABULAR,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = TABULAR,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp,
    ),
)
