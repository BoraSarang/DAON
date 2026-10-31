package com.borasarang.daon.ui

import androidx.annotation.StringRes
import com.borasarang.daon.R

/** 자동 전환 순서: 달력 → 라디오 → TV → 날씨 → 시계. 라디오·TV는 설정에서 뺄 수 있다. */
enum class ScreenMode(@StringRes val labelRes: Int) {
    CALENDAR(R.string.screen_calendar),
    RADIO(R.string.screen_radio),
    TV(R.string.screen_tv),
    WEATHER(R.string.screen_weather),
    CLOCK(R.string.screen_clock),
    ;

    /**
     * 다음 화면. [includeRadio]·[includeTv] 가 false 면 그 화면을 건너뛴다.
     */
    fun next(
        includeRadio: Boolean = true,
        includeTv: Boolean = true,
    ): ScreenMode = when (this) {
        CALENDAR -> when {
            includeRadio -> RADIO
            includeTv -> TV
            else -> WEATHER
        }
        RADIO -> if (includeTv) TV else WEATHER
        TV -> WEATHER
        WEATHER -> CLOCK
        CLOCK -> CALENDAR
    }

    companion object {
        /** 화면 점 순서. 설정에서 뺀 화면은 점도 숨긴다. */
        fun order(includeRadio: Boolean, includeTv: Boolean): List<ScreenMode> =
            entries.filter { mode ->
                (mode != RADIO || includeRadio) && (mode != TV || includeTv)
            }
    }
}
