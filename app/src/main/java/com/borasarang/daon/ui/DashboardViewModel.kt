package com.borasarang.daon.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.borasarang.daon.R
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.ClockFace
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import com.borasarang.daon.data.City
import com.borasarang.daon.data.DeviceLocation
import com.borasarang.daon.data.Place
import com.borasarang.daon.data.PlaceResult
import com.borasarang.daon.data.PlaceSearch
import com.borasarang.daon.data.WeatherException
import com.borasarang.daon.data.WeatherRepository
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.CalendarRepository
import com.borasarang.daon.data.event.CalendarEvent
import com.borasarang.daon.data.event.DeviceCalendar
import com.borasarang.daon.data.event.EventJson
import com.borasarang.daon.data.event.EventStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val isLoading: Boolean = false,
    val errorRes: Int? = null,
    val isOffline: Boolean = false,
)

/** 현재 위치 조회 상태. 화면에는 [DashboardViewModel.locationStatus] 로 전달한다. */
enum class LocationStatus {
    /** 설정에서 현재 위치 사용을 껐다. */
    OFF,

    /** 좌표를 찾는 중. */
    SEARCHING,

    /** 좌표를 얻었다(저장된 좌표 포함). */
    READY,

    /** 위치 권한이 없다. 도시 폴백 중. */
    NO_PERMISSION,

    /** 기기 위치 서비스가 꺼져 있다. 도시 폴백 중. */
    SERVICE_OFF,

    /** 권한은 있지만 좌표를 못 얻었다. 도시 폴백 중. */
    UNAVAILABLE,
}

/** 기기 캘린더 연결 상태. Fire OS 처럼 provider 가 없으면 [UNAVAILABLE] 이다. */
enum class CalendarStatus {
    /** 설정에서 기기 캘린더를 껐다. */
    OFF,

    /** provider·권한을 확인하는 중. */
    CHECKING,

    /** 읽기 가능. */
    AVAILABLE,

    /** provider 는 있지만 READ_CALENDAR 권한이 없다. */
    NO_PERMISSION,

    /** 이 기기에 캘린더 provider 가 없다. */
    UNAVAILABLE,
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WeatherRepository()
    private val deviceLocation = DeviceLocation(app)
    private val placeSearch = PlaceSearch()

    /** 1초 주기 시계. 화면이 없을 때는 5초 후 정지한다. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LocalDateTime.now())

    private val _weather = MutableStateFlow(WeatherUiState(isLoading = true))
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    /** 날씨를 조회하는 지점. 현재 위치면 [Place.city] 가 null 이다. */
    private val _place = MutableStateFlow(initialPlace())
    val place: StateFlow<Place> = _place.asStateFlow()

    /** 위치를 못 찾았을 때 쓰는 도시. 설정의 도시 칩이 이 값을 고른다. */
    private val _city = MutableStateFlow(City.fromId(AppPrefs.cityId))
    val city: StateFlow<City> = _city.asStateFlow()

    var locationEnabled by mutableStateOf(AppPrefs.locationEnabled)
        private set
    var locationStatus by mutableStateOf(LocationStatus.SEARCHING)
        private set
    var locationAccuracy by mutableStateOf<Float?>(null)
        private set

    /** 번들 달력 자산. 파싱 전에는 비어 있고, 준비되면 음력·절기·공휴일이 나타난다. */
    private val _calendar = MutableStateFlow(CalendarData.empty())
    val calendar: StateFlow<CalendarData> = _calendar.asStateFlow()

