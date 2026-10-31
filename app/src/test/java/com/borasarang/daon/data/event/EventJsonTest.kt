package com.borasarang.daon.data.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EventJsonTest {

    @Test
    fun `round trip keeps local events sorted`() {
        val events = listOf(
            CalendarEvent(id = "b", date = LocalDate.of(2026, 11, 2), title = "퇴비 주기"),
            CalendarEvent(id = "a", date = LocalDate.of(2026, 11, 1), title = "밭 갈기"),
            CalendarEvent(id = "c", date = LocalDate.of(2026, 11, 1), title = "모판 준비", time = "09:30"),
        )

        val decoded = EventJson.decode(EventJson.encode(events))

        assertEquals(listOf("c", "a", "b"), decoded.map { it.id })
        assertEquals("09:30", decoded.first().time)
    }

    @Test
    fun `decode skips malformed entries and keeps the rest`() {
        val text = """
            [
              {"id":"ok","date":"2026-11-01","title":"  고추밭 점검  "},
              {"id":"","date":"2026-11-01","title":"no id"},
              {"id":"bad-date","date":"2026-13-40","title":"bad date"},
              {"id":"blank","date":"2026-11-01","title":"   "},
              {"id":"bad-time","date":"2026-11-02","title":"time ignored","time":"25:99"}
            ]
        """.trimIndent()

        val decoded = EventJson.decode(text)

        assertEquals(2, decoded.size)
        assertEquals("고추밭 점검", decoded.first().title)
        assertEquals(null, decoded.last().time)
    }

    @Test
    fun `decode tolerates empty and broken json`() {
        assertTrue(EventJson.decode(null).isEmpty())
        assertTrue(EventJson.decode("").isEmpty())
        assertTrue(EventJson.decode("not json").isEmpty())
        assertTrue(EventJson.decode("{}").isEmpty())
    }

    @Test
    fun `all day events sort after timed events on the same day`() {
        val day = LocalDate.of(2026, 11, 1)
        val events = listOf(
            CalendarEvent(id = "all-day", date = day, title = "종일"),
            CalendarEvent(id = "timed", date = day, title = "아침", time = "07:00"),
        )

        val decoded = EventJson.decode(EventJson.encode(events))

        assertEquals(listOf("timed", "all-day"), decoded.map { it.id })
    }
}
