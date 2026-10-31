package com.borasarang.daon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.core.ClockFace
import com.borasarang.daon.data.Place
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ClockHero = TextStyle(
    fontSize = 210.sp,
    lineHeight = 210.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-10).sp,
    fontFeatureSettings = "tnum",
)

private val ClockHeroAmPm = TextStyle(
    fontSize = 52.sp,
    lineHeight = 58.sp,
    fontWeight = FontWeight.SemiBold,
)

private val ClockHeroSeconds = TextStyle(
    fontSize = 30.sp,
    lineHeight = 36.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

private val ClockHeroDate = TextStyle(
    fontSize = 34.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.SemiBold,
)

private val ClockCompact = TextStyle(
    fontSize = 118.sp,
    lineHeight = 118.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-6).sp,
    fontFeatureSettings = "tnum",
)

private val ClockCompactAmPm = TextStyle(
    fontSize = 30.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.SemiBold,
)

private val ClockCompactSeconds = TextStyle(
    fontSize = 22.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

/**
 * 시계 메인 화면. 설정에서 고른 모양(둥근 아날로그·큰 디지털·혼합)을 그린다.
 * 기본값은 어르신이 멀리서도 읽기 쉬운 둥근 아날로그다.
 */
@Composable
fun ClockScreen(
    now: LocalDateTime,
    hour24: Boolean,
    korean: Boolean,
    locale: Locale,
    calendar: CalendarData,
    place: Place,
    weather: WeatherUiState,
    celsius: Boolean,
    clockFace: ClockFace,
    touchStop: Boolean,
    stopped: Boolean,
    includeRadio: Boolean = true,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
) {
    val clockDatePattern = stringResource(R.string.pattern_clock_date)
    val clockDate = now.format(DateTimeFormatter.ofPattern(clockDatePattern, locale))
    val weekday = now.dayOfWeek.getDisplayName(DateTextStyle.FULL, locale)
    val lunar = calendar.lunarOn(now.toLocalDate())
    val activeTerm = calendar.activeTerm(now.toLocalDate())?.second
    val termDetail = activeTerm?.let { calendar.termDetail(it.index, korean) }

    val dateLine = buildAnnotatedString {
        append("$weekday · $clockDate")
        if (lunar != null) {
            append("  ·  ")
            append(if (korean) "음력 ${lunar.month}월 ${lunar.day}일" else "Lunar ${lunar.month}/${lunar.day}")
            if (lunar.leap) append(if (korean) " 윤달" else " leap")
        }
        if (activeTerm != null) {
            append("  ·  ")
            withStyle(SpanStyle(color = Kiosk.color.term, fontWeight = FontWeight.Bold)) {
                append(activeTerm.displayName(korean))
            }
            if (termDetail != null) append(" ${termDetail.summary}")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_clock),
            subtitle = null,
            current = ScreenMode.CLOCK,
            onOpenSettings = onOpenSettings,
            onGoHome = onGoHome,
            includeRadio = includeRadio,
        )
        Spacer(Modifier.height(Home.panelGap))

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (clockFace) {
                ClockFace.ANALOG -> AnalogClock(
                    now = now,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                )
                ClockFace.DIGITAL -> DigitalClock(
                    now = now,
                    hour24 = hour24,
                    locale = locale,
                )
                ClockFace.HYBRID -> HybridClock(
                    now = now,
                    hour24 = hour24,
                    locale = locale,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dateLine,
                style = ClockHeroDate,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            WeatherLine(place, weather, celsius)
        }
        Spacer(Modifier.height(12.dp))

        if (!stopped) {
            RotationHint(
                stringResource(if (touchStop) R.string.screen_touch_stop else R.string.screen_touch_home),
            )
        }
    }
}

@Composable
private fun DigitalClock(now: LocalDateTime, hour24: Boolean, locale: Locale) {
    val timeText = now.format(DateTimeFormatter.ofPattern(if (hour24) "HH:mm" else "h:mm", locale))
    val amPm = now.format(DateTimeFormatter.ofPattern("a", locale))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!hour24) {
                Text(
                    text = amPm,
                    style = ClockHeroAmPm,
                    color = Kiosk.color.textSecondary,
                    modifier = Modifier.padding(end = 16.dp, bottom = 34.dp),
                )
            }
            Text(
                text = timeText,
                style = ClockHero,
                color = Kiosk.color.textPrimary,
            )
        }
        Text(
            text = stringResource(R.string.label_seconds, now.second),
            style = ClockHeroSeconds,
            color = Kiosk.color.textMuted,
        )
    }
}

