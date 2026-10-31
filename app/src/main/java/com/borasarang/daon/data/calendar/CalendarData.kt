package com.borasarang.daon.data.calendar

import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** 음력 날짜. [leap]이 true이면 윤달이다. */
data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val leap: Boolean,
)

/** 24절기 중 하루. [index]는 소한(0)부터 동지(23)까지의 순서다. */
data class SolarTerm(
    val index: Int,
    val name: String,
    val nameEn: String,
    val hanja: String,
    val time: String,
    val verified: Boolean,
)

/** 공휴일·명절·선거일 하루. */
data class Holiday(
    val name: String,
    val nameEn: String,
    val type: String,
    val substituteFor: String?,
    val description: String,
    val descriptionEn: String,
)

/** 절기 설명·농사일. [locale]에 맞는 문안만 담는다. */
data class TermDetail(
    val index: Int,
    val name: String,
    val hanja: String,
    val season: String,
    val summary: String,
    val description: String,
    val tasks: List<String>,
)

fun SolarTerm.displayName(korean: Boolean): String = if (korean) name else nameEn

fun Holiday.displayName(korean: Boolean): String = if (korean) name else nameEn

fun Holiday.descriptionFor(korean: Boolean): String = if (korean) description else descriptionEn

/**
 * 앱에 번들한 달력 자산을 파싱한 결과. 날짜 조회는 모두 O(1) 또는 O(log n)이다.
 *
 * 자산 형식은 `docs/api/CALENDAR_DATA.md`를 참고한다.
 */
