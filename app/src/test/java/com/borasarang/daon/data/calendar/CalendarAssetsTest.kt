package com.borasarang.daon.data.calendar

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class CalendarAssetsTest {

    private val assetDir: File = listOf(
        File("src/main/assets/calendar"),
        File("app/src/main/assets/calendar"),
    ).firstOrNull { it.isDirectory }
        ?: error("calendar assets not found. Run scripts/gen-calendar-data.py first.")

    private fun asset(name: String): JSONObject =
        JSONObject(File(assetDir, name).readText(charset = Charsets.UTF_8))

    private val data: CalendarData by lazy {
        CalendarData.parse { name ->
            File(assetDir, name).readText(charset = Charsets.UTF_8)
        }
    }

    private data class LunarDay(val year: Int, val month: Int, val day: Int, val leap: Boolean)

    @Test
    fun `lunar asset covers 2025 through 2040 without gaps`() {
        val json = asset("lunar_2025_2040.json")
        val from = LocalDate.parse(json.getJSONObject("range").getString("from"))
        val to = LocalDate.parse(json.getJSONObject("range").getString("to"))

        assertEquals(LocalDate.of(2025, 1, 1), from)
        assertEquals(LocalDate.of(2040, 12, 31), to)
        assertEquals(2, json.getInt("version"))
        assertEquals(5844, json.getInt("count"))
        assertEquals(5844, json.getString("days").split(';').size)

        var date = from
        var previous: LunarDay? = null
        while (date <= to) {
            val lunar = data.lunarOn(date)
            assertNotNull("missing lunar date: $date", lunar)
            val current = LunarDay(lunar!!.year, lunar.month, lunar.day, lunar.leap)

            assertTrue("month: $date", current.month in 1..12)
            assertTrue("day: $date", current.day in 1..30)
            assertTrue(
                "lunar year: $date -> ${current.year}",
                current.year == date.year || current.year == date.year - 1,
            )

            previous?.let { before ->
                if (current.day == 1) {
                    val expectedMonth = if (current.leap && !before.leap) {
                        before.month
                    } else {
                        before.month % 12 + 1
                    }
                    assertEquals("lunar month at $date", expectedMonth, current.month)
                    assertEquals(
                        "lunar year at $date",
                        if (expectedMonth == 1) before.year + 1 else before.year,
                        current.year,
                    )
                } else {
                    assertEquals("lunar day at $date", before.day + 1, current.day)
                    assertEquals("leap flag at $date", before.leap, current.leap)
                    assertEquals("lunar month at $date", before.month, current.month)
                    assertEquals("lunar year at $date", before.year, current.year)
                }
            }
            previous = current
            date = date.plusDays(1)
        }
    }

    @Test
    fun `lunar asset matches known Korean dates`() {
        val known = mapOf(
            LocalDate.of(2025, 1, 29) to LunarDay(2025, 1, 1, false),
            LocalDate.of(2025, 10, 6) to LunarDay(2025, 8, 15, false),
            LocalDate.of(2026, 2, 17) to LunarDay(2026, 1, 1, false),
            LocalDate.of(2026, 9, 25) to LunarDay(2026, 8, 15, false),
            LocalDate.of(2026, 10, 31) to LunarDay(2026, 9, 21, false),
            LocalDate.of(2027, 2, 7) to LunarDay(2027, 1, 1, false),
            LocalDate.of(2027, 9, 15) to LunarDay(2027, 8, 15, false),
        )
        known.forEach { (date, expected) ->
            val lunar = data.lunarOn(date)!!
            assertEquals("$date", expected, LunarDay(lunar.year, lunar.month, lunar.day, lunar.leap))
        }

        var date = LocalDate.of(2025, 1, 1)
        val leapDays = mutableListOf<LunarDay>()
        while (date.year <= 2040) {
            val lunar = data.lunarOn(date)!!
            if (lunar.leap) leapDays.add(LunarDay(lunar.year, lunar.month, lunar.day, true))
            date = date.plusDays(1)
        }
        assertTrue("no leap month found", leapDays.isNotEmpty())
        assertTrue("2025 leap month 6 missing", leapDays.any { it.year == 2025 && it.month == 6 })
    }

    @Test
    fun `solar term asset has 24 terms in order for every year`() {
        val days = asset("solar_terms_2025_2040.json").getJSONObject("days")
        assertEquals(384, days.length())

        val byYear = mutableMapOf<Int, MutableList<Pair<LocalDate, JSONObject>>>()
        for (key in days.keys()) {
            val entries = days.getJSONArray(key)
            assertEquals("one term per day: $key", 1, entries.length())
            val entry = entries.getJSONObject(0)
            val date = LocalDate.parse(key)
            byYear.getOrPut(date.year) { mutableListOf() }.add(date to entry)
        }

        assertEquals(16, byYear.size)
        for (year in 2025..2040) {
            val terms = byYear.getValue(year).sortedBy { it.first }
            assertEquals("$year term count", 24, terms.size)
            assertEquals("$year term order", (0..23).toList(), terms.map { it.second.getInt("index") })
            assertEquals(
                "$year term dates",
                terms.map { it.first }.distinct(),
                terms.map { it.first },
            )
        }
    }

    @Test
    fun `2026 solar terms match the KASI almanac`() {
        val days = asset("solar_terms_2025_2040.json").getJSONObject("days")
        val expected = mapOf(
            "소한" to "2026-01-05 17:23",
            "대한" to "2026-01-20 10:44",
            "입춘" to "2026-02-04 05:02",
            "우수" to "2026-02-19 00:51",
            "경칩" to "2026-03-05 22:59",
            "춘분" to "2026-03-20 23:45",
            "청명" to "2026-04-05 03:40",
            "곡우" to "2026-04-20 10:39",
            "입하" to "2026-05-05 20:48",
            "소만" to "2026-05-21 09:36",
            "망종" to "2026-06-06 00:48",
            "하지" to "2026-06-21 17:24",
            "소서" to "2026-07-07 10:56",
            "대서" to "2026-07-23 04:13",
            "입추" to "2026-08-07 20:42",
            "처서" to "2026-08-23 11:18",
            "백로" to "2026-09-07 23:41",
            "추분" to "2026-09-23 09:05",
            "한로" to "2026-10-08 15:29",
            "상강" to "2026-10-23 18:37",
            "입동" to "2026-11-07 18:52",
            "소설" to "2026-11-22 16:23",
            "대설" to "2026-12-07 11:52",
            "동지" to "2026-12-22 05:50",
        )

        val actual = mutableMapOf<String, String>()
        for (key in days.keys()) {
            if (!key.startsWith("2026-")) continue
            val entry = days.getJSONArray(key).getJSONObject(0)
            actual[entry.getString("name")] = "$key ${entry.getString("time")}"
            assertEquals("2026 verified flag: $key", "KASI", entry.getString("verified"))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `term details cover all 24 terms in Korean and English`() {
        val solarDays = asset("solar_terms_2025_2040.json").getJSONObject("days")
        val names = (0..23).associateWith { index ->
            solarDays.getJSONArray("2026-${monthDayForTerm(index)}").getJSONObject(0)
        }

        val ko = asset("term_details_ko.json")
        val en = asset("term_details_en.json")
        assertEquals("ko", ko.getString("locale"))
        assertEquals("en", en.getString("locale"))
        val koTerms = ko.getJSONObject("terms")
        val enTerms = en.getJSONObject("terms")
        assertEquals(24, koTerms.length())
        assertEquals(koTerms.keys().asSequence().toSet(), enTerms.keys().asSequence().toSet())

        for (index in 0..23) {
            val key = index.toString()
            val koTerm = koTerms.getJSONObject(key)
            val enTerm = enTerms.getJSONObject(key)
            val solar = names.getValue(index)
            assertEquals("hanja $key", solar.getString("hanja"), koTerm.getString("hanja"))
            assertEquals("hanja $key", solar.getString("hanja"), enTerm.getString("hanja"))
            assertEquals("ko name $key", solar.getString("name"), koTerm.getString("name"))
            assertEquals("en name $key", solar.getString("nameEn"), enTerm.getString("name"))

            assertTrue("ko description $key", koTerm.getString("description").length > 20)
            assertTrue("en description $key", enTerm.getString("description").length > 20)
            assertTrue("ko summary $key", koTerm.getString("summary").isNotBlank())
            assertTrue("en summary $key", enTerm.getString("summary").isNotBlank())
            assertEquals(
                "task count $key",
                koTerm.getJSONArray("tasks").length(),
                enTerm.getJSONArray("tasks").length(),
            )
            assertTrue("tasks $key", koTerm.getJSONArray("tasks").length() >= 3)
        }
    }

    // 2026년 절기 날짜는 KASI 값으로 고정되어 있다.
    private fun monthDayForTerm(index: Int): String = listOf(
        "01-05", "01-20", "02-04", "02-19", "03-05", "03-20",
        "04-05", "04-20", "05-05", "05-21", "06-06", "06-21",
        "07-07", "07-23", "08-07", "08-23", "09-07", "09-23",
        "10-08", "10-23", "11-07", "11-22", "12-07", "12-22",
    )[index]

    @Test
    fun `holiday asset covers official 2026 and 2027 dates`() {
        val json = asset("holidays_2026_2027.json")
        val days = json.getJSONObject("days")
        assertEquals(46, days.length())

        val required = setOf(
            "2026-01-01", "2026-02-17", "2026-03-01", "2026-03-02",
            "2026-05-01", "2026-05-05", "2026-06-03", "2026-06-06",
            "2026-07-17", "2026-08-15", "2026-08-17", "2026-09-25",
            "2026-10-03", "2026-10-05", "2026-10-09", "2026-12-25",
            "2027-02-07", "2027-02-09", "2027-03-01", "2027-05-03",
            "2027-05-05", "2027-05-13", "2027-06-06", "2027-07-19",
            "2027-08-16", "2027-09-15", "2027-10-04", "2027-10-11",
            "2027-12-27",
        )

        for (key in days.keys()) {
            val year = key.substring(0, 4).toInt()
            assertTrue("holiday range: $key", year == 2026 || year == 2027)
            val entries = days.getJSONArray(key)
            assertTrue("empty holiday: $key", entries.length() > 0)
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                listOf("name", "nameEn", "type", "description", "descriptionEn").forEach { field ->
                    assertNotNull("$key missing $field", entry.optString(field).takeIf { it.isNotBlank() })
                }
                if (entry.getString("type") == "substitute") {
                    assertTrue("$key substituteFor", entry.getString("substituteFor").isNotBlank())
                }
            }
        }
        assertTrue(required.all { days.has(it) })
        assertEquals("대체공휴일", days.getJSONArray("2026-10-05").getJSONObject(0).getString("name"))
    }
}
