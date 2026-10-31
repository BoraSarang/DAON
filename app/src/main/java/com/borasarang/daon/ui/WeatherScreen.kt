package com.borasarang.daon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.data.City
import com.borasarang.daon.data.DailyForecast
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** 날씨 화면 전용 큰 숫자. 달력 옆 카드보다 훨씬 크게 보여준다. */
private val HeroTemp = TextStyle(
    fontSize = 168.sp,
    lineHeight = 168.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-8).sp,
    fontFeatureSettings = "tnum",
)

private val HeroCondition = TextStyle(
    fontSize = 40.sp,
    lineHeight = 46.sp,
    fontWeight = FontWeight.SemiBold,
)

private val ForecastDay = TextStyle(
    fontSize = 24.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold,
)

private val ForecastCondition = TextStyle(
    fontSize = 19.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Medium,
)

private val ForecastTemp = TextStyle(
    fontSize = 30.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

/**
 * 날씨 메인 화면. T-017에서는 현재·오늘·4일 예보·농사 참고를 보여주고,
 * T-019에서 시간별·7일·비/눈 효과를 더한다.
 */
@Composable
fun WeatherScreen(
    city: City,
    state: WeatherUiState,
    celsius: Boolean,
    locale: Locale,
    touchStop: Boolean,
    stopped: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val cityName = stringResource(city.labelRes)
    Column(
        Modifier
            .fillMaxSize()
            .background(Kiosk.color.bg)
            .padding(Home.screenPadding),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_weather),
            subtitle = "$cityName · ${stringResource(R.string.label_now)}",
            current = ScreenMode.WEATHER,
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(Home.panelGap))

        val snapshot = state.snapshot
        when {
            snapshot != null -> {
                WeatherHero(snapshot, celsius, locale, onRefresh, Modifier.fillMaxWidth().weight(0.62f))
                Spacer(Modifier.height(Home.panelGap))
                ForecastPanel(snapshot, celsius, locale, Modifier.fillMaxWidth().weight(0.38f))
            }
            state.isLoading -> WeatherMessage(stringResource(R.string.weather_loading))
            else -> WeatherMessage(stringResource(state.errorRes ?: R.string.error_weather_network), error = true)
        }

        Spacer(Modifier.height(10.dp))
        if (!stopped) {
            RotationHint(
                stringResource(if (touchStop) R.string.screen_touch_stop else R.string.screen_touch_home),
            )
        }
    }
}

@Composable
private fun WeatherHero(
    snapshot: WeatherSnapshot,
    celsius: Boolean,
    locale: Locale,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)

    Surface(
        modifier = modifier,
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.weather_updated,
                        snapshot.fetchedAt.format(DateTimeFormatter.ofPattern("HH:mm", locale)),
                    ),
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                )
                Spacer(Modifier.weight(1f))
                RefreshButton(onRefresh)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${current.temperature.roundToInt()}°",
                    style = HeroTemp,
                    color = Kiosk.color.textPrimary,
                )
                Spacer(Modifier.width(34.dp))
                Column {
                    Text(
                        text = stringResource(weatherLabelRes(current.weatherCode)),
                        style = HeroCondition,
                        color = Kiosk.color.textPrimary,
                    )
                    if (today != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.label_today_range,
                                "${today.max.roundToInt()}°",
                                "${today.min.roundToInt()}°",
                            ),
                            style = Home.hiLo,
                            color = Kiosk.color.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.label_range) + " " +
                                "${(today.max - today.min).roundToInt()}°",
                            style = Home.hiLo,
                            color = Kiosk.color.textMuted,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
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
                WeatherMetric(
                    label = stringResource(R.string.label_wind),
                    value = String.format(locale, "%.1f m/s", current.windSpeed),
                    modifier = Modifier.weight(1f),
                )
                WeatherMetric(
                    label = stringResource(R.string.label_farm_advice),
                    value = stringResource(farmAdviceRes(snapshot)),
                    modifier = Modifier.weight(1.6f),
                    valueLines = 2,
                    highlight = true,
                )
            }
        }
    }
}

@Composable
private fun ForecastPanel(
    snapshot: WeatherSnapshot,
    celsius: Boolean,
    locale: Locale,
    modifier: Modifier,
) {
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)
    Surface(
        modifier = modifier,
        color = Kiosk.color.surface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.weather_forecast_title),
                style = Home.clockLabel,
                color = Kiosk.color.textMuted,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                snapshot.daily.forEachIndexed { index, day ->
                    DayColumn(
                        day = day,
                        label = if (index == 0) {
                            stringResource(R.string.today)
                        } else {
                            day.date.format(DateTimeFormatter.ofPattern("E", locale))
                        },
                        unit = unit,
                        locale = locale,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: DailyForecast,
    label: String,
    unit: String,
    locale: Locale,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = ForecastDay, color = Kiosk.color.textSecondary, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(
            text = day.date.format(DateTimeFormatter.ofPattern("M/d", locale)),
            style = Home.miniDay,
            color = Kiosk.color.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(weatherLabelRes(day.weatherCode)),
            style = ForecastCondition,
            color = Kiosk.color.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${day.max.roundToInt()}° / ${day.min.roundToInt()}$unit",
            style = ForecastTemp,
            color = Kiosk.color.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Kiosk.color.surfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.label_refresh),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Kiosk.color.accent,
        )
    }
}

@Composable
private fun ColumnScope.WeatherMessage(text: String, error: Boolean = false) {
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = Home.condition,
            color = if (error) Kiosk.color.danger else Kiosk.color.textMuted,
        )
    }
}

/** 날씨 카드와 날씨 화면이 함께 쓰는 지표 한 칸. */
@Composable
internal fun WeatherMetric(
    label: String,
    value: String,
    modifier: Modifier,
    valueLines: Int = 1,
    highlight: Boolean = false,
) {
    Column(modifier) {
        Text(
            text = label,
            style = Home.metricKey,
            color = if (highlight) Kiosk.color.accent else Kiosk.color.textMuted,
            maxLines = 1,
        )
        Text(
            text = value,
            style = Home.metricValue,
            color = Kiosk.color.textPrimary,
            maxLines = valueLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 농사 참고 한 줄. 달력 옆 날씨 카드에서도 쓴다. */
@Composable
internal fun ColumnScope.FarmAdviceBox(snapshot: WeatherSnapshot) {
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
