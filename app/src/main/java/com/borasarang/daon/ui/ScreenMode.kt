package com.borasarang.daon.ui

import androidx.annotation.StringRes
import com.borasarang.daon.R

/** 자동 전환 순서: 달력 → 날씨 → 시계. */
enum class ScreenMode(@StringRes val labelRes: Int) {
    CALENDAR(R.string.screen_calendar),
    WEATHER(R.string.screen_weather),
    CLOCK(R.string.screen_clock),
    ;

    fun next(): ScreenMode = entries[(ordinal + 1) % entries.size]
}