@Composable
private fun HybridClock(now: LocalDateTime, hour24: Boolean, locale: Locale) {
    val timeText = now.format(DateTimeFormatter.ofPattern(if (hour24) "HH:mm" else "h:mm", locale))
    val amPm = now.format(DateTimeFormatter.ofPattern("a", locale))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        AnalogClock(now = now, modifier = Modifier.fillMaxHeight().aspectRatio(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!hour24) {
                Text(
                    text = amPm,
                    style = ClockCompactAmPm,
                    color = Kiosk.color.textSecondary,
                )
            }
            Text(
                text = timeText,
                style = ClockCompact,
                color = Kiosk.color.textPrimary,
            )
            Text(
                text = stringResource(R.string.label_seconds, now.second),
                style = ClockCompactSeconds,
                color = Kiosk.color.textMuted,
            )
        }
    }
}

/**
 * 둥근 아날로그. 60개 눈금, 12개 숫자, 시·분·초침을 그린다.
 * 12·3·6·9 숫자는 더 크고 밝게 강조한다.
 */
@Composable
private fun AnalogClock(now: LocalDateTime, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = Kiosk.color.textSecondary,
        fontFeatureSettings = "tnum",
    )
    val majorNumberStyle = numberStyle.copy(
        fontSize = 46.sp,
        color = Kiosk.color.textPrimary,
    )

    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringWidth = 2.dp.toPx()

        drawCircle(Kiosk.color.surface, radius - ringWidth / 2f, center)
        drawCircle(Kiosk.color.rule, radius - ringWidth / 2f, center, style = Stroke(ringWidth))
        drawCircle(Kiosk.color.rule.copy(alpha = 0.6f), radius * 0.90f, center, style = Stroke(1.dp.toPx()))

        for (tick in 0 until 60) {
            val angle = Math.toRadians((tick * 6 - 90).toDouble())
            val major = tick % 5 == 0
            val inner = radius * if (major) 0.84f else 0.87f
            val outer = radius * 0.89f
            drawLine(
                color = if (major) Kiosk.color.textMuted else Kiosk.color.rule,
                start = center + Offset((cos(angle) * inner).toFloat(), (sin(angle) * inner).toFloat()),
                end = center + Offset((cos(angle) * outer).toFloat(), (sin(angle) * outer).toFloat()),
                strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        for (hour in 1..12) {
            val angle = Math.toRadians((hour * 30 - 90).toDouble())
            val style = if (hour % 3 == 0) majorNumberStyle else numberStyle
            val layout = textMeasurer.measure(hour.toString(), style = style)
            val distance = radius * 0.70f
            val position = center + Offset(
                (cos(angle) * distance).toFloat(),
                (sin(angle) * distance).toFloat(),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    position.x - layout.size.width / 2f,
                    position.y - layout.size.height / 2f,
                ),
            )
        }

        val hourAngle = Math.toRadians(((now.hour % 12 + now.minute / 60f) * 30f - 90f).toDouble())
        val minuteAngle = Math.toRadians(((now.minute + now.second / 60f) * 6f - 90f).toDouble())
        val secondAngle = Math.toRadians((now.second * 6f - 90f).toDouble())

        fun pointAt(angle: Double, length: Float) = center + Offset(
            (cos(angle) * length).toFloat(),
            (sin(angle) * length).toFloat(),
        )

        drawLine(
            color = Kiosk.color.textPrimary,
            start = center,
            end = pointAt(hourAngle, radius * 0.48f),
            strokeWidth = 14.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Kiosk.color.textPrimary,
            start = center,
            end = pointAt(minuteAngle, radius * 0.66f),
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Kiosk.color.accent,
            start = center,
            end = pointAt(secondAngle, radius * 0.80f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(Kiosk.color.accent, 8.dp.toPx(), center)
        drawCircle(Kiosk.color.surface, 3.5.dp.toPx(), center)
    }
}

@Composable
private fun WeatherLine(place: Place, state: WeatherUiState, celsius: Boolean) {
    val snapshot = state.snapshot ?: return
    val current = snapshot.current
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Kiosk.color.surface)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = place.label(),
            style = Home.condition,
            color = Kiosk.color.textSecondary,
        )
        Text(
            text = "${current.temperature.roundToInt()}°$unit",
            style = Home.condition,
            color = Kiosk.color.textPrimary,
        )
        Text(
            text = stringResource(weatherLabelRes(current.weatherCode)),
            style = Home.condition,
            color = Kiosk.color.textSecondary,
        )
    }
}
