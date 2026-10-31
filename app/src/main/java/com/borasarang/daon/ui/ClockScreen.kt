package com.borasarang.daon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.City
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.roundToInt

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

/**
 * 시계 메인 화면. T-017에서는 큰 디지털 시계 하나만 두고,
 * T-020에서 둥근 아날로그(기본)·혼합을 설정으로 고를 수 있게 한다.
 */
@Composable
fun ClockScreen(
    now: LocalDateTime,
    hour24: Boolean,
    korean: Boolean,
    locale: Locale,
    calendar: CalendarData,
    city: City,
    weather: WeatherUiState,
    celsius: Boolean,
    touchStop: Boolean,
    stopped: Boolean,
    onOpenSettings: () -> Unit,
) {
    val timePattern = if (hour24) "HH:mm" else "h:mm"
    val timeText = now.format(DateTimeFormatter.ofPattern(timePattern, locale))
    val amPm = now.format(DateTimeFormatter.ofPattern("a", locale))
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
        )
        Spacer(Modifier.height(Home.panelGap))

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
                Spacer(Modifier.height(22.dp))
                Text(
                    text = dateLine,
                    style = ClockHeroDate,
                    color = Kiosk.color.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(26.dp))
                WeatherLine(city, weather, celsius)
            }
        }

        if (!stopped) {
            RotationHint(
                stringResource(if (touchStop) R.string.screen_touch_stop else R.string.screen_touch_home),
            )
        }
    }
}

@Composable
private fun WeatherLine(city: City, state: WeatherUiState, celsius: Boolean) {
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
            text = stringResource(city.labelRes),
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
