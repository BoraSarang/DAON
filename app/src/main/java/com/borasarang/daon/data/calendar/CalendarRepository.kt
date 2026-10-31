package com.borasarang.daon.data.calendar

import android.content.Context
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode

/** 번들 자산에서 [CalendarData]를 읽는다. 실패해도 앱은 달력 없이 계속 동작한다. */
object CalendarRepository {

    fun load(context: Context): CalendarData {
        val started = android.os.SystemClock.uptimeMillis()
        return try {
            val data = CalendarData.parse { name ->
                context.assets.open("calendar/$name").bufferedReader().use { it.readText() }
            }
            val elapsed = android.os.SystemClock.uptimeMillis() - started
            DebugLogger.perf("CALENDAR", "loaded elapsed=${elapsed}ms")
            data
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.CALENDAR_ASSET, "load failed: ${t.javaClass.simpleName} ${t.message}")
            CalendarData.empty()
        }
    }
}
