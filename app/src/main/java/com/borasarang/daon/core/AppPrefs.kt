package com.borasarang.daon.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 로컬 설정 저장소. v0.1 은 SharedPreferences 로 충분하다.
 * 쓰기 실패는 조용히 삼키지 않고 E-AND-STORE-0001 로 남긴다.
 */
object AppPrefs {
    const val LOCALE_SYSTEM = "system"

    private const val FILE = "settings"
    private const val KEY_CITY = "city"
    private const val KEY_HOUR24 = "hour24"
    private const val KEY_CELSIUS = "celsius"
    private const val KEY_LOCALE = "locale"
    private const val KEY_ROTATE = "rotate"
    private const val KEY_DWELL_CALENDAR = "dwell_calendar"
    private const val KEY_DWELL_WEATHER = "dwell_weather"
    private const val KEY_DWELL_CLOCK = "dwell_clock"
    private const val KEY_TOUCH_STOP = "touch_stop"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_WEATHER_MOTION = "weather_motion"
    private const val KEY_CLOCK_FACE = "clock_face"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var cityId: String
        get() = prefs.getString(KEY_CITY, "seoul") ?: "seoul"
        set(value) = writeString(KEY_CITY, value)

    /** 70대 사용자 기준. 기본은 오전·오후가 함께 보이는 12시간제다. */
    var hour24: Boolean
        get() = prefs.getBoolean(KEY_HOUR24, false)
        set(value) = write { putBoolean(KEY_HOUR24, value) }

    var celsius: Boolean
        get() = prefs.getBoolean(KEY_CELSIUS, true)
        set(value) = write { putBoolean(KEY_CELSIUS, value) }

    var localeTag: String
        get() = prefs.getString(KEY_LOCALE, LOCALE_SYSTEM) ?: LOCALE_SYSTEM
        set(value) = writeString(KEY_LOCALE, value)

    /** 3화면 자동 전환. 기본 켬. */
    var rotationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ROTATE, true)
        set(value) = write { putBoolean(KEY_ROTATE, value) }

    /** 화면별 머무는 시간(초). 허용값 10·20·30·60. */
    var dwellCalendar: Int
        get() = prefs.getInt(KEY_DWELL_CALENDAR, DEFAULT_DWELL_CALENDAR)
        set(value) = write { putInt(KEY_DWELL_CALENDAR, value) }

    var dwellWeather: Int
        get() = prefs.getInt(KEY_DWELL_WEATHER, DEFAULT_DWELL_WEATHER)
        set(value) = write { putInt(KEY_DWELL_WEATHER, value) }

    var dwellClock: Int
        get() = prefs.getInt(KEY_DWELL_CLOCK, DEFAULT_DWELL_CLOCK)
        set(value) = write { putInt(KEY_DWELL_CLOCK, value) }

    /** true면 터치 시 현재 화면에서 멈춤, false면 대시보드(달력)로 복귀. */
    var touchStop: Boolean
        get() = prefs.getBoolean(KEY_TOUCH_STOP, false)
        set(value) = write { putBoolean(KEY_TOUCH_STOP, value) }

    /** 글씨 배율(%). 90·100·115. 100이 승인 목업 크기다. */
    var textScale: Int
        get() = prefs.getInt(KEY_TEXT_SCALE, DEFAULT_TEXT_SCALE)
        set(value) = write { putInt(KEY_TEXT_SCALE, value) }

    /** 비·눈·천둥 효과 표시. 기본 켬. */
    var weatherMotion: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_MOTION, true)
        set(value) = write { putBoolean(KEY_WEATHER_MOTION, value) }

    /** 시계 화면 모양 이름. [ClockFace] 참조. */
    var clockFace: String
        get() = prefs.getString(KEY_CLOCK_FACE, ClockFace.ANALOG.name) ?: ClockFace.ANALOG.name
        set(value) = writeString(KEY_CLOCK_FACE, value)

    private fun writeString(key: String, value: String) = write { putString(key, value) }

    private fun write(block: SharedPreferences.Editor.() -> Unit) {
        try {
            prefs.edit().apply(block).apply()
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.STORE_WRITE, "write failed: ${t.javaClass.simpleName}")
        }
    }

    const val DEFAULT_DWELL_CALENDAR = 30
    const val DEFAULT_DWELL_WEATHER = 20
    const val DEFAULT_DWELL_CLOCK = 20
    const val DEFAULT_TEXT_SCALE = 100

    /** 설정에서 고를 수 있는 머무는 시간(초). */
    val DWELL_OPTIONS = listOf(10, 20, 30, 60)

    /** 글씨 배율 선택지(%). */
    val TEXT_SCALE_OPTIONS = listOf(90, 100, 115)
}
