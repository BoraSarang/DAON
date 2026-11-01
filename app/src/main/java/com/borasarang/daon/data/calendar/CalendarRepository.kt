package com.borasarang.daon.data.calendar

import android.content.Context
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 번들 자산에서 [CalendarData]를 읽는다. 실패해도 앱은 달력 없이 계속 동작한다. */
object CalendarRepository {

    /** 공휴일 자산 만료가 이만큼 남으면 갱신하라고 로그를 남긴다. */
    private const val STALE_WARNING_DAYS = 60L

    fun load(context: Context): CalendarData {
        val started = android.os.SystemClock.uptimeMillis()
        return try {
            val data = CalendarData.parse { name ->
                context.assets.open("calendar/$name").bufferedReader().use { it.readText() }
            }
            val elapsed = android.os.SystemClock.uptimeMillis() - started
            DebugLogger.perf("CALENDAR", "loaded elapsed=${elapsed}ms")
            logHolidayCoverage(data)
            data
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.CALENDAR_ASSET, "load failed: ${t.javaClass.simpleName} ${t.message}")
            CalendarData.empty()
        }
    }

    /**
     * 공휴일 자산의 마지막 날짜를 오늘과 견준다(T-023).
     *
     * 자산이 끝난 뒤에는 앱이 공휴일을 조용히 빼고 그린다. 화면만 보면 알 수 없으므로
     * 시작할 때 한 줄 남겨 갱신 시점을 알린다. 실제 차단은 `CalendarAssetsTest` 가 한다.
     */
    private fun logHolidayCoverage(data: CalendarData) {
        val end = data.holidayCoverageEnd ?: return
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), end)
        val refresh = "renew calendar assets per docs/api/CALENDAR_DATA.md section 5"
        when {
            daysLeft < 0 -> DebugLogger.e(
                ErrorCode.CALENDAR_STALE,
                "holiday data ended $end (${-daysLeft}d ago) - $refresh",
            )
            daysLeft <= STALE_WARNING_DAYS -> DebugLogger.i(
                "CALENDAR",
                "holiday data ends $end in ${daysLeft}d - $refresh",
            )
            else -> DebugLogger.i("CALENDAR", "holiday data through $end")
        }
    }
}
