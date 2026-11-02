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
    private const val KEY_RADIO_IN_ROTATION = "radio_in_rotation"
    private const val KEY_DWELL_RADIO = "dwell_radio"
    private const val KEY_RADIO_CHANNEL = "radio_channel"
    private const val KEY_RADIO_AUTOPLAY = "radio_autoplay"
    private const val KEY_RADIO_VOLUME = "radio_volume"
    private const val KEY_TV_IN_ROTATION = "tv_in_rotation"
    private const val KEY_DWELL_TV = "dwell_tv"
    private const val KEY_TV_CHANNEL = "tv_channel"
    private const val KEY_TOUCH_STOP = "touch_stop"
    private const val KEY_TOUCH_ACTION = "touch_action"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_WEATHER_MOTION = "weather_motion"
    private const val KEY_CLOCK_FACE = "clock_face"
    private const val KEY_THEME_LIGHT = "theme_light"
    private const val KEY_SCREEN_BRIGHTNESS = "screen_brightness"
    private const val KEY_LOCATION_ENABLED = "location_enabled"
    private const val KEY_LOCATION_ASKED = "location_asked"
    private const val KEY_LOCATION_LAT = "location_lat"
    private const val KEY_LOCATION_LON = "location_lon"
    private const val KEY_LOCATION_ACCURACY = "location_accuracy"
    private const val KEY_LOCATION_TIME = "location_time"
    private const val KEY_PLACE_NAME = "place_name"
    private const val KEY_PLACE_LAT = "place_lat"
    private const val KEY_PLACE_LON = "place_lon"
    private const val KEY_DEVICE_CALENDAR = "device_calendar"
    private const val KEY_DEVICE_CALENDAR_ASKED = "device_calendar_asked"
    private const val KEY_KIOSK_LOCK = "kiosk_lock"
    private const val KEY_KIOSK_BOOT = "kiosk_boot"
    private const val KEY_NEWS_ENABLED = "news_enabled"
    private const val KEY_NEWS_IN_ROTATION = "news_in_rotation"
    private const val KEY_DWELL_NEWS = "dwell_news"
    private const val KEY_NEWS_INTERVAL = "news_interval_min"
    private const val KEY_NEWS_ITEMS = "news_items"

    private fun newsUpdatedKey(sourceId: String) = "news_updated_$sourceId"

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

    /** 라디오 화면을 자동 전환에 넣을지. 기본 켬. */
    var radioInRotation: Boolean
        get() = prefs.getBoolean(KEY_RADIO_IN_ROTATION, true)
        set(value) = write { putBoolean(KEY_RADIO_IN_ROTATION, value) }

    /** 라디오 화면에 머무는 시간(초). 허용값 10·30·60. */
    var dwellRadio: Int
        get() = prefs.getInt(KEY_DWELL_RADIO, DEFAULT_DWELL_RADIO)
        set(value) = write { putInt(KEY_DWELL_RADIO, value) }

    /** 마지막으로 고른(또는 기본으로 정한) 채널 id. */
    var radioChannelId: String
        get() = prefs.getString(KEY_RADIO_CHANNEL, DEFAULT_RADIO_CHANNEL)
            ?: DEFAULT_RADIO_CHANNEL
        set(value) = writeString(KEY_RADIO_CHANNEL, value)

    /** 앱을 열거나 부팅했을 때 기본 채널을 바로 재생할지. 기본 끔. */
    var radioAutoplay: Boolean
        get() = prefs.getBoolean(KEY_RADIO_AUTOPLAY, false)
        set(value) = write { putBoolean(KEY_RADIO_AUTOPLAY, value) }

    /** 라디오 소리 크기(0..10). 기기 미디어 볼륨과 별개인 앱 배율이다. */
    var radioVolume: Int
        get() = prefs.getInt(KEY_RADIO_VOLUME, DEFAULT_RADIO_VOLUME)
        set(value) = write { putInt(KEY_RADIO_VOLUME, value) }

    /** TV 화면을 자동 전환에 넣을지. 기본 켬. */
    var tvInRotation: Boolean
        get() = prefs.getBoolean(KEY_TV_IN_ROTATION, true)
        set(value) = write { putBoolean(KEY_TV_IN_ROTATION, value) }

    /** TV 화면에 머무는 시간(초). 허용값 10·30·60. 재생 중에는 전환이 멈춘다. */
    var dwellTv: Int
        get() = prefs.getInt(KEY_DWELL_TV, DEFAULT_DWELL_TV)
        set(value) = write { putInt(KEY_DWELL_TV, value) }

    /** 뉴스 화면을 자동 전환에 넣을지. 기본 켬. */
    var newsInRotation: Boolean
        get() = prefs.getBoolean(KEY_NEWS_IN_ROTATION, true)
        set(value) = write { putBoolean(KEY_NEWS_IN_ROTATION, value) }

    /** 뉴스 화면에 머무는 시간(초). 허용값 10·30·60. */
    var dwellNews: Int
        get() = prefs.getInt(KEY_DWELL_NEWS, DEFAULT_DWELL_NEWS)
        set(value) = write { putInt(KEY_DWELL_NEWS, value) }

    /** 마지막으로 고른 TV 채널 id. */
    var tvChannelId: String
        get() = prefs.getString(KEY_TV_CHANNEL, DEFAULT_TV_CHANNEL) ?: DEFAULT_TV_CHANNEL
        set(value) = writeString(KEY_TV_CHANNEL, value)

    /**
     * 자동 전환 중 화면을 한 번 탭했을 때의 동작.
     *
     * 예전 `touch_stop`(true=멈춤, false=달력)만 있는 기기는 그 값으로 옮겨 읽는다.
     * 새 키가 생기면 `touch_action` 이 우선이다.
     */
    var touchAction: TouchAction
        get() {
            val saved = prefs.getString(KEY_TOUCH_ACTION, null)
            if (saved != null) {
                TouchAction.entries.firstOrNull { it.name == saved }?.let { return it }
            }
            return if (prefs.getBoolean(KEY_TOUCH_STOP, false)) {
                TouchAction.PAUSE_TOGGLE
            } else {
                TouchAction.DASHBOARD
            }
        }
        set(value) = writeString(KEY_TOUCH_ACTION, value.name)

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

    /**
     * 밝은 테마를 쓸지. 기본은 블랙(어두운) 테마다.
     * 키오스크 고정 중에는 기기 설정에 들어갈 수 없어 앱 안에서 바꾼다.
     */
    var themeLight: Boolean
        get() = prefs.getBoolean(KEY_THEME_LIGHT, false)
        set(value) = write { putBoolean(KEY_THEME_LIGHT, value) }

    /**
     * 화면 밝기(%). [BRIGHTNESS_SYSTEM] 이면 기기 설정을 그대로 따른다.
     * 창 밝기만 바꾸므로 기기의 자동 밝기 설정은 건드리지 않는다.
     */
    var screenBrightness: Int
        get() = prefs.getInt(KEY_SCREEN_BRIGHTNESS, BRIGHTNESS_SYSTEM)
        set(value) = write { putInt(KEY_SCREEN_BRIGHTNESS, value) }

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

    /** 기기 캘린더 일정을 달력에 함께 표시할지. 기본 켬. */
    var deviceCalendarEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_CALENDAR, true)
        set(value) = write { putBoolean(KEY_DEVICE_CALENDAR, value) }

    /** 캘린더 권한을 한 번이라도 요청했는지. 거부 후 자동 팝업을 반복하지 않는다. */
    var deviceCalendarAsked: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_CALENDAR_ASKED, false)
        set(value) = write { putBoolean(KEY_DEVICE_CALENDAR_ASKED, value) }

    /** 사용자가 마지막으로 요청한 키오스크 고정 상태. 실제 고정 여부는 [KioskMode] 로 확인한다. */
    var kioskLock: Boolean
        get() = prefs.getBoolean(KEY_KIOSK_LOCK, false)
        set(value) = write { putBoolean(KEY_KIOSK_LOCK, value) }

    /** 부팅이 끝나면 대시보드를 자동으로 연다. 기본 끔(Fire OS 가 막을 수 있어 사용자가 켠다). */
    var kioskBootStart: Boolean
        get() = prefs.getBoolean(KEY_KIOSK_BOOT, false)
        set(value) = write { putBoolean(KEY_KIOSK_BOOT, value) }

    /** 달력 상단 뉴스 티커 표시. 기본 켬. 끄면 네트워크 요청도 하지 않는다. */
    var newsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEWS_ENABLED, true)
        set(value) = write { putBoolean(KEY_NEWS_ENABLED, value) }

    /** 뉴스 갱신 간격(분). 30·60·180·360, 기본 60. */
    var newsIntervalMinutes: Int
        get() = prefs.getInt(KEY_NEWS_INTERVAL, DEFAULT_NEWS_INTERVAL_MIN)
        set(value) = write { putInt(KEY_NEWS_INTERVAL, value) }

    /** 출처별 최대 30건(합계 60건) 캐시 JSON. 본문은 저장하지 않는다. */
    var newsItemsJson: String
        get() = prefs.getString(KEY_NEWS_ITEMS, "") ?: ""
        set(value) = writeString(KEY_NEWS_ITEMS, value)

    /** 그 출처의 마지막 성공 시각(epoch millis). 한 번도 성공하지 못했으면 0. */
    fun newsUpdatedAt(sourceId: String): Long =
        prefs.getLong(newsUpdatedKey(sourceId), 0L)

    fun setNewsUpdatedAt(sourceId: String, millis: Long) {
        write { putLong(newsUpdatedKey(sourceId), millis) }
    }

    private fun readDouble(key: String): Double? =
        if (prefs.contains(key)) Double.fromBits(prefs.getLong(key, 0L)) else null

    private fun writeString(key: String, value: String) = write { putString(key, value) }

    private fun write(block: SharedPreferences.Editor.() -> Unit) {
        try {
            // 설정은 드물게 바뀌고 양이 적다. apply() 는 메모리에만 먼저 반영하고 디스크
            // 기록은 미뤄서, 곧바로 앱이 내려가면(키오스크 재부팅·강제 종료) 값이 사라질 수
            // 있다. 사용자가 바꾼 설정은 바로 확정되도록 commit() 을 쓴다.
            val saved = prefs.edit().apply(block).commit()
            if (!saved) DebugLogger.e(ErrorCode.STORE_WRITE, "commit returned false")
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.STORE_WRITE, "write failed: ${t.javaClass.simpleName}")
        }
    }

    const val DEFAULT_DWELL_CALENDAR = 30
    const val DEFAULT_DWELL_WEATHER = 20
    const val DEFAULT_DWELL_CLOCK = 20
    const val DEFAULT_DWELL_RADIO = 30
    const val DEFAULT_TEXT_SCALE = 100
    const val DEFAULT_RADIO_CHANNEL = "kbs_1radio"
    const val DEFAULT_RADIO_VOLUME = 6
    const val DEFAULT_DWELL_TV = 30
    const val DEFAULT_TV_CHANNEL = "kbs1"
    const val DEFAULT_DWELL_NEWS = 30
    const val DEFAULT_NEWS_INTERVAL_MIN = 60

    /** 설정에서 고를 수 있는 머무는 시간(초). */
    val DWELL_OPTIONS = listOf(10, 20, 30, 60)

    /** 라디오 화면은 10·30·60초만 고른다. */
    val RADIO_DWELL_OPTIONS = listOf(10, 30, 60)

    /** TV 화면도 10·30·60초만 고른다. */
    val TV_DWELL_OPTIONS = listOf(10, 30, 60)

    /** 뉴스 화면도 10·30·60초만 고른다. 30초면 기사 3건(10초씩)을 본다. */
    val NEWS_DWELL_OPTIONS = listOf(10, 30, 60)

    /** 글씨 배율 선택지(%). */
    val TEXT_SCALE_OPTIONS = listOf(90, 100, 115)

    /** 기기 밝기 설정을 그대로 쓰는 값. 창 밝기 지정값은 0..100 이다. */
    const val BRIGHTNESS_SYSTEM = -1

    /**
     * 설정에서 고를 수 있는 화면 밝기(%). [BRIGHTNESS_SYSTEM] 은 기기 설정.
     * 상시 켜짐 기기라 밤에는 눈부시지 않게, 낮에는 잘 보이게 고른다.
     */
    val BRIGHTNESS_OPTIONS = listOf(BRIGHTNESS_SYSTEM, 20, 40, 60, 80, 100)
}
