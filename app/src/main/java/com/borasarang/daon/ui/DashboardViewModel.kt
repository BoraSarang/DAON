package com.borasarang.daon.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
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
import com.borasarang.daon.data.event.EventOccurrences
import com.borasarang.daon.data.event.EventStore
import com.borasarang.daon.data.event.RepeatRule
import com.borasarang.daon.data.radio.RadioCatalog
import com.borasarang.daon.data.radio.RadioChannel
import com.borasarang.daon.data.radio.RadioException
import com.borasarang.daon.data.radio.RadioPlayer
import com.borasarang.daon.data.radio.RadioResolver
import com.borasarang.daon.data.tv.TvCatalog
import com.borasarang.daon.data.tv.TvChannel
import com.borasarang.daon.data.tv.TvException
import com.borasarang.daon.data.tv.TvPlayer
import com.borasarang.daon.data.tv.TvResolver
import kotlinx.coroutines.CancellationException
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

/**
 * 라디오 화면 상태.
 *
 * [startedAtMs] 는 [SystemClock.elapsedRealtime] 기준 재생 시작 시각이다.
 * 화면에 `재생 중 · 00:12:34` 처럼 보여 주는 데만 쓴다.
 */
data class RadioUiState(
    val channel: RadioChannel? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val errorRes: Int? = null,
    val startedAtMs: Long = 0L,
)

/**
 * TV 화면 상태.
 *
 * [fullscreen] 은 영상만 전체 화면으로 보는 상태다. 재생 중이면 자동 전환도 멈춘다.
 */
