package com.borasarang.daon.data.event

import com.borasarang.daon.data.calendar.CalendarData
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 반복 일정을 화면에 보여 줄 기간만큼 펼친다.
 *
 * 저장은 원본 1건만 하고, 화면에는 회차별 사본을 넘긴다. 사본은 [CalendarEvent.id] 가 원본과
 * 같으므로 삭제하면 반복 전체가 지워진다(회차 하나만 지우는 기능은 v0.3 이후).
 *
 * 음력 일정은 번들 음력 데이터(2025~2040) 범위 안에서만 펼쳐진다. 범위 밖은 조용히 빠진다.
 */
object EventOccurrences {

    fun expand(
        event: CalendarEvent,
        calendar: CalendarData,
        from: LocalDate,
        to: LocalDate,
    ): List<CalendarEvent> {
        if (from > to) return emptyList()
        if (event.repeat == RepeatRule.NONE && !event.lunar) {
            return if (event.date in from..to) listOf(event) else emptyList()
        }
        val dates = when {
            event.lunar -> calendar
                .lunarDates(event.lunarMonth, event.lunarDay, event.lunarLeap)
                .filter { it >= event.date && it in from..to }
            event.repeat == RepeatRule.YEARLY -> yearlyDates(event.date, from, to)
            event.repeat == RepeatRule.MONTHLY -> monthlyDates(event.date, from, to)
            event.repeat == RepeatRule.WEEKLY -> weeklyDates(event.date, from, to)
            else -> emptyList()
        }
        return dates.map { event.copy(date = it) }
    }

    private fun weeklyDates(start: LocalDate, from: LocalDate, to: LocalDate): List<LocalDate> {
        val first = if (start >= from) {
            start
        } else {
            val days = ChronoUnit.DAYS.between(start, from)
            start.plusDays(((days + 6) / 7) * 7)
        }
        return buildList {
            var date = first
            while (date <= to) {
                add(date)
                date = date.plusDays(7)
            }
        }
    }

    private fun monthlyDates(start: LocalDate, from: LocalDate, to: LocalDate): List<LocalDate> {
        val result = ArrayList<LocalDate>()
        var month = YearMonth.from(if (start > from) start else from)
        val lastMonth = YearMonth.from(to)
        while (month <= lastMonth) {
            // 31일 반복은 짧은 달에서 말일로 당긴다.
            val day = minOf(start.dayOfMonth, month.lengthOfMonth())
            val date = month.atDay(day)
            if (date >= start && date in from..to) result.add(date)
            month = month.plusMonths(1)
        }
        return result
    }

    private fun yearlyDates(start: LocalDate, from: LocalDate, to: LocalDate): List<LocalDate> {
        val result = ArrayList<LocalDate>()
        var year = maxOf(start.year, from.year)
        while (year <= to.year) {
            // 2월 29일 반복은 평년에 2월 28일로 둔다.
            val day = minOf(start.dayOfMonth, YearMonth.of(year, start.monthValue).lengthOfMonth())
            val date = LocalDate.of(year, start.monthValue, day)
            if (date >= start && date in from..to) result.add(date)
            year++
        }
        return result
    }
}
