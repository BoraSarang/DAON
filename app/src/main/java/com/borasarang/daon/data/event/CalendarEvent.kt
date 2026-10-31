package com.borasarang.daon.data.event

import java.time.LocalDate

/** 일정 출처. 로컬 일정은 이 태블릿에서 직접 추가하고, 기기 일정은 읽기 전용이다. */
enum class EventSource { LOCAL, DEVICE }

/** 반복 규칙. 음력 일정은 달력 정렬 때문에 [YEARLY] 만 쓴다. */
enum class RepeatRule {
    NONE,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    companion object {
        fun fromName(name: String?): RepeatRule =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

/**
 * 달력에 표시하는 일정 한 건.
 *
 * [date] 는 양력 기준 시작일이다. [lunar] 가 true 면 [lunarMonth]·[lunarDay]·[lunarLeap] 로
 * 매년 같은 음력 날짜에 다시 나타난다(예: 제사, 생일).
 * [time] 은 "HH:mm" 이며 종일 일정이면 null 이다.
 */
data class CalendarEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val time: String? = null,
    val source: EventSource = EventSource.LOCAL,
    val calendarName: String? = null,
    val repeat: RepeatRule = RepeatRule.NONE,
    val lunar: Boolean = false,
    val lunarMonth: Int = 0,
    val lunarDay: Int = 0,
    val lunarLeap: Boolean = false,
) {
    /** 반복 일정인지. */
    val isRepeating: Boolean get() = repeat != RepeatRule.NONE || lunar
}
