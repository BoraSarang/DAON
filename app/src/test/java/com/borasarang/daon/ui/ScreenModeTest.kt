package com.borasarang.daon.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenModeTest {

    @Test
    fun `rotation includes radio and tv by default`() {
        assertEquals(ScreenMode.RADIO, ScreenMode.CALENDAR.next())
        assertEquals(ScreenMode.TV, ScreenMode.RADIO.next())
        assertEquals(ScreenMode.WEATHER, ScreenMode.TV.next())
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next())
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next())
    }

    @Test
    fun `rotation skips tv when disabled`() {
        assertEquals(ScreenMode.RADIO, ScreenMode.CALENDAR.next(includeTv = false))
        assertEquals(ScreenMode.WEATHER, ScreenMode.RADIO.next(includeTv = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeTv = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeTv = false))
    }

    @Test
    fun `rotation skips radio when disabled`() {
        assertEquals(ScreenMode.TV, ScreenMode.CALENDAR.next(includeRadio = false))
        assertEquals(ScreenMode.WEATHER, ScreenMode.TV.next(includeRadio = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeRadio = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeRadio = false))
    }

    @Test
    fun `rotation goes straight to weather when radio and tv are off`() {
        assertEquals(
            ScreenMode.WEATHER,
            ScreenMode.CALENDAR.next(includeRadio = false, includeTv = false),
        )
        assertEquals(
            ScreenMode.WEATHER,
            ScreenMode.RADIO.next(includeRadio = false, includeTv = false),
        )
        assertEquals(
            ScreenMode.WEATHER,
            ScreenMode.TV.next(includeRadio = false, includeTv = false),
        )
    }

    @Test
    fun `dot order matches the rotation`() {
        assertEquals(
            listOf(
                ScreenMode.CALENDAR,
                ScreenMode.RADIO,
                ScreenMode.TV,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = true, includeTv = true),
        )
        assertEquals(
            listOf(ScreenMode.CALENDAR, ScreenMode.TV, ScreenMode.WEATHER, ScreenMode.CLOCK),
            ScreenMode.order(includeRadio = false, includeTv = true),
        )
        assertEquals(
            listOf(ScreenMode.CALENDAR, ScreenMode.RADIO, ScreenMode.WEATHER, ScreenMode.CLOCK),
            ScreenMode.order(includeRadio = true, includeTv = false),
        )
        assertEquals(
            listOf(ScreenMode.CALENDAR, ScreenMode.WEATHER, ScreenMode.CLOCK),
            ScreenMode.order(includeRadio = false, includeTv = false),
        )
    }
}
