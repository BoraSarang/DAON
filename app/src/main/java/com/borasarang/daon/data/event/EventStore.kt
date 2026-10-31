package com.borasarang.daon.data.event

import android.content.Context
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import java.time.LocalDate

/**
 * 이 태블릿에서 직접 추가한 일정 저장소.
 *
 * v0.1.1 은 일정 수가 적고 편집이 드물어 SharedPreferences + JSON 한 덩어리로 충분하다.
 * 저장 실패는 E-AND-STORE-0001 로 남기고, 메모리 상태는 그대로 유지한다.
 */
class EventStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun all(): List<CalendarEvent> = EventJson.decode(prefs.getString(KEY_EVENTS, null))

    fun add(date: LocalDate, title: String): List<CalendarEvent> {
        val trimmed = title.trim().take(MAX_TITLE_LENGTH)
        if (trimmed.isEmpty()) return all()
        val event = CalendarEvent(
            id = "local-${System.currentTimeMillis()}-${(0..999).random()}",
            date = date,
            title = trimmed,
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
