package com.borasarang.daon.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.borasarang.daon.R
import com.borasarang.daon.core.monthCells
import com.borasarang.daon.data.City
import com.borasarang.daon.data.DailyForecast
import com.borasarang.daon.ui.theme.Kiosk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(vm: DashboardViewModel, onOpenSettings: () -> Unit) {
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val city by vm.city.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()

    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Kiosk.screenPadding),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_tagline).uppercase(locale),
                style = MaterialTheme.typography.labelMedium,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = stringResource(R.string.label_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Kiosk.color.accent,
                )
            }
        }

        Spacer(Modifier.height(Kiosk.lg))

        Row(Modifier.fillMaxWidth().weight(1f)) {
            ClockPanel(
                now = now,
                hour24 = vm.hour24,
                locale = locale,
                modifier = Modifier.weight(0.34f).fillMaxHeight(),
            )
            PanelDivider()
            CalendarPanel(
                today = now.toLocalDate(),
                locale = locale,
                modifier = Modifier.weight(0.40f).fillMaxHeight(),
            )
            PanelDivider()
            WeatherPanel(
                city = city,
                state = weather,
                celsius = vm.celsius,
                locale = locale,
                onRefresh = { vm.refresh() },
                modifier = Modifier.weight(0.26f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ClockPanel(now: LocalDateTime, hour24: Boolean, locale: Locale, modifier: Modifier) {
    val dateText = now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    val timePattern = if (hour24) "HH:mm" else "h:mm"
    val timeText = now.format(DateTimeFormatter.ofPattern(timePattern, locale))
    val amPm = now.format(DateTimeFormatter.ofPattern("a", locale))
    val seconds = now.second.toString().padStart(2, '0')

    Column(modifier) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.headlineLarge,
            color = Kiosk.color.textSecondary,
        )
        Spacer(Modifier.height(Kiosk.xl))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val clockSize = (maxWidth.value / 3.2f).coerceIn(56f, 132f).sp
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    fontSize = clockSize,
                    lineHeight = clockSize,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                    color = Kiosk.color.textPrimary,
                )
                Spacer(Modifier.width(Kiosk.md))
                Column(Modifier.padding(bottom = Kiosk.sm)) {
                    Text(
                        text = seconds,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Kiosk.color.textMuted,
                    )
                    if (!hour24) {
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Kiosk.color.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(Kiosk.hairline).background(Kiosk.color.rule))
        Spacer(Modifier.height(Kiosk.md))
        SectionLabel(stringResource(R.string.label_agenda))
        Spacer(Modifier.height(Kiosk.sm))
        Text(
            text = stringResource(R.string.no_events),
            style = MaterialTheme.typography.bodyLarge,
            color = Kiosk.color.textMuted,
        )
    }
}

@Composable
private fun CalendarPanel(today: LocalDate, locale: Locale, modifier: Modifier) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val weekStart = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val cells = remember(month, weekStart) { monthCells(month, weekStart) }
    val pattern = stringResource(R.string.month_pattern)
    val title = remember(month, pattern, locale) {
        month.atDay(1).format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    val weekdays = remember(weekStart, locale) {
        (0 until 7).map { offset -> weekStart.plus(offset.toLong()) }
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                SectionLabel(stringResource(R.string.label_calendar))
                Spacer(Modifier.height(Kiosk.xs))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Kiosk.color.textPrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { month = month.minusMonths(1) }) {
                Text("‹", fontSize = 28.sp, color = Kiosk.color.textSecondary)
            }
            TextButton(onClick = { month = month.plusMonths(1) }) {
                Text("›", fontSize = 28.sp, color = Kiosk.color.textSecondary)
            }
        }

        Spacer(Modifier.height(Kiosk.lg))

        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = Kiosk.color.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(Kiosk.sm))

        Column(Modifier.fillMaxWidth().weight(1f)) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            isToday = date == today,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate?, isToday: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier.padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (date == null) return@Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (isToday) Kiosk.color.accentSoft else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(Kiosk.radiusSm),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) Kiosk.color.accent else Kiosk.color.textSecondary,
            )
        }
    }
}

@Composable
private fun WeatherPanel(
    city: City,
    state: WeatherUiState,
    celsius: Boolean,
    locale: Locale,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    val snapshot = state.snapshot
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)

    Column(modifier) {
        SectionLabel(stringResource(R.string.label_weather))
        Spacer(Modifier.height(Kiosk.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(city.labelRes),
                style = MaterialTheme.typography.headlineLarge,
                color = Kiosk.color.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRefresh) {
                Text(
                    text = stringResource(R.string.label_refresh),
                    style = MaterialTheme.typography.labelSmall,
                    color = Kiosk.color.accent,
                )
            }
        }
        snapshot?.let {
            val updated = DateTimeFormatter.ofPattern("HH:mm", locale).format(it.fetchedAt)
            Text(
                text = stringResource(R.string.updated_at, updated),
                style = MaterialTheme.typography.labelSmall,
                color = Kiosk.color.textMuted,
            )
        }

        Spacer(Modifier.height(Kiosk.lg))

        when {
            snapshot != null -> CurrentWeatherBlock(snapshot, unit, locale)
            state.isLoading -> Text(
                text = stringResource(R.string.weather_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = Kiosk.color.textMuted,
            )
            else -> Text(
                text = stringResource(state.errorRes ?: R.string.error_weather_network),
                style = MaterialTheme.typography.bodyLarge,
                color = Kiosk.color.danger,
            )
        }

        if (state.errorRes != null && snapshot != null) {
            Spacer(Modifier.height(Kiosk.sm))
            Text(
                text = stringResource(R.string.weather_offline),
                style = MaterialTheme.typography.labelSmall,
                color = Kiosk.color.danger,
            )
        }

        Spacer(Modifier.weight(1f))
        if (snapshot != null) {
            Box(Modifier.fillMaxWidth().height(Kiosk.hairline).background(Kiosk.color.rule))
            Spacer(Modifier.height(Kiosk.md))
            SectionLabel(stringResource(R.string.label_forecast))
            Spacer(Modifier.height(Kiosk.sm))
            snapshot.daily.drop(1).take(3).forEach { day ->
                ForecastRow(day, unit, locale)
            }
        }
    }
}

