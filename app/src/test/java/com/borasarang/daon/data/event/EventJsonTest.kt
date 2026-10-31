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

    @Test
    fun `round trip keeps repeat and lunar fields`() {
        val day = LocalDate.of(2026, 10, 31)
        val events = listOf(
            CalendarEvent(
                id = "weekly",
                date = day,
                title = "매주 밭 점검",
                repeat = RepeatRule.WEEKLY,
            ),
            CalendarEvent(
                id = "lunar",
                date = day,
                title = "제사",
                repeat = RepeatRule.YEARLY,
                lunar = true,
                lunarMonth = 9,
                lunarDay = 21,
            ),
        )

        val decoded = EventJson.decode(EventJson.encode(events))

        val weekly = decoded.first { it.id == "weekly" }
        assertEquals(RepeatRule.WEEKLY, weekly.repeat)
        assertEquals(false, weekly.lunar)

        val lunar = decoded.first { it.id == "lunar" }
        assertEquals(RepeatRule.YEARLY, lunar.repeat)
        assertEquals(true, lunar.lunar)
        assertEquals(9, lunar.lunarMonth)
        assertEquals(21, lunar.lunarDay)
    }

    @Test
    fun `legacy entries without repeat fields read as single solar events`() {
        val text = """[{"id":"old","date":"2026-11-01","title":"예전 일정"}]"""

        val decoded = EventJson.decode(text)

        assertEquals(1, decoded.size)
        assertEquals(RepeatRule.NONE, decoded.first().repeat)
        assertEquals(false, decoded.first().lunar)
    }

    @Test
    fun `lunar flag without a valid lunar date falls back to a solar event`() {
        val text = """
            [{"id":"bad","date":"2026-11-01","title":"깨진 음력","lunar":true,
              "lunarMonth":0,"lunarDay":40,"repeat":"WEEKLY"}]
        """.trimIndent()

        val decoded = EventJson.decode(text)

        assertEquals(1, decoded.size)
        assertEquals(false, decoded.first().lunar)
        assertEquals(RepeatRule.WEEKLY, decoded.first().repeat)
    }
}
