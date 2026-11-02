package com.borasarang.daon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borasarang.daon.R
import com.borasarang.daon.core.TouchAction
import com.borasarang.daon.data.Place
import com.borasarang.daon.data.DailyForecast
import com.borasarang.daon.data.HourlyForecast
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.ui.theme.Home
import com.borasarang.daon.ui.theme.Kiosk
import java.time.LocalDate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val HeroTemp = TextStyle(
    fontSize = 134.sp,
    lineHeight = 140.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-7).sp,
    fontFeatureSettings = "tnum",
)

private val HeroCondition = TextStyle(
    fontSize = 34.sp,
    lineHeight = 40.sp,
    fontWeight = FontWeight.SemiBold,
)

private val WeekDay = TextStyle(
    fontSize = 18.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold,
)

private val WeekTemp = TextStyle(
    fontSize = 20.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

private val HourTime = TextStyle(
    fontSize = 18.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

private val HourTemp = TextStyle(
    fontSize = 24.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

private val SmallNote = TextStyle(
    fontSize = 13.sp,
    lineHeight = 17.sp,
    fontWeight = FontWeight.Medium,
)

/**
 * 하늘 장면이 비치도록 카드를 살짝 투명하게 둔다.
 * 최악(밝은 지평선 위)에서도 본문 대비 13:1 이상이라 70대 가독성 기준을 지킨다.
 */
private val CardSurface: Color
    @Composable get() = Kiosk.color.surface.copy(alpha = 0.88f)

/**
 * 날씨 메인. 현재·오늘 요약 + 이번 주 7일 + 농사 참고 + 3시간 간격 8칸.
 * 배경에는 비·눈·천둥 효과를 그린다(설정에서 끌 수 있음).
 */
@Composable
fun WeatherScreen(
    place: Place,
    state: WeatherUiState,
    celsius: Boolean,
    locale: Locale,
    weatherMotion: Boolean,
    touchAction: TouchAction,
    stopped: Boolean,
    includeRadio: Boolean = true,
    includeTv: Boolean = true,
    includeNews: Boolean = true,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onRefresh: () -> Unit,
) {
    val snapshot = state.snapshot
    val cityName = place.label()
    val korean = locale.language == "ko"
    val subtitle = "$cityName · " + stringResource(
        if (state.isOffline) R.string.weather_offline else R.string.label_now,
    )

    Box(Modifier.fillMaxSize().background(Kiosk.color.bg)) {
        WeatherScene(
            code = snapshot?.current?.weatherCode,
            isDay = snapshot?.current?.isDay ?: true,
            motion = weatherMotion,
            light = Kiosk.isLight,
            modifier = Modifier.fillMaxSize(),
        )
        WeatherEffect(
            code = snapshot?.current?.weatherCode,
            motion = weatherMotion,
            modifier = Modifier.fillMaxSize(),
        )

        Column(Modifier.fillMaxSize().padding(Home.screenPadding)) {
            ScreenHeader(
                title = stringResource(R.string.screen_weather),
                subtitle = subtitle,
                current = ScreenMode.WEATHER,
                onOpenSettings = onOpenSettings,
                onGoHome = onGoHome,
                includeRadio = includeRadio,
                includeTv = includeTv,
                includeNews = includeNews,
            )
            Spacer(Modifier.height(Home.panelGap))

            when {
                snapshot != null -> {
                    Row(Modifier.fillMaxWidth().weight(0.60f)) {
                        WeatherHero(
                            snapshot = snapshot,
                            celsius = celsius,
                            locale = locale,
                            offline = state.isOffline,
                            onRefresh = onRefresh,
                            modifier = Modifier.weight(0.64f).fillMaxHeight(),
                        )
                        Spacer(Modifier.width(Home.panelGap))
                        WeekPanel(
                            snapshot = snapshot,
                            celsius = celsius,
                            locale = locale,
                            modifier = Modifier.weight(0.36f).fillMaxHeight(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AdvisoryBanner(snapshot, locale)
                    Spacer(Modifier.height(12.dp))
                    HourlyPanel(
                        snapshot = snapshot,
                        celsius = celsius,
                        locale = locale,
                        korean = korean,
                        modifier = Modifier.fillMaxWidth().weight(0.40f),
                    )
                }
                state.isLoading -> WeatherMessage(stringResource(R.string.weather_loading))
                else -> WeatherMessage(
                    stringResource(state.errorRes ?: R.string.error_weather_network),
                    error = true,
                )
            }

            Spacer(Modifier.height(10.dp))
            if (!stopped) {
                RotationHint(stringResource(rotationHintRes(touchAction, false)))
            }
        }
    }
}

@Composable
private fun WeatherHero(
    snapshot: WeatherSnapshot,
    celsius: Boolean,
    locale: Locale,
    offline: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)

    Surface(
        modifier = modifier,
        color = CardSurface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.weather_updated,
                        snapshot.fetchedAt.format(Formatters.of("HH:mm", locale)),
                    ),
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                )
                if (offline) {
                    Spacer(Modifier.width(10.dp))
                    OfflineBadge()
                }
                Spacer(Modifier.weight(1f))
                RefreshButton(onRefresh)
            }
            Spacer(Modifier.height(6.dp))

            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                WeatherGlyph(
                    code = current.weatherCode,
                    isDay = current.isDay,
                    modifier = Modifier.size(84.dp),
                )
                Spacer(Modifier.width(22.dp))
                Text(
                    text = "${current.temperature.roundToInt()}°",
                    style = HeroTemp,
                    color = Kiosk.color.textPrimary,
                )
                Spacer(Modifier.width(24.dp))
                Column {
                    Text(
                        text = stringResource(weatherLabelRes(current.weatherCode)),
                        style = HeroCondition,
                        color = Kiosk.color.textPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.feels_like,
                            "${current.feelsLike.roundToInt()}$unit",
                        ),
                        style = Home.hiLo,
                        color = Kiosk.color.textSecondary,
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                WeatherMetric(
                    label = stringResource(R.string.label_precip_prob),
                    value = "${today?.precipitationProbability ?: 0}%",
                    modifier = Modifier.weight(1f),
                    highlight = (today?.precipitationProbability ?: 0) >= 60,
                )
                WeatherMetric(
                    label = stringResource(R.string.label_precip_amount),
                    value = formatMm(today?.precipitationSum ?: 0.0, locale),
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
            }
        }
    }
}

@Composable
private fun WeekPanel(
    snapshot: WeatherSnapshot,
    celsius: Boolean,
    locale: Locale,
    modifier: Modifier,
) {
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)
    Surface(
        modifier = modifier,
        color = CardSurface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weather_week_title),
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.weather_week_note),
                    style = SmallNote,
                    color = Kiosk.color.textMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
            snapshot.daily.take(7).forEach { day ->
                WeekRow(day = day, unit = unit, locale = locale, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeekRow(day: DailyForecast, unit: String, locale: Locale, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dayLabel(day.date, locale),
            style = WeekDay,
            color = if (day.date == LocalDate.now()) Kiosk.color.accent else Kiosk.color.textPrimary,
            maxLines = 1,
            modifier = Modifier.width(52.dp),
        )
        WeatherGlyph(code = day.weatherCode, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (day.precipitationProbability > 0) "${day.precipitationProbability}%" else "",
            style = SmallNote,
            color = Kiosk.color.rain,
            maxLines = 1,
            modifier = Modifier.width(42.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${day.max.roundToInt()}°",
            style = WeekTemp,
            color = Kiosk.color.textPrimary,
        )
        Text(
            text = " / ${day.min.roundToInt()}$unit",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Kiosk.color.textMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun AdvisoryBanner(snapshot: WeatherSnapshot, locale: Locale) {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val wind = today?.windMax?.takeIf { it > 0.0 } ?: current.windSpeed
    val detail = stringResource(
        R.string.weather_advisory_detail,
        today?.precipitationProbability ?: 0,
        formatMm(today?.precipitationSum ?: 0.0, locale),
        String.format(locale, "%.1f", wind),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .background(Kiosk.color.accentSoft)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(40.dp)
                .background(Kiosk.color.accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(farmAdviceRes(snapshot)),
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Kiosk.color.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Kiosk.color.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HourlyPanel(
    snapshot: WeatherSnapshot,
    celsius: Boolean,
    locale: Locale,
    korean: Boolean,
    modifier: Modifier,
) {
    val unit = stringResource(if (celsius) R.string.unit_celsius else R.string.unit_fahrenheit)
    Surface(
        modifier = modifier,
        color = CardSurface,
        shape = RoundedCornerShape(Home.panelRadiusLarge),
        border = BorderStroke(1.dp, Kiosk.color.rule),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weather_hourly_title),
                    style = Home.clockLabel,
                    color = Kiosk.color.textMuted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.weather_hourly_note),
                    style = SmallNote,
                    color = Kiosk.color.textMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (snapshot.hourly.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("—", style = Home.condition, color = Kiosk.color.textMuted)
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    snapshot.hourly.forEach { hour ->
                        HourColumn(
                            hour = hour,
                            unit = unit,
                            locale = locale,
                            korean = korean,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourColumn(
    hour: HourlyForecast,
    unit: String,
    locale: Locale,
    korean: Boolean,
    modifier: Modifier,
) {
    val timeText = if (korean) {
        "${hour.time.hour}시"
    } else {
        hour.time.format(Formatters.of("HH:mm", locale))
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(timeText, style = HourTime, color = Kiosk.color.textSecondary, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        WeatherGlyph(code = hour.weatherCode, isDay = hour.isDay, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            text = "${hour.temperature.roundToInt()}$unit",
            style = HourTemp,
            color = Kiosk.color.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatMm(hour.precipitation, locale),
            style = SmallNote,
            color = if (hour.precipitation > 0) Kiosk.color.rain else Kiosk.color.textMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        PrecipBar(hour.precipitation)
    }
}

@Composable
private fun PrecipBar(millimetres: Double) {
    val fraction = (millimetres / 5.0).coerceIn(0.0, 1.0).toFloat()
    Box(
        Modifier
            .width(52.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Kiosk.color.rule),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Kiosk.color.rain, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun OfflineBadge() {
    Text(
        text = stringResource(R.string.weather_offline),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Kiosk.color.danger,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Kiosk.color.danger.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
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
            color = if (highlight) Kiosk.color.accent else Kiosk.color.textPrimary,
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

private fun dayLabel(date: LocalDate, locale: Locale): String =
    date.format(Formatters.of("E", locale))

private fun formatMm(value: Double, locale: Locale): String {
    val text = if (value >= 10.0) {
        String.format(locale, "%.0f", value)
    } else {
        String.format(locale, "%.1f", value)
    }
    return "$text mm"
}

// ---------- 날씨 그림 ----------

private enum class GlyphKind { SUN, PARTLY, CLOUD, FOG, RAIN, SNOW, THUNDER }

private fun glyphKind(code: Int): GlyphKind = when (code) {
    0 -> GlyphKind.SUN
    1, 2 -> GlyphKind.PARTLY
    3 -> GlyphKind.CLOUD
    45, 48 -> GlyphKind.FOG
    71, 73, 75, 77, 85, 86 -> GlyphKind.SNOW
    95, 96, 99 -> GlyphKind.THUNDER
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> GlyphKind.RAIN
    else -> GlyphKind.CLOUD
}

/** WMO 코드를 작은 Canvas 그림으로 그린다. 아이콘 폰트 없이도 모든 기기에서 같게 보인다. */
@Composable
private fun WeatherGlyph(code: Int, isDay: Boolean = true, modifier: Modifier) {
    val kind = glyphKind(code)
    // 라이트 카드 위에서는 밝은 달이 보이지 않아 글자색으로 그린다.
    val moonInk = if (Kiosk.isLight) Kiosk.color.textSecondary else MoonInk
    val cardSurface = Kiosk.color.surface
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = size.minDimension
        when (kind) {
            GlyphKind.SUN -> if (isDay) {
                drawSun(Offset(w / 2f, h / 2f), s * 0.20f)
            } else {
                drawMoon(Offset(w / 2f, h / 2f), s * 0.26f, moonInk, cardSurface)
            }
            GlyphKind.PARTLY -> {
                if (isDay) {
                    drawSun(Offset(w * 0.36f, h * 0.34f), s * 0.15f)
                } else {
                    drawMoon(Offset(w * 0.36f, h * 0.34f), s * 0.19f, moonInk, cardSurface)
                }
                drawCloud(Offset(w * 0.54f, h * 0.56f), s * 0.18f, Kiosk.color.textSecondary)
            }
            GlyphKind.CLOUD -> drawCloud(Offset(w / 2f, h * 0.44f), s * 0.21f, Kiosk.color.textSecondary)
            GlyphKind.FOG -> drawFog(Offset(w / 2f, h / 2f), s * 0.34f)
            GlyphKind.RAIN -> {
                drawCloud(Offset(w / 2f, h * 0.40f), s * 0.20f, Kiosk.color.textSecondary)
                drawRain(Offset(w / 2f, h * 0.64f), s * 0.20f)
            }
            GlyphKind.SNOW -> {
                drawCloud(Offset(w / 2f, h * 0.40f), s * 0.20f, Kiosk.color.textSecondary)
                drawSnow(Offset(w / 2f, h * 0.68f), s * 0.16f)
            }
            GlyphKind.THUNDER -> {
                drawCloud(Offset(w / 2f, h * 0.38f), s * 0.20f, Kiosk.color.textSecondary)
                drawBolt(Offset(w / 2f, h * 0.58f), s * 0.20f)
            }
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    drawCircle(Kiosk.color.accent, radius, center)
    val inner = radius * 1.45f
    val outer = radius * 2.0f
    repeat(8) { index ->
        val angle = index * PI / 4.0
        drawLine(
            color = Kiosk.color.accent,
            start = Offset(
                center.x + (cos(angle) * inner).toFloat(),
                center.y + (sin(angle) * inner).toFloat(),
            ),
            end = Offset(
                center.x + (cos(angle) * outer).toFloat(),
                center.y + (sin(angle) * outer).toFloat(),
            ),
            strokeWidth = radius * 0.22f,
            cap = StrokeCap.Round,
        )
    }
}

/** 블랙 카드 위 달 색. 라이트 테마는 글자색으로 대신한다. */
private val MoonInk = Color(0xFFE8E2D0)

private fun DrawScope.drawMoon(center: Offset, radius: Float, moon: Color, background: Color) {
    drawCircle(moon, radius, center)
    drawCircle(
        color = background,
        radius = radius * 0.86f,
        center = Offset(center.x + radius * 0.44f, center.y - radius * 0.30f),
    )
    repeat(3) { index ->
        val angle = index * PI / 1.5 + 0.6
        drawCircle(
            color = moon.copy(alpha = 0.55f),
            radius = radius * 0.09f,
            center = Offset(
                center.x + (cos(angle) * radius * 1.65f).toFloat(),
                center.y + (sin(angle) * radius * 1.65f).toFloat(),
            ),
        )
    }
}

private fun DrawScope.drawCloud(center: Offset, radius: Float, color: Color) {
    val cx = center.x
    val cy = center.y
    drawCircle(color, radius * 0.85f, Offset(cx - radius * 0.85f, cy))
    drawCircle(color, radius * 1.15f, Offset(cx, cy - radius * 0.55f))
    drawCircle(color, radius * 0.80f, Offset(cx + radius * 0.90f, cy))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - radius * 1.55f, cy),
        size = Size(radius * 3.1f, radius * 1.05f),
        cornerRadius = CornerRadius(radius * 0.52f),
    )
}

private fun DrawScope.drawRain(center: Offset, radius: Float) {
    repeat(3) { index ->
        val x = center.x + (index - 1) * radius * 0.9f
        drawLine(
            color = Kiosk.color.rain,
            start = Offset(x, center.y),
            end = Offset(x - radius * 0.22f, center.y + radius * 0.9f),
            strokeWidth = radius * 0.18f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(center: Offset, radius: Float) {
    val flake = if (Kiosk.isLight) Kiosk.color.textSecondary else Color.White
    repeat(3) { index ->
        drawCircle(
            color = flake.copy(alpha = 0.85f),
            radius = radius * 0.22f,
            center = Offset(center.x + (index - 1) * radius * 0.9f, center.y),
        )
    }
}

private fun DrawScope.drawBolt(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x + radius * 0.25f, center.y - radius * 0.55f)
        lineTo(center.x - radius * 0.15f, center.y + radius * 0.10f)
        lineTo(center.x + radius * 0.10f, center.y + radius * 0.10f)
        lineTo(center.x - radius * 0.30f, center.y + radius * 0.95f)
        lineTo(center.x + radius * 0.35f, center.y + radius * 0.05f)
        lineTo(center.x + radius * 0.08f, center.y + radius * 0.05f)
        close()
    }
    drawPath(path, Kiosk.color.accent)
}

private fun DrawScope.drawFog(center: Offset, radius: Float) {
    repeat(3) { index ->
        val y = center.y + (index - 1) * radius * 0.55f
        val half = radius * (1f - index * 0.12f)
        drawLine(
            color = Kiosk.color.textSecondary,
            start = Offset(center.x - half, y),
            end = Offset(center.x + half, y),
            strokeWidth = radius * 0.16f,
            cap = StrokeCap.Round,
        )
    }
}
