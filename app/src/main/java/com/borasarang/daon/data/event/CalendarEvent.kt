package com.borasarang.daon.data.event

import java.time.LocalDate

/** 일정 출처. 로컬 일정은 이 태블릿에서 직접 추가하고, 기기 일정은 읽기 전용이다. */
enum class EventSource { LOCAL, DEVICE }

/**
 * 달력에 표시하는 일정 한 건.
 * [time] 은 "HH:mm" 이며 종일 일정이면 null 이다.
 */
data class CalendarEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val time: String? = null,
    val source: EventSource = EventSource.LOCAL,
    val calendarName: String? = null,
)
