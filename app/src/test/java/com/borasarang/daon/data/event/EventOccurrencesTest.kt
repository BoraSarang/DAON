package com.borasarang.daon.data.event

import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.LunarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

class EventOccurrencesTest {

    private lateinit var calendar: CalendarData

    @Before
    fun setUp() {
        val dir = listOf(
            File("src/main/assets/calendar"),
            File("app/src/main/assets/calendar"),
        ).firstOrNull { it.isDirectory }
            ?: error("calendar assets not found. Run scripts/gen-calendar-data.py first.")

        calendar = CalendarData.parse { name ->
            File(dir, name).readText(charset = Charsets.UTF_8)
        }
    }

    private fun event(
        date: LocalDate,
        repeat: RepeatRule = RepeatRule.NONE,
        lunar: Boolean = false,
        lunarMonth: Int = 0,
        lunarDay: Int = 0,
    ) = CalendarEvent(
        id = "e",
        date = date,
        title = "밭 갈기",
        repeat = repeat,
        lunar = lunar,
        lunarMonth = lunarMonth,
        lunarDay = lunarDay,
    )

    @Test
    fun `single event appears only inside the window`() {
        val day = LocalDate.of(2026, 11, 2)
        val event = event(day)

        assertEquals(1, EventOccurrences.expand(event, calendar, day, day).size)
        assertTrue(
            EventOccurrences.expand(
                event,
                calendar,
                day.plusDays(1),
                day.plusDays(10),
            ).isEmpty(),
        )
    }

    @Test
    fun `weekly event repeats every seven days from its start`() {
        val event = event(LocalDate.of(2026, 10, 1), RepeatRule.WEEKLY)

        val dates = EventOccurrences.expand(
            event,
            calendar,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
        ).map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 19),
                LocalDate.of(2026, 11, 26),
            ),
            dates,
        )
    }

    @Test
    fun `monthly event clamps to the last day of short months`() {
        val event = event(LocalDate.of(2026, 1, 31), RepeatRule.MONTHLY)

        val dates = EventOccurrences.expand(
            event,
            calendar,
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 4, 30),
        ).map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            dates,
        )
    }

    @Test
    fun `yearly event keeps the month and day`() {
        val event = event(LocalDate.of(2026, 5, 8), RepeatRule.YEARLY)

        val dates = EventOccurrences.expand(
            event,
            calendar,
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2028, 12, 31),
        ).map { it.date }

        assertEquals(
            listOf(LocalDate.of(2027, 5, 8), LocalDate.of(2028, 5, 8)),
            dates,
        )
    }

    @Test
    fun `lunar event repeats on the same lunar day each year`() {
        // 2026-10-31 은 음력 9월 21일이다(CalendarDataTest 와 같은 값).
        val start = LocalDate.of(2026, 10, 31)
        val lunar = calendar.lunarOn(start)!!
        val event = event(
            date = start,
            repeat = RepeatRule.YEARLY,
            lunar = true,
            lunarMonth = lunar.month,
            lunarDay = lunar.day,
        )

        val dates = EventOccurrences.expand(
            event,
            calendar,
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 12, 31),
        ).map { it.date }

        assertEquals(1, dates.size)
        assertEquals(
            LunarDate(2027, 9, 21, false),
            calendar.lunarOn(dates.single()),
        )
    }

    @Test
    fun `occurrences keep the series id so deleting removes the series`() {
        val event = event(LocalDate.of(2026, 10, 1), RepeatRule.WEEKLY)

        val occurrences = EventOccurrences.expand(
            event,
            calendar,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
        )

        assertTrue(occurrences.isNotEmpty())
        assertTrue(occurrences.all { it.id == event.id })
        assertTrue(occurrences.all { it.repeat == RepeatRule.WEEKLY })
    }
}