data class TvUiState(
    val channel: TvChannel? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val fullscreen: Boolean = false,
    val errorRes: Int? = null,
    val startedAtMs: Long = 0L,
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

    /** 로컬 일정(반복 펼침) + 기기 캘린더 일정. 날짜·시간 순으로 정렬된다. */
    val events: StateFlow<List<CalendarEvent>> =
        combine(_localEvents, _deviceEvents, _calendar) { local, device, data ->
            val today = LocalDate.now()
            val from = today.minusMonths(EVENT_WINDOW_BACK_MONTHS)
            val to = today.plusMonths(EVENT_WINDOW_FORWARD_MONTHS)
            val expanded = local.flatMap { event ->
                EventOccurrences.expand(event, data, from, to)
            }
            val visibleDevice = device.filter { it.date in from..to }
            (expanded + visibleDevice).sortedWith(EventJson.ORDER)
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

    var radioInRotation by mutableStateOf(AppPrefs.radioInRotation)
        private set
    var dwellRadio by mutableStateOf(AppPrefs.dwellRadio)
        private set
    var radioAutoplay by mutableStateOf(AppPrefs.radioAutoplay)
        private set
    var radioChannelId by mutableStateOf(AppPrefs.radioChannelId)
        private set
    var radioVolume by mutableStateOf(AppPrefs.radioVolume)
        private set

    var tvInRotation by mutableStateOf(AppPrefs.tvInRotation)
        private set
    var dwellTv by mutableStateOf(AppPrefs.dwellTv)
        private set
    var tvChannelId by mutableStateOf(AppPrefs.tvChannelId)
        private set

    private val _radio = MutableStateFlow(RadioUiState())
    val radio: StateFlow<RadioUiState> = _radio.asStateFlow()

    private val _radioChannels = MutableStateFlow<List<RadioChannel>>(emptyList())
    val radioChannels: StateFlow<List<RadioChannel>> = _radioChannels.asStateFlow()

    private val radioResolver = RadioResolver()
    private val radioPlayer = RadioPlayer(app)
    private var radioJob: Job? = null

    private val _tv = MutableStateFlow(TvUiState())
    val tv: StateFlow<TvUiState> = _tv.asStateFlow()

    private val _tvChannels = MutableStateFlow<List<TvChannel>>(emptyList())
    val tvChannels: StateFlow<List<TvChannel>> = _tvChannels.asStateFlow()

    private val tvResolver = TvResolver()
    private val tvPlayer = TvPlayer(app)
    private var tvJob: Job? = null

    /** 화면의 `PlayerView` 에 붙일 플레이어. 첫 재생 전에는 null. */
    var tvPlayerInstance by mutableStateOf<ExoPlayer?>(null)
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
        radioPlayer.onBuffering = {
            _radio.update { it.copy(buffering = true, errorRes = null) }
        }
        radioPlayer.onPlaying = {
            _radio.update {
                it.copy(
                    playing = true,
                    buffering = false,
                    errorRes = null,
                    startedAtMs = if (it.startedAtMs > 0L) it.startedAtMs else SystemClock.elapsedRealtime(),
                )
            }
        }
        radioPlayer.onEnded = {
            _radio.update { it.copy(playing = false, buffering = false, startedAtMs = 0L) }
        }
        radioPlayer.onError = { name ->
            DebugLogger.e(ErrorCode.RADIO_PLAYBACK, "player error: $name")
            _radio.update {
                it.copy(
                    playing = false,
                    buffering = false,
                    errorRes = R.string.error_radio_playback,
                    startedAtMs = 0L,
                )
            }
        }
        tvPlayer.onBuffering = {
            _tv.update { it.copy(buffering = true, errorRes = null) }
        }
        tvPlayer.onPlaying = {
            _tv.update {
                it.copy(
                    playing = true,
                    buffering = false,
                    errorRes = null,
                    startedAtMs = if (it.startedAtMs > 0L) it.startedAtMs else SystemClock.elapsedRealtime(),
                )
            }
        }
        tvPlayer.onEnded = {
            _tv.update { it.copy(playing = false, buffering = false, startedAtMs = 0L) }
        }
        tvPlayer.onError = { name ->
            DebugLogger.e(ErrorCode.TV_PLAYBACK, "player error: $name")
            _tv.update {
                it.copy(
                    playing = false,
                    buffering = false,
                    errorRes = R.string.error_tv_playback,
                    startedAtMs = 0L,
                )
            }
        }
        loadRadioChannels()
        loadTvChannels()
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

    fun addEvent(
        date: LocalDate,
        title: String,
        repeat: RepeatRule = RepeatRule.NONE,
        lunar: Boolean = false,
    ) {
        if (title.isBlank()) return
        // 음력 일정은 그 날의 음력 월·일을 저장해 매년 같은 음력 날짜에 다시 보여 준다.
        val lunarDate = if (lunar) _calendar.value.lunarOn(date) else null
        _localEvents.value = eventStore.add(date, title, repeat, lunarDate)
        DebugLogger.i(
            "EVENT",
            "added date=$date repeat=${repeat.name} lunar=${lunarDate != null}",
        )
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

    /** [seconds]는 화면별 허용값 중 하나만 쓴다. */
    fun updateDwell(mode: ScreenMode, seconds: Int) {
        val allowed = when (mode) {
            ScreenMode.RADIO -> AppPrefs.RADIO_DWELL_OPTIONS
            ScreenMode.TV -> AppPrefs.TV_DWELL_OPTIONS
            else -> AppPrefs.DWELL_OPTIONS
        }
        if (seconds !in allowed) return
        when (mode) {
            ScreenMode.CALENDAR -> {
                AppPrefs.dwellCalendar = seconds
                dwellCalendar = seconds
            }
            ScreenMode.RADIO -> {
                AppPrefs.dwellRadio = seconds
                dwellRadio = seconds
            }
            ScreenMode.TV -> {
                AppPrefs.dwellTv = seconds
                dwellTv = seconds
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

    // ---------- 라디오 ----------

    /** 번들 채널 목록을 읽는다. 실패해도 나머지 화면은 그대로 쓸 수 있다. */
    private fun loadRadioChannels() {
        viewModelScope.launch {
            val channels = withContext(Dispatchers.IO) {
                val text = runCatching {
                    getApplication<Application>().assets
                        .open(RADIO_ASSET)
                        .bufferedReader()
                        .use { it.readText() }
                }.getOrNull()
                RadioCatalog.parse(text)
            }
            _radioChannels.value = channels
            if (channels.isEmpty()) {
                DebugLogger.e(ErrorCode.RADIO_CHANNEL, "radio catalog empty")
                _radio.value = RadioUiState(errorRes = R.string.error_radio_channel)
                return@launch
            }
            val saved = channels.firstOrNull { it.id == AppPrefs.radioChannelId } ?: channels.first()
            _radio.value = RadioUiState(channel = saved)
            DebugLogger.i("RADIO", "channels=${channels.size} default=${saved.id}")
            if (AppPrefs.radioAutoplay) playChannel(saved)
        }
    }

    /** 채널을 고르면 바로 재생한다. 주소는 매번 새로 확인한다(토큰 만료 대응). */
    fun playChannel(channel: RadioChannel) {
        // TV 와 동시에 소리가 나지 않게 먼저 멈춘다.
        if (_tv.value.playing || _tv.value.buffering) stopTv()
        radioChannelId = channel.id
        AppPrefs.radioChannelId = channel.id
        radioJob?.cancel()
        _radio.value = RadioUiState(channel = channel, buffering = true)
        DebugLogger.i("RADIO", "play ${channel.id} type=${channel.type.name}")
        radioJob = viewModelScope.launch {
            try {
                val url = radioResolver.resolveWithRetry(channel)
                radioPlayer.play(url, radioVolume)
            } catch (e: RadioException) {
                DebugLogger.e(e.code, "resolve ${channel.id}: ${e.message}")
                _radio.update {
                    it.copy(
                        buffering = false,
                        playing = false,
                        errorRes = R.string.error_radio_network,
                    )
                }
            } catch (c: CancellationException) {
                // 다른 채널을 눌러 취소된 경우다. 오류가 아니다.
                throw c
            } catch (t: Throwable) {
                DebugLogger.e(
                    ErrorCode.RADIO_RESOLVE,
                    "resolve ${channel.id}: ${t.javaClass.simpleName}",
                )
                _radio.update {
                    it.copy(
                        buffering = false,
                        playing = false,
                        errorRes = R.string.error_radio_network,
                    )
                }
            }
        }
    }

    /** 큰 재생/멈춤 버튼. 재생 중이거나 연결 중이면 멈춘다. */
    fun toggleRadio() {
        val state = _radio.value
        if (state.playing || state.buffering) {
            stopRadio()
        } else {
            state.channel?.let { playChannel(it) }
        }
    }

    fun stopRadio() {
        radioJob?.cancel()
        radioJob = null
        radioPlayer.stop()
        _radio.update { it.copy(playing = false, buffering = false, startedAtMs = 0L) }
        DebugLogger.i("RADIO", "stop")
    }

    /** 오류 뒤 `다시 시도`. 채널 목록이 비었으면 목록부터 다시 읽는다. */
    fun retryRadio() {
        val channel = _radio.value.channel
        if (channel == null) {
            loadRadioChannels()
        } else {
            playChannel(channel)
        }
    }

    fun updateRadioVolume(step: Int) {
        val value = step.coerceIn(0, RadioPlayer.MAX_STEP)
        if (value == radioVolume) return
        radioVolume = value
        AppPrefs.radioVolume = value
        radioPlayer.setVolume(value)
    }

    fun updateRadioInRotation(enabled: Boolean) {
        AppPrefs.radioInRotation = enabled
        radioInRotation = enabled
        DebugLogger.i("SETTINGS", "radioInRotation=$enabled")
    }

    fun updateRadioAutoplay(enabled: Boolean) {
        AppPrefs.radioAutoplay = enabled
        radioAutoplay = enabled
        DebugLogger.i("SETTINGS", "radioAutoplay=$enabled")
    }

    /** 설정의 `기본 채널`. 재생 중이 아니면 지금 보이는 채널도 바꾼다. */
    fun setDefaultRadioChannel(channel: RadioChannel) {
        AppPrefs.radioChannelId = channel.id
        radioChannelId = channel.id
        if (!_radio.value.playing && !_radio.value.buffering) {
            _radio.update { it.copy(channel = channel, errorRes = null) }
        }
        DebugLogger.i("SETTINGS", "radioChannel=${channel.id}")
    }

    override fun onCleared() {
        radioPlayer.release()
        tvPlayer.release()
        super.onCleared()
    }

    // ---------- TV ----------

    /** 번들 TV 채널 목록. 실패해도 나머지 화면은 그대로 쓸 수 있다. */
    private fun loadTvChannels() {
        viewModelScope.launch {
            val channels = withContext(Dispatchers.IO) {
                val text = runCatching {
                    getApplication<Application>().assets
                        .open(TV_ASSET)
                        .bufferedReader()
                        .use { it.readText() }
                }.getOrNull()
                TvCatalog.parse(text)
            }
            _tvChannels.value = channels
            if (channels.isEmpty()) {
                DebugLogger.e(ErrorCode.TV_CHANNEL, "tv catalog empty")
                _tv.value = TvUiState(errorRes = R.string.error_tv_channel)
                return@launch
            }
            val saved = channels.firstOrNull { it.id == AppPrefs.tvChannelId } ?: channels.first()
            _tv.value = TvUiState(channel = saved)
            DebugLogger.i("TV", "channels=${channels.size} default=${saved.id}")
        }
    }

    /** 채널을 고르면 바로 재생한다. 라디오가 나오고 있으면 먼저 멈춘다. */
    fun playTvChannel(channel: TvChannel) {
        if (_radio.value.playing || _radio.value.buffering) stopRadio()
        tvChannelId = channel.id
        AppPrefs.tvChannelId = channel.id
        tvJob?.cancel()
        _tv.update {
            it.copy(channel = channel, buffering = true, errorRes = null, startedAtMs = 0L)
        }
        DebugLogger.i("TV", "play ${channel.id} type=${channel.type.name}")
        tvJob = viewModelScope.launch {
            try {
                val stream = tvResolver.resolveWithRetry(channel)
                tvPlayer.play(stream)
                tvPlayerInstance = tvPlayer.exoPlayer
            } catch (e: TvException) {
                DebugLogger.e(e.code, "resolve ${channel.id}: ${e.message}")
                _tv.update {
                    it.copy(
                        buffering = false,
                        playing = false,
                        errorRes = R.string.error_tv_network,
                    )
                }
            } catch (c: CancellationException) {
                // 다른 채널을 눌러 취소된 경우다. 오류가 아니다.
                throw c
            } catch (t: Throwable) {
                DebugLogger.e(
                    ErrorCode.TV_RESOLVE,
                    "resolve ${channel.id}: ${t.javaClass.simpleName}",
                )
                _tv.update {
                    it.copy(
                        buffering = false,
                        playing = false,
                        errorRes = R.string.error_tv_network,
                    )
                }
            }
        }
    }

    /** 큰 재생/멈춤 버튼. 재생 중이거나 연결 중이면 멈춘다. */
    fun toggleTv() {
        val state = _tv.value
        if (state.playing || state.buffering) {
            stopTv()
        } else {
            state.channel?.let { playTvChannel(it) }
        }
    }

    /** 재생을 멈춘다. 전체화면도 함께 빠져나온다. */
    fun stopTv() {
        val state = _tv.value
        if (tvJob == null && !state.playing && !state.buffering && !state.fullscreen) return
        tvJob?.cancel()
        tvJob = null
        tvPlayer.stop()
        _tv.update {
            it.copy(
                playing = false,
                buffering = false,
                fullscreen = false,
                startedAtMs = 0L,
            )
        }
        DebugLogger.i("TV", "stop")
    }

    /** 오류 뒤 `다시 시도`. 목록이 비었으면 목록부터 다시 읽는다. */
    fun retryTv() {
        val channel = _tv.value.channel
        if (channel == null) {
            loadTvChannels()
        } else {
            playTvChannel(channel)
        }
    }

    /** 전체화면으로 볼지. 재생 중이 아니면 전체화면에 들어가지 않는다. */
    fun setTvFullscreen(enabled: Boolean) {
        val state = _tv.value
        if (enabled && !state.playing && !state.buffering) return
        _tv.update { it.copy(fullscreen = enabled) }
        DebugLogger.i("TV", "fullscreen=$enabled")
    }

    fun updateTvInRotation(enabled: Boolean) {
        AppPrefs.tvInRotation = enabled
        tvInRotation = enabled
        DebugLogger.i("SETTINGS", "tvInRotation=$enabled")
    }

    /** 설정의 `기본 채널`. 재생 중이 아니면 지금 보이는 채널도 바꾼다. */
    fun setDefaultTvChannel(channel: TvChannel) {
        AppPrefs.tvChannelId = channel.id
        tvChannelId = channel.id
        if (!_tv.value.playing && !_tv.value.buffering) {
            _tv.update { it.copy(channel = channel, errorRes = null) }
        }
        DebugLogger.i("SETTINGS", "tvChannel=${channel.id}")
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1_000L
        private const val RADIO_ASSET = "radio_channels.json"
        private const val TV_ASSET = "tv_channels.json"

        /** 타이핑이 멈춘 뒤 검색까지 기다리는 시간. Nominatim 초당 1회 정책을 지킨다. */
        private const val SEARCH_DEBOUNCE_MS = 1_000L

        /** 기기 캘린더를 읽는 범위(오늘 기준 개월). 달력 이동 시 점 표시 범위다. */
        private const val DEVICE_CALENDAR_MONTHS_BACK = 12L
        private const val DEVICE_CALENDAR_MONTHS_FORWARD = 12L

        /** 반복 일정을 미리 펼치는 범위. 달력을 몇 년 넘겨도 점이 보이도록 넉넉히 잡는다. */
        private const val EVENT_WINDOW_BACK_MONTHS = 24L
        private const val EVENT_WINDOW_FORWARD_MONTHS = 120L
    }
}
