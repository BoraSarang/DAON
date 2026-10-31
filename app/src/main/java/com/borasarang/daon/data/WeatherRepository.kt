package com.borasarang.daon.data

import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val windSpeed: Double,
)

data class DailyForecast(
    val date: LocalDate,
    val weatherCode: Int,
    val max: Double,
    val min: Double,
)

data class WeatherSnapshot(
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val fetchedAt: LocalDateTime,
)

class WeatherException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Open-Meteo(무키) 클라이언트. 호출은 IO 디스패처에서 수행할 것.
 * 실패 시 [WeatherException] 으로 error_code 를 붙여 던진다.
 */
class WeatherRepository {

    fun fetch(city: City, celsius: Boolean): WeatherSnapshot {
        val url = buildUrl(city, celsius)
        val body = request(url)
        return try {
            parse(body)
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.NET_PARSE, "parse failed: ${t.javaClass.simpleName}")
            throw WeatherException(ErrorCode.NET_PARSE, "weather parse failed", t)
        }
    }

    private fun buildUrl(city: City, celsius: Boolean): String {
        val unit = if (celsius) "celsius" else "fahrenheit"
        return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.latitude}" +
            "&longitude=${city.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=4&wind_speed_unit=ms" +
            "&temperature_unit=$unit"
    }

    private fun request(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                DebugLogger.e(ErrorCode.NET_HTTP, "http_status=$status")
                throw WeatherException(ErrorCode.NET_HTTP, "http status $status")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: WeatherException) {
            throw e
        } catch (e: SocketTimeoutException) {
            DebugLogger.e(ErrorCode.NET_TIMEOUT, "timeout")
            throw WeatherException(ErrorCode.NET_TIMEOUT, "timeout", e)
        } catch (e: IOException) {
            DebugLogger.e(ErrorCode.NET_HTTP, "io: ${e.javaClass.simpleName}")
            throw WeatherException(ErrorCode.NET_HTTP, "network error", e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(body: String): WeatherSnapshot {
        val root = JSONObject(body)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")

        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")

        val forecast = (0 until minOf(dates.length(), codes.length(), maxes.length(), mins.length()))
            .map { i ->
                DailyForecast(
                    date = LocalDate.parse(dates.getString(i)),
                    weatherCode = codes.getInt(i),
                    max = maxes.getDouble(i),
                    min = mins.getDouble(i),
                )
            }

        return WeatherSnapshot(
            current = CurrentWeather(
                temperature = current.getDouble("temperature_2m"),
                feelsLike = current.getDouble("apparent_temperature"),
                humidity = current.getInt("relative_humidity_2m"),
                weatherCode = current.getInt("weather_code"),
                isDay = current.getInt("is_day") == 1,
                windSpeed = current.getDouble("wind_speed_10m"),
            ),
            daily = forecast,
            fetchedAt = LocalDateTime.now(),
        )
    }
}