@Composable
private fun CurrentWeatherBlock(snapshot: com.borasarang.daon.data.WeatherSnapshot, unit: String, locale: Locale) {
    val current = snapshot.current
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = current.temperature.roundToInt().toString(),
            style = MaterialTheme.typography.displayMedium,
            color = Kiosk.color.textPrimary,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.headlineSmall,
            color = Kiosk.color.textSecondary,
            modifier = Modifier.padding(bottom = 18.dp, start = 4.dp),
        )
    }
    Text(
        text = stringResource(weatherLabelRes(current.weatherCode)),
        style = MaterialTheme.typography.bodyLarge,
        color = Kiosk.color.textSecondary,
    )
    Spacer(Modifier.height(Kiosk.md))
    Text(
        text = stringResource(R.string.feels_like, "${current.feelsLike.roundToInt()}$unit"),
        style = MaterialTheme.typography.bodyMedium,
        color = Kiosk.color.textMuted,
    )
    Text(
        text = stringResource(R.string.humidity, current.humidity),
        style = MaterialTheme.typography.bodyMedium,
        color = Kiosk.color.textMuted,
    )
    Text(
        text = stringResource(R.string.wind, String.format(locale, "%.1f", current.windSpeed)),
        style = MaterialTheme.typography.bodyMedium,
        color = Kiosk.color.textMuted,
    )
}

@Composable
private fun ForecastRow(day: DailyForecast, unit: String, locale: Locale) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = day.date.format(DateTimeFormatter.ofPattern("E d", locale)),
            modifier = Modifier.width(84.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Kiosk.color.textSecondary,
        )
        Text(
            text = stringResource(weatherLabelRes(day.weatherCode)),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Kiosk.color.textMuted,
        )
        Text(
            text = "${day.max.roundToInt()}° / ${day.min.roundToInt()}$unit",
            style = MaterialTheme.typography.bodyMedium,
            color = Kiosk.color.textPrimary,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium,
        color = Kiosk.color.textMuted,
    )
}

@Composable
private fun PanelDivider() {
    Box(
        Modifier
            .padding(horizontal = Kiosk.panelGap / 2)
            .fillMaxHeight()
            .width(Kiosk.hairline)
            .background(Kiosk.color.rule),
    )
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPanel(vm: DashboardViewModel, onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    val city by vm.city.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        // 스크림 — 바깥을 누르면 닫힌다. 다이얼로그 창을 쓰지 않아 시스템 바가 다시 나타나지 않는다.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(680.dp),
            color = Kiosk.color.surface,
            shape = RoundedCornerShape(topStart = Kiosk.radiusLg, bottomStart = Kiosk.radiusLg),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Kiosk.xxl),
                verticalArrangement = Arrangement.spacedBy(Kiosk.lg),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_settings),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Kiosk.color.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.label_close), color = Kiosk.color.textSecondary)
                    }
                }

                SettingsSection(stringResource(R.string.label_city)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        City.entries.forEach { item ->
                            SettingChip(
                                selected = city == item,
                                label = stringResource(item.labelRes),
                                onClick = { vm.setCity(item) },
                            )
                        }
                    }
                }

                SettingsSection(stringResource(R.string.label_hour_format)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        SettingChip(
                            selected = vm.hour24,
                            label = stringResource(R.string.hour_24),
                            onClick = { vm.updateHour24(true) },
                        )
                        SettingChip(
                            selected = !vm.hour24,
                            label = stringResource(R.string.hour_12),
                            onClick = { vm.updateHour24(false) },
                        )
                    }
                }

                SettingsSection(stringResource(R.string.label_units)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        SettingChip(
                            selected = vm.celsius,
                            label = stringResource(R.string.unit_celsius),
                            onClick = { vm.updateCelsius(true) },
                        )
                        SettingChip(
                            selected = !vm.celsius,
                            label = stringResource(R.string.unit_fahrenheit),
                            onClick = { vm.updateCelsius(false) },
                        )
                    }
                }

                SettingsSection(stringResource(R.string.label_language)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
                        listOf("system", "en", "ko").forEach { tag ->
                            SettingChip(
                                selected = vm.localeTag == tag,
                                label = stringResource(com.borasarang.daon.core.LocaleController.labelRes(tag)),
                                onClick = {
                                    vm.updateLocaleTag(tag)
                                    com.borasarang.daon.core.LocaleController.set(tag, activity)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Kiosk.color.surfaceAlt,
            labelColor = Kiosk.color.textSecondary,
            selectedContainerColor = Kiosk.color.accentSoft,
            selectedLabelColor = Kiosk.color.accent,
        ),
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Kiosk.sm)) {
        SectionLabel(title)
        content()
    }
}
