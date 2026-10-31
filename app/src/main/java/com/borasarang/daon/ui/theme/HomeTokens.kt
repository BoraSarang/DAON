package com.borasarang.daon.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * v0.2 농사 달력 홈 토큰.
 *
 * 기준 화면은 1920×1200, 240dpi(Fire HD 10)다. 디자인 값은 px 단위이므로
 * Compose dp/sp로 옮길 때 1.5로 나눈다. 예: 47px → 31.3sp.
 * (sp는 fontScale 1.0에서 1.5를 곱해 px가 된다.)
 */
object Home {
    val screenPadding = 27.dp
    val panelGap = 16.dp
    val panelRadius = 13.dp
    val topBar = 59.dp
    val settingsHeight = 48.dp
    val dot = 7.dp

    val calHead = 61.dp
    val navSize = 43.dp
    val todayButtonHeight = 38.dp
    val weekdays = 33.dp
    val farmStrip = 65.dp
    val cellPadH = 8.dp
    val cellPadV = 6.dp

    val clockCard = 213.dp
    val cardPadding = 20.dp

    private const val TABULAR = "tnum"

    val date = TextStyle(
        fontSize = 32.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    )
    val sub = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    )
    val month = TextStyle(
        fontSize = 39.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = TABULAR,
    )
    val year = TextStyle(
        fontSize = 23.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TABULAR,
    )
    val weekday = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val dayNumber = TextStyle(
        fontSize = 31.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = TABULAR,
    )
    val lunar = TextStyle(
        fontSize = 17.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = TABULAR,
    )
    val tag = TextStyle(
        fontSize = 14.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
    )
    val farmLabel = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
    )
    val farmText = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val adviceText = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val clockLabel = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
    val clockTime = TextStyle(
        fontSize = 75.sp,
        lineHeight = 75.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-3).sp,
        fontFeatureSettings = TABULAR,
    )
    val clockAmPm = TextStyle(
        fontSize = 25.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val clockSeconds = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TABULAR,
    )
    val clockDay = TextStyle(
        fontSize = 21.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val temp = TextStyle(
        fontSize = 84.sp,
        lineHeight = 84.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-4).sp,
        fontFeatureSettings = TABULAR,
    )
    val condition = TextStyle(
        fontSize = 25.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val hiLo = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = TABULAR,
    )
    val metricKey = TextStyle(
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val metricValue = TextStyle(
        fontSize = 21.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TABULAR,
    )
    val miniDay = TextStyle(
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )
    val miniCondition = TextStyle(
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
    val miniTemp = TextStyle(
        fontSize = 17.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = TABULAR,
    )
}