class CalendarData internal constructor(
    private val lunarStart: LocalDate,
    private val lunarValues: IntArray,
    private val lunarCount: Int,
    private val termsByDate: Map<LocalDate, SolarTerm>,
    private val holidaysByDate: Map<LocalDate, List<Holiday>>,
    private val detailsKo: Map<Int, TermDetail>,
    private val detailsEn: Map<Int, TermDetail>,
) {

    private val sortedTermDates: List<LocalDate> = termsByDate.keys.sorted()

    val isLoaded: Boolean get() = lunarCount > 0

    /**
     * 음력은 시작일로부터의 일수로 색인한다. 5,844일을 매번 LocalDate 키로 찾지 않아
     * 시작 시 파싱과 조회가 모두 빠르다.
     */
    fun lunarOn(date: LocalDate): LunarDate? {
        if (lunarCount == 0) return null
        val offset = ChronoUnit.DAYS.between(lunarStart, date)
        if (offset < 0 || offset >= lunarCount) return null
        val index = offset.toInt() * 4
        return LunarDate(
            year = lunarValues[index],
            month = lunarValues[index + 1],
            day = lunarValues[index + 2],
            leap = lunarValues[index + 3] == 1,
        )
    }

    /**
     * (음력 월, 일, 윤달) → 번들 범위 안의 양력 날짜 목록.
     * 음력 반복 일정(제사·생일)을 매년 펼칠 때 쓴다. 첫 조회 때 한 번만 만든다.
     */
    private val lunarByMonthDay: Map<Int, List<LocalDate>> by lazy {
        val map = HashMap<Int, MutableList<LocalDate>>(512)
        for (index in 0 until lunarCount) {
            val cursor = index * 4
            val key = lunarKey(
                month = lunarValues[cursor + 1],
                day = lunarValues[cursor + 2],
                leap = lunarValues[cursor + 3] == 1,
            )
            map.getOrPut(key) { ArrayList(2) }.add(lunarStart.plusDays(index.toLong()))
        }
        map
    }

    /** 같은 음력 날짜의 양력 날짜들(보통 1년에 1개, 윤달은 드물게). 범위 밖이면 빈 목록. */
    fun lunarDates(month: Int, day: Int, leap: Boolean = false): List<LocalDate> =
        lunarByMonthDay[lunarKey(month, day, leap)] ?: emptyList()

    fun termsOn(date: LocalDate): List<SolarTerm> =
        termsByDate[date]?.let { listOf(it) } ?: emptyList()

    fun holidaysOn(date: LocalDate): List<Holiday> = holidaysByDate[date] ?: emptyList()

    fun termDetail(index: Int, korean: Boolean): TermDetail? =
        (if (korean) detailsKo else detailsEn)[index]

    /** 해당 날짜에 이미 들어선 절기(가장 최근 절기). 2025-01-01 이전이면 null. */
    fun activeTerm(date: LocalDate): Pair<LocalDate, SolarTerm>? {
        if (sortedTermDates.isEmpty()) return null
        val found = sortedTermDates.binarySearch(date)
        val position = if (found >= 0) found else -found - 2
        if (position < 0) return null
        val key = sortedTermDates[position]
        return key to termsByDate.getValue(key)
    }

    /** 해당 월에 들어오는 절기. */
    fun termsIn(month: YearMonth): List<Pair<LocalDate, SolarTerm>> =
        sortedTermDates
            .filter { YearMonth.from(it) == month }
            .map { it to termsByDate.getValue(it) }

    /** 해당 월의 공휴일. */
    fun holidaysIn(month: YearMonth): List<Pair<LocalDate, List<Holiday>>> =
        holidaysByDate
            .filterKeys { YearMonth.from(it) == month }
            .toSortedMap()
            .map { it.key to it.value }

    private fun lunarKey(month: Int, day: Int, leap: Boolean): Int =
        month * 1_000 + day * 10 + if (leap) 1 else 0

    companion object {
        const val LUNAR_ASSET = "lunar_2025_2040.json"
        const val TERMS_ASSET = "solar_terms_2025_2040.json"
        const val HOLIDAYS_ASSET = "holidays_2026_2027.json"
        const val DETAILS_KO_ASSET = "term_details_ko.json"
        const val DETAILS_EN_ASSET = "term_details_en.json"

        fun empty(): CalendarData = CalendarData(
            lunarStart = LocalDate.MIN,
            lunarValues = IntArray(0),
            lunarCount = 0,
            termsByDate = emptyMap(),
            holidaysByDate = emptyMap(),
            detailsKo = emptyMap(),
            detailsEn = emptyMap(),
        )

        /**
         * 자산 5개를 [read]로 읽어 파싱한다. [read]는 파일 이름을 받아 JSON 문자열을 돌려준다.
         * 테스트에서는 파일 시스템에서, 앱에서는 AssetManager에서 읽는다.
         */
        fun parse(read: (String) -> String): CalendarData {
            val lunarRoot = JSONObject(read(LUNAR_ASSET))
            val lunarStart = LocalDate.parse(lunarRoot.getJSONObject("range").getString("from"))
            val lunarText = lunarRoot.getString("days")

            // "2026,9,21,0;2026,9,22,0;…" 를 할당 없이 직접 훑는다.
            // 5,844일을 split/toInt로 처리하면 Fire HD에서 300ms 넘게 걸린다.
            var lunarCount = 0
            for (character in lunarText) if (character == ';') lunarCount++
            if (lunarText.isNotEmpty()) lunarCount++
            val lunarValues = IntArray(lunarCount * 4)
            var cursor = 0
            var record = 0
            while (cursor < lunarText.length) {
                var field = 0
                var number = 0
                while (cursor < lunarText.length && lunarText[cursor] != ';') {
                    val character = lunarText[cursor]
                    if (character == ',') {
                        lunarValues[record * 4 + field] = number
                        field++
                        number = 0
                    } else {
                        number = number * 10 + (character - '0')
                    }
                    cursor++
                }
                lunarValues[record * 4 + field] = number
                record++
                cursor++
            }
            val expectedCount = lunarRoot.optInt("count", record)
            require(expectedCount == record) {
                "lunar count mismatch: $expectedCount != $record"
            }

            val termsRoot = JSONObject(read(TERMS_ASSET))
            val termsByDate = buildMap {
                val days = termsRoot.getJSONObject("days")
                for (key in days.keys()) {
                    val entries = days.getJSONArray(key)
                    if (entries.length() == 0) continue
                    val value = entries.getJSONObject(0)
                    put(
                        LocalDate.parse(key),
                        SolarTerm(
                            index = value.getInt("index"),
                            name = value.getString("name"),
                            nameEn = value.getString("nameEn"),
                            hanja = value.getString("hanja"),
                            time = value.getString("time"),
                            verified = value.optString("verified") == "KASI",
                        ),
                    )
                }
            }

            val holidaysRoot = JSONObject(read(HOLIDAYS_ASSET))
            val holidaysByDate = buildMap {
                val days = holidaysRoot.getJSONObject("days")
                for (key in days.keys()) {
                    val entries = days.getJSONArray(key)
                    val items = ArrayList<Holiday>(entries.length())
                    for (i in 0 until entries.length()) {
                        val value = entries.getJSONObject(i)
                        items.add(
                            Holiday(
                                name = value.getString("name"),
                                nameEn = value.getString("nameEn"),
                                type = value.getString("type"),
                                substituteFor = value.optString("substituteFor").takeIf { it.isNotBlank() },
                                description = value.getString("description"),
                                descriptionEn = value.getString("descriptionEn"),
                            ),
                        )
                    }
                    put(LocalDate.parse(key), items)
                }
            }

            return CalendarData(
                lunarStart = lunarStart,
                lunarValues = lunarValues,
                lunarCount = record,
                termsByDate = termsByDate,
                holidaysByDate = holidaysByDate,
                detailsKo = parseDetails(read(DETAILS_KO_ASSET)),
                detailsEn = parseDetails(read(DETAILS_EN_ASSET)),
            )
        }

        private fun parseDetails(json: String): Map<Int, TermDetail> {
            val root = JSONObject(json)
            val terms = root.getJSONObject("terms")
            return buildMap {
                for (key in terms.keys()) {
                    val value = terms.getJSONObject(key)
                    val tasks = value.getJSONArray("tasks")
                    put(
                        key.toInt(),
                        TermDetail(
                            index = key.toInt(),
                            name = value.getString("name"),
                            hanja = value.getString("hanja"),
                            season = value.getString("season"),
                            summary = value.getString("summary"),
                            description = value.getString("description"),
                            tasks = (0 until tasks.length()).map { tasks.getString(it) },
                        ),
                    )
                }
            }
        }
    }
}
