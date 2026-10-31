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
    private const val KEY_LOCATION_ENABLED = "location_enabled"
    private const val KEY_LOCATION_ASKED = "location_asked"
    private const val KEY_LOCATION_LAT = "location_lat"
    private const val KEY_LOCATION_LON = "location_lon"
    private const val KEY_LOCATION_ACCURACY = "location_accuracy"
    private const val KEY_LOCATION_TIME = "location_time"
    private const val KEY_PLACE_NAME = "place_name"
    private const val KEY_PLACE_LAT = "place_lat"
    private const val KEY_PLACE_LON = "place_lon"

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

    /** 현재 위치로 날씨를 조회할지. 기본 켬. 끄면 [cityId] 도시를 쓴다. */
    var locationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_ENABLED, true)
        set(value) = write { putBoolean(KEY_LOCATION_ENABLED, value) }

    /** 위치 권한을 한 번이라도 요청했는지. 거부 후 자동 팝업을 반복하지 않는다. */
    var locationAsked: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_ASKED, false)
        set(value) = write { putBoolean(KEY_LOCATION_ASKED, value) }

    /** 마지막 위치 좌표를 저장한다. Double 은 비트 그대로 보존한다. */
    fun saveLocation(latitude: Double, longitude: Double, accuracyMeters: Float, timeMs: Long) {
        write {
            putLong(KEY_LOCATION_LAT, latitude.toRawBits())
            putLong(KEY_LOCATION_LON, longitude.toRawBits())
            putFloat(KEY_LOCATION_ACCURACY, accuracyMeters)
            putLong(KEY_LOCATION_TIME, timeMs)
        }
    }

    fun cachedLatitude(): Double? = readDouble(KEY_LOCATION_LAT)

    fun cachedLongitude(): Double? = readDouble(KEY_LOCATION_LON)

    fun cachedAccuracy(): Float =
        if (prefs.contains(KEY_LOCATION_ACCURACY)) prefs.getFloat(KEY_LOCATION_ACCURACY, -1f) else -1f

    fun cachedLocationTime(): Long =
        if (prefs.contains(KEY_LOCATION_TIME)) prefs.getLong(KEY_LOCATION_TIME, 0L) else 0L

    fun clearLocation() {
        write {
            remove(KEY_LOCATION_LAT)
            remove(KEY_LOCATION_LON)
            remove(KEY_LOCATION_ACCURACY)
            remove(KEY_LOCATION_TIME)
        }
    }

    /**
     * 설정에서 검색해 저장한 동네 이름. 없으면 null.
     * 기기 위치를 못 얻는 Fire 태블릿에서 쓰는 실제 좌표원이다.
     */
    var placeName: String?
        get() = prefs.getString(KEY_PLACE_NAME, null)
        set(value) = write {
            if (value == null) {
                remove(KEY_PLACE_NAME)
            } else {
                putString(KEY_PLACE_NAME, value)
            }
        }

    fun savePlace(name: String, latitude: Double, longitude: Double) {
        write {
            putString(KEY_PLACE_NAME, name)
            putLong(KEY_PLACE_LAT, latitude.toRawBits())
            putLong(KEY_PLACE_LON, longitude.toRawBits())
        }
    }

    fun savedPlaceLatitude(): Double? = readDouble(KEY_PLACE_LAT)

    fun savedPlaceLongitude(): Double? = readDouble(KEY_PLACE_LON)

    fun clearPlace() {
        write {
            remove(KEY_PLACE_NAME)
            remove(KEY_PLACE_LAT)
            remove(KEY_PLACE_LON)
        }
    }

    private fun readDouble(key: String): Double? =
        if (prefs.contains(key)) Double.fromBits(prefs.getLong(key, 0L)) else null

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
