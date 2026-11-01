package com.borasarang.daon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 디자인 토큰 — docs/DESIGN.md 단일 출처.
 *
 * v0.8 부터 블랙(기본)·라이트 두 벌을 쓴다. 키오스크로 고정하면 기기 설정에
 * 들어갈 수 없어서 앱 안에서 테마를 바꾼다.
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
    /** accent 를 배경으로 쓸 때 그 위에 얹는 글자색. */
    val onAccent: Color,
    val ok: Color,
    val danger: Color,
    val term: Color,
    val sunday: Color,
    val saturday: Color,
    val rain: Color,
    /** 패널·다이얼로그 뒤를 덮는 반투명 막. */
    val scrim: Color,
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
    onAccent = Color(0xFF0B0D10),
    ok = Color(0xFF57C99B),
    danger = Color(0xFFE2695C),
    term = Color(0xFF9ADB8A),
    sunday = Color(0xFFF07A6A),
    saturday = Color(0xFF82B4FF),
    rain = Color(0xFF7FB0FF),
    scrim = Color(0x99000000),
)

/**
 * 라이트 테마. 종이색 바탕 + 흰 카드.
 *
 * 대비(70대 기준): 본문 16.4:1, 보조 8.4:1, 흐린 글자 6.2:1(흰 카드 기준).
 * 강조색은 밝은 금색 대신 짙은 황토색을 쓴다. 라이트 배경 위에서 강조색을
 * 글자로 써도 8:1 이 나오고, 강조색 바탕 위 글자는 [onAccent] 흰색이다.
 */
val LightKioskColors = KioskColors(
    bg = Color(0xFFF4F1EA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEAE5DB),
    textPrimary = Color(0xFF1A1D21),
    textSecondary = Color(0xFF4B525C),
    textMuted = Color(0xFF5E656F),
    rule = Color(0xFFD9D2C6),
    accent = Color(0xFF7A500F),
    accentSoft = Color(0x337A500F),
    onAccent = Color(0xFFFFFFFF),
    ok = Color(0xFF1F7A4E),
    danger = Color(0xFFB3352A),
    term = Color(0xFF2C6B1E),
    sunday = Color(0xFFB3352A),
    saturday = Color(0xFF1F5FA8),
    rain = Color(0xFF1F5FA8),
    scrim = Color(0x73000000),
)

object Kiosk {
    /**
     * 현재 테마. Compose 상태라서, 이 값을 읽은 화면은 테마를 바꾸면 즉시 다시 그려진다.
     * [applyTheme] 로만 바꾼다(앱 시작 때 저장값을 적용).
     */
    var isLight by mutableStateOf(false)
        private set

    val color: KioskColors
        get() = if (isLight) LightKioskColors else DarkKioskColors

    fun applyTheme(light: Boolean) {
        isLight = light
    }

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
