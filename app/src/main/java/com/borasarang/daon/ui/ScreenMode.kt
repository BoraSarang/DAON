package com.borasarang.daon.ui

import androidx.annotation.StringRes
import com.borasarang.daon.R

/** 자동 전환 순서: 달력 → 라디오 → 날씨 → 시계. 라디오는 설정에서 뺄 수 있다. */
enum class ScreenMode(@StringRes val labelRes: Int) {
    CALENDAR(R.string.screen_calendar),
    RADIO(R.string.screen_radio),
    WEATHER(R.string.screen_weather),
    CLOCK(R.string.screen_clock),
    ;

    /**
     * 다음 화면. [includeRadio] 가 false 면 라디오를 건너뛴다.
     * 라디오 화면에서 라디오를 끈 경우에는 대시보드로 돌아간다.
     */
    fun next(includeRadio: Boolean = true): ScreenMode = when (this) {
        CALENDAR -> if (includeRadio) RADIO else WEATHER
        RADIO -> if (includeRadio) WEATHER else CALENDAR
        WEATHER -> CLOCK
        CLOCK -> CALENDAR
    }

    companion object {
        /** 화면 점 순서. [includeRadio] 가 false 면 3개만 보여 준다. */
        fun order(includeRadio: Boolean): List<ScreenMode> =
            if (includeRadio) entries else entries.filter { it != RADIO }
    }
}
