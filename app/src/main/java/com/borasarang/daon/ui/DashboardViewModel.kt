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
import com.borasarang.daon.data.WeatherException
import com.borasarang.daon.data.WeatherRepository
import com.borasarang.daon.data.WeatherSnapshot
import com.borasarang.daon.data.calendar.CalendarData
import com.borasarang.daon.data.calendar.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val isLoading: Boolean = false,
    val errorRes: Int? = null,
    val isOffline: Boolean = false,
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WeatherRepository()

    /** 1초 주기 시계. 화면이 없을 때는 5초 후 정지한다. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LocalDateTime.now())

    private val _weather = MutableStateFlow(WeatherUiState(isLoading = true))
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _city = MutableStateFlow(City.fromId(AppPrefs.cityId))
    val city: StateFlow<City> = _city.asStateFlow()

    /** 번들 달력 자산. 파싱 전에는 비어 있고, 준비되면 음력·절기·공휴일이 나타난다. */
    private val _calendar = MutableStateFlow(CalendarData.empty())
    val calendar: StateFlow<CalendarData> = _calendar.asStateFlow()

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

    init {
        DebugLogger.i("FEATURE", "dashboard")
        viewModelScope.launch(Dispatchers.IO) {
            _calendar.value = CalendarRepository.load(getApplication())
        }
        refresh()
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
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.fetch(_city.value, celsius) }
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
        DebugLogger.i("SETTINGS", "city=${city.id}")
        refresh()
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
    }
}
