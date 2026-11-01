package com.borasarang.daon.data.event

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 로컬 일정을 SharedPreferences 에 넣기 위한 JSON 변환.
 * 일정 수가 적어(수십 건) Room 없이 JSON 한 덩어리로 저장한다.
 * 깨진 항목은 조용히 건너뛰고 나머지를 살린다.
 *
 * v0.2 이전에 저장한 항목에는 반복·음력 필드가 없으므로, 없으면 기본값(반복 없음·양력)으로 읽는다.
 */
object EventJson {

    /** 날짜 → 시간(종일은 맨 뒤) → 제목 순. */
    val ORDER: Comparator<CalendarEvent> = compareBy(
        { it.date },
        { it.time ?: "99:99" },
        { it.title },
    )

    fun encode(events: List<CalendarEvent>): String {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, event.id)
                    put(KEY_DATE, event.date.toString())
                    put(KEY_TITLE, event.title)
                    if (event.time != null) put(KEY_TIME, event.time)
                    if (event.repeat != RepeatRule.NONE) put(KEY_REPEAT, event.repeat.name)
                    if (event.lunar) {
                        put(KEY_LUNAR, true)
                        put(KEY_LUNAR_MONTH, event.lunarMonth)
                        put(KEY_LUNAR_DAY, event.lunarDay)
                        if (event.lunarLeap) put(KEY_LUNAR_LEAP, true)
                    }
                    if (event.skipDates.isNotEmpty()) {
                        put(
                            KEY_SKIP,
                            JSONArray(event.skipDates.sorted().map { it.toString() }),
                        )
                    }
                },
            )
        }
        return array.toString()
    }

    fun decode(text: String?): List<CalendarEvent> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(KEY_ID).takeIf { it.isNotBlank() } ?: continue
                    val date = runCatching { LocalDate.parse(item.optString(KEY_DATE)) }.getOrNull()
                        ?: continue
                    val title = item.optString(KEY_TITLE).trim()
                    if (title.isEmpty()) continue
                    val time = item.optString(KEY_TIME).takeIf { TIME_PATTERN.matches(it) }
                    val lunar = item.optBoolean(KEY_LUNAR, false)
                    val lunarMonth = item.optInt(KEY_LUNAR_MONTH, 0)
                    val lunarDay = item.optInt(KEY_LUNAR_DAY, 0)
                    val validLunar = lunar && lunarMonth in 1..12 && lunarDay in 1..30
                    add(
                        CalendarEvent(
                            id = id,
                            date = date,
                            title = title,
                            time = time,
                            repeat = if (validLunar) {
                                RepeatRule.YEARLY
                            } else {
                                RepeatRule.fromName(item.optString(KEY_REPEAT))
                            },
                            lunar = validLunar,
                            lunarMonth = if (validLunar) lunarMonth else 0,
                            lunarDay = if (validLunar) lunarDay else 0,
                            lunarLeap = validLunar && item.optBoolean(KEY_LUNAR_LEAP, false),
                            skipDates = decodeSkipDates(item.optJSONArray(KEY_SKIP)),
                        ),
                    )
                }
            }.sortedWith(ORDER)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private const val KEY_ID = "id"
    private const val KEY_DATE = "date"
    private const val KEY_TITLE = "title"
    private const val KEY_TIME = "time"
    private const val KEY_REPEAT = "repeat"
    private const val KEY_LUNAR = "lunar"
    private const val KEY_LUNAR_MONTH = "lunarMonth"
    private const val KEY_LUNAR_DAY = "lunarDay"
    private const val KEY_LUNAR_LEAP = "lunarLeap"
    private const val KEY_SKIP = "skipDates"

    /** 건너뛴 날짜 배열. 깨진 값은 버리고 읽을 수 있는 것만 남긴다. */
    private fun decodeSkipDates(array: JSONArray?): Set<LocalDate> {
        if (array == null || array.length() == 0) return emptySet()
        val dates = HashSet<LocalDate>(array.length())
        for (index in 0 until array.length()) {
            val text = array.optString(index)
            runCatching { LocalDate.parse(text) }.getOrNull()?.let { dates.add(it) }
        }
        return dates
    }

    private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
}
