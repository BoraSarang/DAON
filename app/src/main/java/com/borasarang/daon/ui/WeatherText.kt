package com.borasarang.daon.ui

import androidx.annotation.StringRes
import com.borasarang.daon.R

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
