package com.borasarang.daon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {

    @Test
    fun `always produces 42 cells`() {
        for (month in 0..11) {
            val cells = monthCells(YearMonth.of(2026, month + 1), DayOfWeek.SUNDAY)
            assertEquals(CELL_COUNT, cells.size)
        }
    }

    @Test
    fun `october 2026 starts on the fifth column when week starts on sunday`() {
        val cells = monthCells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(4, cells.indexOfFirst { it != null })
        assertEquals(LocalDate.of(2026, 10, 1), cells[4])
    }

    @Test
    fun `contains every day of the month exactly once in order`() {
        val month = YearMonth.of(2026, 2)
        val days = monthCells(month, DayOfWeek.MONDAY).filterNotNull()
        assertEquals(month.lengthOfMonth(), days.size)
        assertEquals(month.atDay(1), days.first())
        assertEquals(month.atEndOfMonth(), days.last())
        assertTrue(days.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `leading cells are empty`() {
        val cells = monthCells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(listOf(null, null, null, null), cells.take(4))
        assertNull(cells.last())
    }

    @Test
    fun `month grid fills adjacent months and stays contiguous`() {
        val days = monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(CELL_COUNT, days.size)
        assertEquals(LocalDate.of(2026, 9, 27), days.first())
        assertEquals(LocalDate.of(2026, 11, 7), days.last())
        assertTrue(days.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
        assertEquals(4, days.indexOf(LocalDate.of(2026, 10, 1)))
    }
}
