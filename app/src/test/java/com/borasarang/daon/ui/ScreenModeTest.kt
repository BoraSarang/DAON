package com.borasarang.daon.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenModeTest {

    @Test
    fun `rotation includes radio by default`() {
        assertEquals(ScreenMode.RADIO, ScreenMode.CALENDAR.next())
        assertEquals(ScreenMode.WEATHER, ScreenMode.RADIO.next())
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next())
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next())
    }

    @Test
    fun `rotation skips radio when disabled`() {
        assertEquals(ScreenMode.WEATHER, ScreenMode.CALENDAR.next(includeRadio = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeRadio = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeRadio = false))
    }

    @Test
    fun `radio screen returns home when radio leaves rotation`() {
        assertEquals(ScreenMode.CALENDAR, ScreenMode.RADIO.next(includeRadio = false))
    }

    @Test
    fun `dot order matches the rotation`() {
        assertEquals(
            listOf(
                ScreenMode.CALENDAR,
                ScreenMode.RADIO,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = true),
        )
        assertEquals(
            listOf(ScreenMode.CALENDAR, ScreenMode.WEATHER, ScreenMode.CLOCK),
            ScreenMode.order(includeRadio = false),
        )
    }
}
