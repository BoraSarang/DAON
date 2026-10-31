package com.borasarang.daon.ui

import androidx.annotation.StringRes
import com.borasarang.daon.R
import com.borasarang.daon.data.WeatherSnapshot

/** WMO 날씨 코드 → 문자열 리소스. Open-Meteo 문서 기준. */
@StringRes
fun weatherLabelRes(code: Int): Int = when (code) {
    0 -> R.string.wmo_clear
    1 -> R.string.wmo_mainly_clear
    2 -> R.string.wmo_partly_cloudy
    3 -> R.string.wmo_overcast
    45, 48 -> R.string.wmo_fog
    51, 53, 55 -> R.string.wmo_drizzle
    56, 57 -> R.string.wmo_freezing_drizzle
    61, 63, 65 -> R.string.wmo_rain
    66, 67 -> R.string.wmo_freezing_rain
    71, 73, 75 -> R.string.wmo_snow
    77 -> R.string.wmo_snow_grains
    80, 81, 82 -> R.string.wmo_rain_showers
    85, 86 -> R.string.wmo_snow_showers
    95 -> R.string.wmo_thunderstorm
    96, 99 -> R.string.wmo_thunderstorm_hail
    else -> R.string.wmo_unknown
}

private val snowCodes = setOf(71, 73, 75, 77, 85, 86)
private val rainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)

/**
 * 오늘 날씨에서 농사에 바로 쓸 수 있는 한 줄을 고른다.
 * 눈 > 비 > 바람 > 추위 > 더위 > 일교차 > 건조 > 맑음 순으로 우선한다.
 */
@StringRes
fun farmAdviceRes(snapshot: WeatherSnapshot): Int {
    val current = snapshot.current
    val today = snapshot.daily.firstOrNull()
    val code = current.weatherCode
    val max = today?.max ?: current.temperature
    val min = today?.min ?: current.temperature
    val range = max - min

    return when {
        code in snowCodes -> R.string.advice_snow
        code in rainCodes -> R.string.advice_rain
        current.windSpeed >= 5.0 -> R.string.advice_wind
        min <= -5.0 -> R.string.advice_cold
        max >= 33.0 -> R.string.advice_hot
        range >= 10.0 -> R.string.advice_range
        current.humidity <= 35 -> R.string.advice_dry
        else -> R.string.advice_clear
    }
}
