package com.borasarang.daon.data.news

import org.junit.Assert.assertEquals
import org.junit.Test

class NewsScheduleTest {

    @Test
    fun `success uses the configured interval with one hour as default`() {
        assertEquals(60, NewsSchedule.delayMinutes(failures = 0, intervalMinutes = 60))
        assertEquals(180, NewsSchedule.delayMinutes(failures = 0, intervalMinutes = 180))
        assertEquals(60, NewsSchedule.normalizedInterval(0))
        assertEquals(60, NewsSchedule.normalizedInterval(17))
        assertEquals(30, NewsSchedule.normalizedInterval(30))
    }

    @Test
    fun `failures back off five then fifteen then sixty minutes`() {
        assertEquals(5, NewsSchedule.delayMinutes(failures = 1, intervalMinutes = 60))
        assertEquals(15, NewsSchedule.delayMinutes(failures = 2, intervalMinutes = 60))
        assertEquals(60, NewsSchedule.delayMinutes(failures = 3, intervalMinutes = 60))
        assertEquals(60, NewsSchedule.delayMinutes(failures = 9, intervalMinutes = 60))
    }

    @Test
    fun `delay milliseconds matches minutes`() {
        assertEquals(5 * 60_000L, NewsSchedule.delayMs(failures = 1, intervalMinutes = 60))
        assertEquals(360 * 60_000L, NewsSchedule.delayMs(failures = 0, intervalMinutes = 360))
    }

    @Test
    fun `only the four documented intervals are offered`() {
        assertEquals(listOf(30, 60, 180, 360), NewsSchedule.INTERVAL_OPTIONS)
    }
}
