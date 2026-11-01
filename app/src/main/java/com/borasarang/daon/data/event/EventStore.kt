package com.borasarang.daon.data.event

import android.content.Context
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import com.borasarang.daon.data.calendar.LunarDate
import java.time.LocalDate

/**
 * 이 태블릿에서 직접 추가한 일정 저장소.
 *
 * v0.2 는 일정 수가 적고 편집이 드물어 SharedPreferences + JSON 한 덩어리로 충분하다.
 * 반복 일정도 원본 1건만 저장하고, 화면에 그릴 때 [EventOccurrences] 가 펼친다.
 * 저장 실패는 E-AND-STORE-0001 로 남기고, 메모리 상태는 그대로 유지한다.
 */
class EventStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun all(): List<CalendarEvent> = EventJson.decode(prefs.getString(KEY_EVENTS, null))

    /**
     * 일정을 추가한다. [lunar] 가 있으면 그 음력 날짜로 매년 반복하고,
     * [repeat] 은 양력 반복 규칙이다(둘 중 하나만 쓴다).
     */
    fun add(
        date: LocalDate,
        title: String,
        repeat: RepeatRule = RepeatRule.NONE,
        lunar: LunarDate? = null,
    ): List<CalendarEvent> {
        val trimmed = title.trim().take(MAX_TITLE_LENGTH)
        if (trimmed.isEmpty()) return all()
        val event = CalendarEvent(
            id = "local-${System.currentTimeMillis()}-${(0..999).random()}",
            date = date,
            title = trimmed,
            repeat = if (lunar != null) RepeatRule.YEARLY else repeat,
            lunar = lunar != null,
            lunarMonth = lunar?.month ?: 0,
            lunarDay = lunar?.day ?: 0,
            lunarLeap = lunar?.leap ?: false,
        )
        val updated = (all() + event).sortedWith(EventJson.ORDER)
        save(updated)
        return updated
    }

    fun remove(id: String): List<CalendarEvent> {
        val updated = all().filterNot { it.id == id }
        save(updated)
        return updated
    }

    /**
     * 반복 일정에서 [date] 회차만 건너뛴다. 원본은 남고 그 날짜만 화면에서 빠진다.
     * 반복이 아닌 일정은 이 함수로 지우지 않는다(그때는 [remove]).
     */
    fun skipOccurrence(id: String, date: LocalDate): List<CalendarEvent> {
        val events = all()
        val target = events.firstOrNull { it.id == id } ?: return events
        if (!target.isRepeating) return events
        if (date in target.skipDates) return events
        val updated = events.map {
            if (it.id == id) it.copy(skipDates = it.skipDates + date) else it
        }
        save(updated)
        return updated
    }

    private fun save(events: List<CalendarEvent>) {
        try {
            prefs.edit().putString(KEY_EVENTS, EventJson.encode(events)).apply()
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.STORE_WRITE, "event write failed: ${t.javaClass.simpleName}")
        }
    }

    private companion object {
        const val FILE = "events"
        const val KEY_EVENTS = "events"
        const val MAX_TITLE_LENGTH = 60
    }
}
