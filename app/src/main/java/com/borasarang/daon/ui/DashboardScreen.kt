package com.borasarang.daon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.data.City
import com.borasarang.daon.data.DailyForecast
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.displayName
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * v0.2 달력 홈. 달력 70% + 오른쪽 시계·날씨 30%.
 * 70대 사용자를 기준으로 큰 글씨와 큰 터치 영역을 쓴다.
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel, onOpenSettings: () -> Unit) {
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val city by vm.city.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()
    val korean = locale.language == "ko"
    val today = now.toLocalDate()

    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        HomeTopBar(
            now = now,
            calendar = calendar,
            korean = korean,
            locale = locale,
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(Home.panelGap))

        Row(Modifier.fillMaxWidth().weight(1f)) {
            CalendarPanel(
                today = today,
                calendar = calendar,
                korean = korean,
                locale = locale,
                modifier = Modifier.weight(0.7f).fillMaxHeight(),
            )
            Spacer(Modifier.width(Home.panelGap))
            Column(
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Home.panelGap),
            ) {
                ClockCard(
                    now = now,
                    hour24 = vm.hour24,
                    korean = korean,
                    locale = locale,
                    modifier = Modifier.fillMaxWidth().height(Home.clockCard),
                )
                WeatherCard(
                    city = city,
                    state = weather,
                    celsius = vm.celsius,
                    locale = locale,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    now: LocalDateTime,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    onOpenSettings: () -> Unit,
) {
    val today = now.toLocalDate()
    val headerPattern = stringResource(R.string.pattern_date_header)
    val dateText = remember(today, headerPattern, locale) {
        now.format(DateTimeFormatter.ofPattern(headerPattern, locale))
    }
    val weekday = remember(today, locale) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    }
    val lunar = calendar.lunarOn(today)
    val active = calendar.activeTerm(today)
    val activeDetail = active?.let { calendar.termDetail(it.second.index, korean) }
    val subText = buildAnnotatedString {
        if (lunar != null) {
            append(
                if (korean) {
                    "음력 ${lunar.month}월 ${lunar.day}일"
                } else {
                    "Lunar ${lunar.month}/${lunar.day}"
                },
            )
            if (lunar.leap) {
                append(if (korean) " 윤달" else " leap")
            }
        }
        if (active != null) {
            if (length > 0) append(" · ")
            withStyle(SpanStyle(color = Kiosk.color.term, fontWeight = FontWeight.Bold)) {
                append(active.second.displayName(korean))
            }
            if (activeDetail != null) {
                append(" · ")
                append(activeDetail.summary)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(Home.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (korean) {
                Row {
                    Text(dateText, style = Home.date, color = Kiosk.color.textPrimary)
                    Text(" $weekday", style = Home.date, color = Kiosk.color.accent)
                }
            } else {
                Text(
                    text = "$weekday, $dateText",
                    style = Home.date,
                    color = Kiosk.color.textPrimary,
                    maxLines = 1,
                )
            }
            if (subText.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subText,
                    style = Home.sub,
                    color = Kiosk.color.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        ScreenDots()
        Spacer(Modifier.width(18.dp))
        Box(
            modifier = Modifier
                .height(Home.settingsHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(Kiosk.color.surface)
                .border(1.dp, Kiosk.color.rule, RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.label_settings),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Kiosk.color.textSecondary,
            )
        }
    }
}

/** 화면 점 3개. 장식이며 터치 대상이 아니다(달력 화면이 첫 번째). */
@Composable
private fun ScreenDots() {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(Home.dot)
                    .clip(CircleShape)
                    .background(if (index == 0) Kiosk.color.accent else Kiosk.color.rule),
            )
        }
    }
}

@Composable
private fun ClockCard(
    now: LocalDateTime,
    hour24: Boolean,
    korean: Boolean,
    locale: Locale,
    modifier: Modifier,
) {
    val timeText = now.format(
        DateTimeFormatter.ofPattern(if (hour24) "HH:mm" else "h:mm", locale),
    )
    val amPm = now.format(DateTimeFormatter.ofPattern("a", locale))
    val clockDatePattern = stringResource(R.string.pattern_clock_date)
    val clockDate = now.format(DateTimeFormatter.ofPattern(clockDatePattern, locale))
    val weekday = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    Surface(
        modifier = modifier,
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadius),
        border = androidx.compose.foundation.BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Home.cardPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.label_now),
                style = Home.clockLabel,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                if (!hour24) {
                    Text(
                        text = amPm,
                        style = Home.clockAmPm,
                        color = Kiosk.color.textSecondary,
                        modifier = Modifier.padding(end = 8.dp, bottom = 10.dp),
                    )
                }
                Text(
                    text = timeText,
                    style = Home.clockTime,
                    color = Kiosk.color.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.label_seconds, now.second),
                style = Home.clockSeconds,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$weekday · $clockDate",
                style = Home.clockDay,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherCard(
    city: City,
    state: WeatherUiState,
    celsius: Boolean,
    locale: Locale,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadius),
        border = androidx.compose.foundation.BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(Home.cardPadding)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${stringResource(city.labelRes)} · ${stringResource(R.string.label_now)}",
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_refresh),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Kiosk.color.accent,
                    )
                }
            }

            val snapshot = state.snapshot
            when {
                snapshot != null -> WeatherContent(snapshot, celsius, locale)
                state.isLoading -> Text(
                    text = stringResource(R.string.weather_loading),
                    style = Home.condition,
                    color = Kiosk.color.textMuted,
                )
                else -> Text(
                    text = stringResource(state.errorRes ?: R.string.error_weather_network),
                    style = Home.condition,
                    color = Kiosk.color.danger,
                )
            }

            if (state.errorRes != null && snapshot != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.weather_offline),
                    fontSize = 13.sp,
                    color = Kiosk.color.danger,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.WeatherContent(snapshot: WeatherSnapshot, celsius: Boolean, locale: Locale) {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)

    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "${current.temperature.roundToInt()}°",
            style = Home.temp,
            color = Kiosk.color.textPrimary,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 10.dp)) {
            Text(
                text = stringResource(weatherLabelRes(current.weatherCode)),
                style = Home.condition,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
            )
            if (today != null) {
                Text(
                    text = stringResource(
                        R.string.label_today_range,
                        "${today.max.roundToInt()}°",
                        "${today.min.roundToInt()}°",
                    ),
                    style = Home.hiLo,
                    color = Kiosk.color.textMuted,
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth()) {
        WeatherMetric(
            label = stringResource(R.string.label_feels),
            value = "${current.feelsLike.roundToInt()}$unit",
            modifier = Modifier.weight(1f),
        )
        WeatherMetric(
            label = stringResource(R.string.label_humidity),
            value = "${current.humidity}%",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth()) {
        WeatherMetric(
            label = stringResource(R.string.label_wind),
            value = String.format(locale, "%.1f m/s", current.windSpeed),
            modifier = Modifier.weight(1f),
        )
        WeatherMetric(
            label = stringResource(R.string.label_range),
            value = today?.let { "${(it.max - it.min).roundToInt()}°" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(12.dp))
    FarmAdvice(snapshot)

    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Kiosk.color.rule))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        snapshot.daily.drop(1).take(3).forEach { day ->
            MiniForecast(day, locale, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FarmAdvice(snapshot: WeatherSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Kiosk.color.accentSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(30.dp)
                .background(Kiosk.color.accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.label_farm_advice),
                style = Home.farmLabel,
                color = Kiosk.color.accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(farmAdviceRes(snapshot)),
                style = Home.adviceText,
                color = Kiosk.color.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = Home.metricKey,
            color = Kiosk.color.textMuted,
            maxLines = 1,
        )
        Text(
            text = value,
            style = Home.metricValue,
            color = Kiosk.color.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniForecast(day: DailyForecast, locale: Locale, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.date.format(DateTimeFormatter.ofPattern("E", locale)),
            style = Home.miniDay,
            color = Kiosk.color.textSecondary,
            maxLines = 1,
        )
        Text(
            text = stringResource(weatherLabelRes(day.weatherCode)),
            style = Home.miniCondition,
            color = Kiosk.color.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${day.max.roundToInt()}° / ${day.min.roundToInt()}°",
            style = Home.miniTemp,
            color = Kiosk.color.textPrimary,
            maxLines = 1,
        )
    }
}