    private val eventStore = EventStore(app)
    private val deviceCalendar = DeviceCalendar(app)
    private val _localEvents = MutableStateFlow(eventStore.all())
    private val _deviceEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())

    /** 로컬 일정 + 기기 캘린더 일정. 날짜·시간 순으로 정렬된다. */
    val events: StateFlow<List<CalendarEvent>> = combine(_localEvents, _deviceEvents) { local, device ->
        (local + device).sortedWith(EventJson.ORDER)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), _localEvents.value)

    var deviceCalendarEnabled by mutableStateOf(AppPrefs.deviceCalendarEnabled)
        private set
    var calendarStatus by mutableStateOf(CalendarStatus.CHECKING)
        private set
    var kioskBootStart by mutableStateOf(AppPrefs.kioskBootStart)
        private set

    var hour24 by mutableStateOf(AppPrefs.hour24)
        private set
    var celsius by mutableStateOf(AppPrefs.celsius)
        private set
    var localeTag by mutableStateOf(AppPrefs.localeTag)
        private set
    var rotationEnabled by mutableStateOf(AppPrefs.rotationEnabled)
        private set
    var dwellCalendar by mutableStateOf(AppPrefs.dwellCalendar)
        private set
    var dwellWeather by mutableStateOf(AppPrefs.dwellWeather)
        private set
    var dwellClock by mutableStateOf(AppPrefs.dwellClock)
        private set
    var touchStop by mutableStateOf(AppPrefs.touchStop)
        private set
    var textScale by mutableStateOf(AppPrefs.textScale)
        private set
    var weatherMotion by mutableStateOf(AppPrefs.weatherMotion)
        private set
    var clockFace by mutableStateOf(ClockFace.fromName(AppPrefs.clockFace))
        private set

    private var refreshJob: Job? = null
    private var locationJob: Job? = null
    private var searchJob: Job? = null

    init {
        DebugLogger.i("FEATURE", "dashboard")
        locationStatus = when {
            !AppPrefs.locationEnabled -> LocationStatus.OFF
            !deviceLocation.hasPermission() -> LocationStatus.NO_PERMISSION
            deviceLocation.cached() != null -> LocationStatus.READY
            else -> LocationStatus.SEARCHING
        }
        locationAccuracy = deviceLocation.cached()?.accuracyMeters?.takeIf { it > 0f }
        viewModelScope.launch(Dispatchers.IO) {
            _calendar.value = CalendarRepository.load(getApplication())
        }
        refreshDeviceCalendar()
        refresh()
        startLocationRefresh(force = false)
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _weather.update { it.copy(isLoading = true, errorRes = null) }
            val place = _place.value
            DebugLogger.i(
                "WEATHER",
                "fetch lat=${String.format(java.util.Locale.US, "%.3f", place.latitude)} " +
                    "lon=${String.format(java.util.Locale.US, "%.3f", place.longitude)} auto=${place.auto}",
            )
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.fetch(place.latitude, place.longitude, celsius) }
            }
            result.fold(
                onSuccess = { snapshot ->
                    DebugLogger.perf("WEATHER", "fetched days=${snapshot.daily.size} hours=${snapshot.hourly.size}")
                    _weather.value = WeatherUiState(snapshot = snapshot, isLoading = false, errorRes = null)
                },
                onFailure = { throwable ->
                    val code = (throwable as? WeatherException)?.code ?: ErrorCode.NET_HTTP
                    DebugLogger.e(code, throwable.message ?: throwable.javaClass.simpleName)
                    val messageRes = if (code == ErrorCode.NET_PARSE) {
                        R.string.error_weather_parse
                    } else {
                        R.string.error_weather_network
                    }
                    _weather.update {
                        it.copy(
                            isLoading = false,
                            errorRes = messageRes,
                            isOffline = it.snapshot != null,
                        )
                    }
                },
            )
        }
    }

    fun setCity(city: City) {
        AppPrefs.cityId = city.id
        _city.value = city
        // 도시를 직접 고르면 검색해 둔 동네는 지운다. 사용자의 마지막 선택을 존중한다.
        AppPrefs.clearPlace()
        savedPlaceName = null
        DebugLogger.i("SETTINGS", "city=${city.id}")
        if (!_place.value.auto) {
            _place.value = Place.of(city)
            refresh()
        }
    }

    /** 설정에서 고른 동네. 없으면 null. 폴백 우선순위는 현재 위치 → 저장한 동네 → 도시. */
    var savedPlaceName by mutableStateOf(AppPrefs.placeName)
        private set

    var placeQuery by mutableStateOf("")
        private set
    var placeResults by mutableStateOf<List<PlaceResult>>(emptyList())
        private set
    var placeSearching by mutableStateOf(false)
        private set
    var placeSearchError by mutableStateOf<Int?>(null)
        private set

    fun updatePlaceQuery(text: String) {
        placeQuery = text
        placeSearchError = null
        searchJob?.cancel()
        val query = text.trim()
        if (query.length < 2) {
            placeResults = emptyList()
            return
        }
        // 타이핑이 멈추면 자동으로 찾는다. Nominatim 은 초당 1회 이하만 허용한다.
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query)
        }
    }

    /** `검색`을 직접 눌렀을 때. 자동 검색을 기다리지 않고 바로 찾는다. */
    fun searchPlace() {
        searchJob?.cancel()
        val query = placeQuery.trim()
        if (query.length < 2) {
            placeSearchError = R.string.place_search_too_short
            return
        }
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        placeSearching = true
        placeSearchError = null
        val result = withContext(Dispatchers.IO) {
            runCatching { placeSearch.search(query) }
        }
        placeSearching = false
        result.onSuccess { places ->
            placeResults = places
            if (places.isEmpty()) placeSearchError = R.string.place_search_empty
            DebugLogger.i("PLACE", "search results=${places.size}")
        }.onFailure { error ->
            placeResults = emptyList()
            placeSearchError = R.string.place_search_failed
            DebugLogger.e(
                ErrorCode.NET_HTTP,
                "place search failed: ${error.javaClass.simpleName}",
            )
        }
    }

    /** 검색 결과에서 한 곳을 저장한다. 이후 자동 위치가 실패하면 이 지점을 쓴다. */
    fun saveSearchedPlace(result: PlaceResult) {
        val label = result.label
        AppPrefs.savePlace(label, result.latitude, result.longitude)
        savedPlaceName = label
        placeQuery = ""
        placeResults = emptyList()
        placeSearchError = null
        DebugLogger.i("SETTINGS", "place saved")
        if (!_place.value.auto) {
            _place.value = Place.saved(result.latitude, result.longitude, label)
            refresh()
        }
    }

    fun clearSavedPlace() {
        AppPrefs.clearPlace()
        savedPlaceName = null
        DebugLogger.i("SETTINGS", "place cleared")
        if (!_place.value.auto && applyFallbackPlace()) refresh()
    }

    fun clearPlaceSearch() {
        placeQuery = ""
        placeResults = emptyList()
        placeSearchError = null
        placeSearching = false
    }

    /**
     * 기기 캘린더를 다시 읽는다. provider 가 없거나 권한이 없으면 상태만 바꾸고
     * 로컬 일정은 그대로 유지한다.
     */
    fun refreshDeviceCalendar() {
        viewModelScope.launch {
            if (!deviceCalendarEnabled) {
                calendarStatus = CalendarStatus.OFF
                _deviceEvents.value = emptyList()
                return@launch
            }
            calendarStatus = CalendarStatus.CHECKING
            val available = withContext(Dispatchers.IO) { deviceCalendar.isAvailable() }
            if (!available) {
                calendarStatus = CalendarStatus.UNAVAILABLE
                _deviceEvents.value = emptyList()
                DebugLogger.i("CALENDAR", "device provider unavailable")
                return@launch
            }
            if (!deviceCalendar.hasPermission()) {
                calendarStatus = CalendarStatus.NO_PERMISSION
                _deviceEvents.value = emptyList()
                return@launch
            }
            val today = LocalDate.now()
            val list = withContext(Dispatchers.IO) {
                deviceCalendar.events(today.minusMonths(DEVICE_CALENDAR_MONTHS_BACK), today.plusMonths(DEVICE_CALENDAR_MONTHS_FORWARD))
            }
            _deviceEvents.value = list
            calendarStatus = CalendarStatus.AVAILABLE
            DebugLogger.i("CALENDAR", "device events=${list.size}")
        }
    }

    fun updateDeviceCalendarEnabled(enabled: Boolean) {
        AppPrefs.deviceCalendarEnabled = enabled
        deviceCalendarEnabled = enabled
        DebugLogger.i("SETTINGS", "deviceCalendar=$enabled")
        refreshDeviceCalendar()
    }

    /** 권한 요청 결과. 거부해도 로컬 일정은 계속 쓸 수 있다. */
    fun onCalendarPermissionResult(granted: Boolean) {
        AppPrefs.deviceCalendarAsked = true
        DebugLogger.i("CALENDAR", "permission=$granted")
        if (granted) {
            refreshDeviceCalendar()
        } else {
            calendarStatus = CalendarStatus.NO_PERMISSION
            _deviceEvents.value = emptyList()
        }
    }

    fun hasCalendarPermission(): Boolean = deviceCalendar.hasPermission()

    fun addEvent(date: LocalDate, title: String) {
        if (title.isBlank()) return
        _localEvents.value = eventStore.add(date, title)
        DebugLogger.i("EVENT", "added date=$date")
    }

    fun removeEvent(id: String) {
        _localEvents.value = eventStore.remove(id)
        DebugLogger.i("EVENT", "removed")
    }

    fun updateKioskLock(enabled: Boolean) {
        AppPrefs.kioskLock = enabled
        DebugLogger.i("SETTINGS", "kioskLock=$enabled")
    }

    fun updateBootStart(enabled: Boolean) {
        AppPrefs.kioskBootStart = enabled
        kioskBootStart = enabled
        DebugLogger.i("SETTINGS", "kioskBootStart=$enabled")
    }

    fun hasLocationPermission(): Boolean = deviceLocation.hasPermission()

    /** 설정의 `현재 위치 사용` 토글. */
    fun updateLocationEnabled(enabled: Boolean) {
        AppPrefs.locationEnabled = enabled
        locationEnabled = enabled
        DebugLogger.i("SETTINGS", "locationEnabled=$enabled")
        if (enabled) {
            locationStatus = LocationStatus.SEARCHING
            startLocationRefresh(force = true)
        } else {
            locationStatus = LocationStatus.OFF
            locationAccuracy = null
            if (applyFallbackPlace()) refresh()
        }
    }

    /** 권한 요청 결과. 거부하면 도시 폴백을 유지하고 다시 자동 팝업하지 않는다. */
    fun onLocationPermissionResult(granted: Boolean) {
        AppPrefs.locationAsked = true
        DebugLogger.i("LOCATION", "permission=$granted")
        if (granted) {
            locationStatus = LocationStatus.SEARCHING
            startLocationRefresh(force = true)
        } else {
            locationStatus = LocationStatus.NO_PERMISSION
            if (applyFallbackPlace()) refresh()
        }
    }

    /** 설정의 `지금 위치 다시 찾기`. */
    fun refreshLocation() {
        DebugLogger.i("LOCATION", "manual refresh")
        startLocationRefresh(force = true)
    }

    /**
     * 저장된 좌표가 있으면 먼저 쓰고, 하루가 지났거나 [force]면 새 좌표를 요청한다.
     * 새 좌표가 기존과 다르면 날씨를 다시 받는다.
     */
    private fun startLocationRefresh(force: Boolean) {
        if (!AppPrefs.locationEnabled) {
            locationStatus = LocationStatus.OFF
            if (applyFallbackPlace()) refresh()
            return
        }
        if (!deviceLocation.hasPermission()) {
            locationStatus = LocationStatus.NO_PERMISSION
            DebugLogger.i("LOCATION", "permission missing")
            if (applyFallbackPlace()) refresh()
            return
        }

        val cached = deviceLocation.cached()
        if (cached != null) {
            val cachedPlace = Place(
                latitude = cached.latitude,
                longitude = cached.longitude,
                city = null,
                auto = true,
                accuracyMeters = cached.accuracyMeters.takeIf { it > 0f },
            )
            if (!_place.value.sameAs(cachedPlace)) _place.value = cachedPlace
            locationStatus = LocationStatus.READY
            locationAccuracy = cachedPlace.accuracyMeters
            if (!force && cached.ageMillis in 0..DeviceLocation.CACHE_MAX_AGE_MS) return
        } else {
            locationStatus = LocationStatus.SEARCHING
        }

        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val fix = withContext(Dispatchers.IO) { deviceLocation.requestFix(force = force) }
            if (fix != null) {
                AppPrefs.saveLocation(
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracyMeters,
                    timeMs = fix.timeMs,
                )
                val newPlace = Place(
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    city = null,
                    auto = true,
                    accuracyMeters = fix.accuracyMeters.takeIf { it > 0f },
                )
                val moved = !_place.value.sameAs(newPlace)
                _place.value = newPlace
                locationStatus = LocationStatus.READY
                locationAccuracy = newPlace.accuracyMeters
                DebugLogger.i(
                    "LOCATION",
                    "stored auto=true acc=${fix.accuracyMeters.roundToInt()}m moved=$moved",
                )
                if (moved) refresh()
            } else {
                val last = deviceLocation.cached()
                if (last != null) {
                    val lastPlace = Place(
                        latitude = last.latitude,
                        longitude = last.longitude,
                        city = null,
                        auto = true,
                        accuracyMeters = last.accuracyMeters.takeIf { it > 0f },
                    )
                    val moved = !_place.value.sameAs(lastPlace)
                    _place.value = lastPlace
                    locationStatus = LocationStatus.READY
                    locationAccuracy = lastPlace.accuracyMeters
                    DebugLogger.i("LOCATION", "using cached fix after failure")
                    if (moved) refresh()
                } else {
                    locationStatus = if (!deviceLocation.isServiceEnabled()) {
                        LocationStatus.SERVICE_OFF
                    } else {
                        LocationStatus.UNAVAILABLE
                    }
                    DebugLogger.e(
                        ErrorCode.PERM_LOCATION,
                        "no fix status=${locationStatus.name}",
                    )
                    if (applyFallbackPlace()) refresh()
                }
            }
        }
    }

    private fun initialPlace(): Place {
        if (AppPrefs.locationEnabled) {
            val latitude = AppPrefs.cachedLatitude()
            val longitude = AppPrefs.cachedLongitude()
            if (latitude != null && longitude != null) {
                return Place(
                    latitude = latitude,
                    longitude = longitude,
                    city = null,
                    auto = true,
                    accuracyMeters = AppPrefs.cachedAccuracy().takeIf { it > 0f },
                )
            }
        }
        savedPlace()?.let { return it }
        return Place.of(City.fromId(AppPrefs.cityId))
    }

    private fun savedPlace(): Place? {
        val latitude = AppPrefs.savedPlaceLatitude() ?: return null
        val longitude = AppPrefs.savedPlaceLongitude() ?: return null
        val name = AppPrefs.placeName ?: return null
        return Place.saved(latitude, longitude, name)
    }

    /** 현재 위치를 못 쓸 때의 자리. 저장한 동네가 있으면 도시보다 먼저 쓴다. */
    private fun applyFallbackPlace(): Boolean {
        val fallback = savedPlace() ?: Place.of(_city.value)
        locationAccuracy = null
        if (_place.value.sameAs(fallback)) return false
        _place.value = fallback
        return true
    }

    fun updateHour24(enabled: Boolean) {
        AppPrefs.hour24 = enabled
        hour24 = enabled
    }

    fun updateCelsius(enabled: Boolean) {
        AppPrefs.celsius = enabled
        celsius = enabled
        refresh()
    }

    fun updateLocaleTag(tag: String) {
        AppPrefs.localeTag = tag
        localeTag = tag
    }

    fun updateRotationEnabled(enabled: Boolean) {
        AppPrefs.rotationEnabled = enabled
        rotationEnabled = enabled
        DebugLogger.i("SETTINGS", "rotation=$enabled")
    }

    /** [seconds]는 [AppPrefs.DWELL_OPTIONS] 중 하나만 허용한다. */
    fun updateDwell(mode: ScreenMode, seconds: Int) {
        if (seconds !in AppPrefs.DWELL_OPTIONS) return
        when (mode) {
            ScreenMode.CALENDAR -> {
                AppPrefs.dwellCalendar = seconds
                dwellCalendar = seconds
            }
            ScreenMode.WEATHER -> {
                AppPrefs.dwellWeather = seconds
                dwellWeather = seconds
            }
            ScreenMode.CLOCK -> {
                AppPrefs.dwellClock = seconds
                dwellClock = seconds
            }
        }
        DebugLogger.i("SETTINGS", "dwell ${mode.name}=$seconds")
    }

    fun updateTouchStop(enabled: Boolean) {
        AppPrefs.touchStop = enabled
        touchStop = enabled
        DebugLogger.i("SETTINGS", "touchStop=$enabled")
    }

    /** [percent]는 [AppPrefs.TEXT_SCALE_OPTIONS] 중 하나만 허용한다. */
    fun updateTextScale(percent: Int) {
        if (percent !in AppPrefs.TEXT_SCALE_OPTIONS) return
        AppPrefs.textScale = percent
        textScale = percent
        DebugLogger.i("SETTINGS", "textScale=$percent")
    }

    fun updateWeatherMotion(enabled: Boolean) {
        AppPrefs.weatherMotion = enabled
        weatherMotion = enabled
        DebugLogger.i("SETTINGS", "weatherMotion=$enabled")
    }

    fun updateClockFace(face: ClockFace) {
        AppPrefs.clockFace = face.name
        clockFace = face
        DebugLogger.i("SETTINGS", "clockFace=${face.name}")
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1_000L

        /** 타이핑이 멈춘 뒤 검색까지 기다리는 시간. Nominatim 초당 1회 정책을 지킨다. */
        private const val SEARCH_DEBOUNCE_MS = 1_000L

        /** 기기 캘린더를 읽는 범위(오늘 기준 개월). 달력 이동 시 점 표시 범위다. */
        private const val DEVICE_CALENDAR_MONTHS_BACK = 12L
        private const val DEVICE_CALENDAR_MONTHS_FORWARD = 12L
    }
}
