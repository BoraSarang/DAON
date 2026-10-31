package com.borasarang.daon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * v0.2 키오스크 홈. 달력 → 날씨 → 시계 3화면을 자동 전환한다.
 * 70대 사용자를 기준으로 큰 글씨와 큰 터치 영역을 쓴다.
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val city by vm.city.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()
    val korean = locale.language == "ko"

    var screen by rememberSaveable { mutableStateOf(ScreenMode.CALENDAR) }
    var detailDate by remember { mutableStateOf<LocalDate?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    /** 터치 동작이 `그 화면에서 멈추기`일 때 사용자가 직접 멈춘 상태. */
    var rotationStopped by remember { mutableStateOf(false) }

    val paused = settingsOpen || detailDate != null
    val dwell = when (screen) {
        ScreenMode.CALENDAR -> vm.dwellCalendar
        ScreenMode.WEATHER -> vm.dwellWeather
        ScreenMode.CLOCK -> vm.dwellClock
    }

    LaunchedEffect(screen, dwell, vm.rotationEnabled, paused, rotationStopped) {
        if (!vm.rotationEnabled || paused || rotationStopped) return@LaunchedEffect
        delay(dwell * 1_000L)
        DebugLogger.i("ROTATE", "auto ${screen.name} -> ${screen.next().name} after ${dwell}s")
        screen = screen.next()
    }

    BackHandler(enabled = detailDate != null || settingsOpen || screen != ScreenMode.CALENDAR) {
        when {
            detailDate != null -> detailDate = null
            settingsOpen -> settingsOpen = false
            else -> screen = ScreenMode.CALENDAR
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .pointerInput(vm.touchStop, screen) {
                detectTapGestures {
                    DebugLogger.i("ROTATE", "tap screen=${screen.name} touchStop=${vm.touchStop} stopped=$rotationStopped")
                    if (vm.touchStop) {
                        rotationStopped = !rotationStopped
                    } else if (screen != ScreenMode.CALENDAR) {
                        screen = ScreenMode.CALENDAR
                    }
                }
            },
    ) {
        Crossfade(targetState = screen, animationSpec = tween(250), label = "screen") { mode ->
            when (mode) {
                ScreenMode.CALENDAR -> CalendarHome(
                    now = now,
                    weather = weather,
                    city = city,
                    calendar = calendar,
                    korean = korean,
                    locale = locale,
                    hour24 = vm.hour24,
                    celsius = vm.celsius,
                    onDateClick = { detailDate = it },
                    onOpenSettings = { settingsOpen = true },
                    onRefreshWeather = { vm.refresh() },
                )
                ScreenMode.WEATHER -> WeatherScreen(
                    city = city,
                    state = weather,
                    celsius = vm.celsius,
                    locale = locale,
                    touchStop = vm.touchStop,
                    stopped = rotationStopped,
                    onOpenSettings = { settingsOpen = true },
                    onRefresh = { vm.refresh() },
                )
                ScreenMode.CLOCK -> ClockScreen(
                    now = now,
                    hour24 = vm.hour24,
                    korean = korean,
                    locale = locale,
                    calendar = calendar,
                    city = city,
                    weather = weather,
                    celsius = vm.celsius,
                    touchStop = vm.touchStop,
                    stopped = rotationStopped,
                    onOpenSettings = { settingsOpen = true },
                )
            }
        }

        if (rotationStopped) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                RotationHint(stringResource(R.string.rotation_paused))
            }
        }

        detailDate?.let { date ->
            DateDetailPanel(
                date = date,
                calendar = calendar,
                korean = korean,
                locale = locale,
                onClose = { detailDate = null },
            )
        }

        if (settingsOpen) {
            SettingsPanel(vm = vm, onDismiss = { settingsOpen = false })
        }
    }
}

/** 달력 홈. 달력 70% + 오른쪽 시계·날씨 30%. */
@Composable
private fun CalendarHome(
    now: LocalDateTime,
    weather: WeatherUiState,
    city: City,
    calendar: CalendarData,
    korean: Boolean,
    locale: Locale,
    hour24: Boolean,
    celsius: Boolean,
    onDateClick: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
) {
    val today = now.toLocalDate()
    Column(
        Modifier
            .fillMaxSize()
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
                onDateClick = onDateClick,
            )
            Spacer(Modifier.width(Home.panelGap))
            Column(
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Home.panelGap),
            ) {
                ClockCard(
                    now = now,
                    hour24 = hour24,
                    korean = korean,
                    locale = locale,
                    modifier = Modifier.fillMaxWidth().height(Home.clockCard),
                )
                WeatherCard(
                    city = city,
                    state = weather,
                    celsius = celsius,
                    locale = locale,
                    onRefresh = onRefreshWeather,
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

        ScreenDots(ScreenMode.CALENDAR)
        Spacer(Modifier.width(18.dp))
        SettingsButton(onClick = onOpenSettings)
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
    FarmAdviceBox(snapshot)

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
