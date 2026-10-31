package com.borasarang.daon.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class CalendarDataTest {

    private lateinit var data: CalendarData

    @Before
    fun setUp() {
        val dir = listOf(
            File("src/main/assets/calendar"),
            File("app/src/main/assets/calendar"),
        ).firstOrNull { it.isDirectory }
            ?: error("calendar assets not found. Run scripts/gen-calendar-data.py first.")

        data = CalendarData.parse { name ->
            File(dir, name).readText(charset = Charsets.UTF_8)
        }
    }

    @Test
    fun `parses all bundled assets`() {
        assertTrue(data.isLoaded)
    }

    @Test
    fun `finds lunar date and known holidays`() {
        val lunar = data.lunarOn(LocalDate.of(2026, 10, 31))
        assertEquals(LunarDate(2026, 9, 21, false), lunar)

        val substitute = data.holidaysOn(LocalDate.of(2026, 10, 5))
        assertEquals(1, substitute.size)
        assertEquals("대체공휴일", substitute.first().name)
        assertEquals("substitute", substitute.first().type)
        assertEquals("개천절", substitute.first().substituteFor)

        assertEquals("한글날", data.holidaysOn(LocalDate.of(2026, 10, 9)).first().name)
        assertTrue(data.holidaysOn(LocalDate.of(2026, 10, 31)).isEmpty())
    }

    @Test
    fun `active term is the latest term on or before the date`() {
        val (date, term) = data.activeTerm(LocalDate.of(2026, 10, 31))!!
        assertEquals(LocalDate.of(2026, 10, 23), date)
        assertEquals("상강", term.name)
        assertEquals(19, term.index)

        assertEquals("한로", data.activeTerm(LocalDate.of(2026, 10, 8))!!.second.name)
        assertEquals("한로", data.activeTerm(LocalDate.of(2026, 10, 22))!!.second.name)
        assertNull(data.activeTerm(LocalDate.of(2025, 1, 1)))
        assertEquals("소한", data.activeTerm(LocalDate.of(2025, 1, 6))!!.second.name)
    }

    @Test
    fun `lists terms and holidays in a month`() {
        val terms = data.termsIn(YearMonth.of(2026, 10))
        assertEquals(listOf("한로", "상강"), terms.map { it.second.name })

        val holidays = data.holidaysIn(YearMonth.of(2026, 10))
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 9),
            ),
            holidays.map { it.first },
        )
    }

    @Test
    fun `term details are localized with farming tasks`() {
        val ko = data.termDetail(19, korean = true)
        val en = data.termDetail(19, korean = false)
        assertNotNull(ko)
        assertNotNull(en)
        assertEquals("상강", ko!!.name)
        assertEquals("Sanggang", en!!.name)
        assertEquals("가을", ko.season)
        assertEquals(3, ko.tasks.size)
        assertEquals(3, en.tasks.size)
        assertTrue(ko.description.length > 20)
        assertTrue(en.description.length > 20)
    }

    @Test
    fun `returns null outside the bundled lunar range`() {
        assertNull(data.lunarOn(LocalDate.of(2024, 12, 31)))
        assertNull(data.lunarOn(LocalDate.of(2041, 1, 1)))
    }
}
