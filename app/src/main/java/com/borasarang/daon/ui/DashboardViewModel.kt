package com.borasarang.daon.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.borasarang.daon.R
import com.borasarang.daon.core.AppPrefs
import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import com.borasarang.daon.data.City
import com.borasarang.daon.data.WeatherException
import com.borasarang.daon.data.WeatherRepository
import com.borasarang.daon.data.WeatherSnapshot
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

    var hour24 by mutableStateOf(AppPrefs.hour24)
        private set
    var celsius by mutableStateOf(AppPrefs.celsius)
        private set
    var localeTag by mutableStateOf(AppPrefs.localeTag)
        private set

    private var refreshJob: Job? = null

    init {
        DebugLogger.i("FEATURE", "dashboard")
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
                    DebugLogger.perf("WEATHER", "fetched days=${snapshot.daily.size}")
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
                    _weather.update { it.copy(isLoading = false, errorRes = messageRes) }
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

    companion object {
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1_000L
    }
}
