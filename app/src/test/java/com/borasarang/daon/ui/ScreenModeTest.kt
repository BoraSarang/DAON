package com.borasarang.daon.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenModeTest {

    @Test
    fun `rotation includes news radio and tv by default`() {
        assertEquals(ScreenMode.NEWS, ScreenMode.CALENDAR.next())
        assertEquals(ScreenMode.RADIO, ScreenMode.NEWS.next())
        assertEquals(ScreenMode.TV, ScreenMode.RADIO.next())
        assertEquals(ScreenMode.WEATHER, ScreenMode.TV.next())
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next())
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next())
    }

    @Test
    fun `rotation skips news when disabled`() {
        assertEquals(ScreenMode.RADIO, ScreenMode.CALENDAR.next(includeNews = false))
        assertEquals(ScreenMode.TV, ScreenMode.RADIO.next(includeNews = false))
        assertEquals(ScreenMode.WEATHER, ScreenMode.TV.next(includeNews = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeNews = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeNews = false))
    }

    @Test
    fun `rotation skips tv when disabled`() {
        assertEquals(ScreenMode.NEWS, ScreenMode.CALENDAR.next(includeTv = false))
        assertEquals(ScreenMode.RADIO, ScreenMode.NEWS.next(includeTv = false))
        assertEquals(ScreenMode.WEATHER, ScreenMode.RADIO.next(includeTv = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeTv = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeTv = false))
    }

    @Test
    fun `rotation skips radio when disabled`() {
        assertEquals(ScreenMode.NEWS, ScreenMode.CALENDAR.next(includeRadio = false))
        assertEquals(ScreenMode.TV, ScreenMode.NEWS.next(includeRadio = false))
        assertEquals(ScreenMode.WEATHER, ScreenMode.TV.next(includeRadio = false))
        assertEquals(ScreenMode.CLOCK, ScreenMode.WEATHER.next(includeRadio = false))
        assertEquals(ScreenMode.CALENDAR, ScreenMode.CLOCK.next(includeRadio = false))
    }

    @Test
    fun `rotation goes calendar to news to weather when radio and tv are off`() {
        assertEquals(
            ScreenMode.NEWS,
            ScreenMode.CALENDAR.next(includeRadio = false, includeTv = false),
        )
        assertEquals(
            ScreenMode.WEATHER,
            ScreenMode.NEWS.next(includeRadio = false, includeTv = false),
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
                ScreenMode.NEWS,
                ScreenMode.RADIO,
                ScreenMode.TV,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = true, includeTv = true),
        )
        assertEquals(
            listOf(
                ScreenMode.CALENDAR,
                ScreenMode.RADIO,
                ScreenMode.TV,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = true, includeTv = true, includeNews = false),
        )
        assertEquals(
            listOf(
                ScreenMode.CALENDAR,
                ScreenMode.NEWS,
                ScreenMode.TV,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = false, includeTv = true),
        )
        assertEquals(
            listOf(
                ScreenMode.CALENDAR,
                ScreenMode.NEWS,
                ScreenMode.RADIO,
                ScreenMode.WEATHER,
                ScreenMode.CLOCK,
            ),
            ScreenMode.order(includeRadio = true, includeTv = false),
        )
        assertEquals(
            listOf(ScreenMode.CALENDAR, ScreenMode.NEWS, ScreenMode.WEATHER, ScreenMode.CLOCK),
            ScreenMode.order(includeRadio = false, includeTv = false),
        )
    }
}
