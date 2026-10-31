package com.borasarang.daon.data.event

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 기기 캘린더 읽기(READ_CALENDAR). Fire OS 에는 캘린더 앱이 없어 provider 가 없을 수 있고,
 * 그때는 [isAvailable] 이 false 를 돌려주고 앱은 로컬 일정만 쓴다.
 *
 * 일정은 [from]~[to] 범위만 읽는다. 앱은 이 범위를 벗어난 달을 넘겨도 점 표시가 없을 뿐,
 * 달력 자체는 정상 동작한다.
 */
class DeviceCalendar(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** 캘린더 provider 가 있고 일정을 하나라도 담고 있는지. */
    fun isAvailable(): Boolean = try {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            null,
            null,
            null,
        )?.use { it.count > 0 } ?: false
    } catch (t: Throwable) {
        DebugLogger.i("CALENDAR", "provider unavailable: ${t.javaClass.simpleName}")
        false
    }

    fun events(from: LocalDate, to: LocalDate): List<CalendarEvent> {
        if (!hasPermission()) return emptyList()
        val zone = ZoneId.systemDefault()
        val begin = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(begin.toString())
            .appendPath(end.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )
        return try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val calendarIndex =
                    cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                buildList {
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(titleIndex)?.trim().orEmpty()
                        if (title.isEmpty()) continue
                        val allDay = cursor.getInt(allDayIndex) == 1
                        val beginMs = cursor.getLong(beginIndex)
                        val instant = Instant.ofEpochMilli(beginMs)
                        val date = if (allDay) {
                            instant.atZone(ZoneOffset.UTC).toLocalDate()
                        } else {
                            instant.atZone(zone).toLocalDate()
                        }
                        val time = if (allDay) {
                            null
                        } else {
                            instant.atZone(zone).toLocalTime().format(TIME_FORMAT)
                        }
                        add(
                            CalendarEvent(
                                id = "device-${cursor.getLong(idIndex)}-$beginMs",
                                date = date,
                                title = title,
                                time = time,
                                source = EventSource.DEVICE,
                                calendarName = cursor.getString(calendarIndex),
                            ),
                        )
                    }
                }.distinctBy { it.id }.sortedWith(EventJson.ORDER)
            } ?: emptyList()
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.PERM_CALENDAR, "read failed: ${t.javaClass.simpleName}")
            emptyList()
        }
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
