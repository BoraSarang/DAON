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
    val precipitationProbability: Int,
    val precipitationSum: Double,
    val windMax: Double,
)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val weatherCode: Int,
    val precipitationProbability: Int,
    val precipitation: Double,
    val isDay: Boolean,
)

data class WeatherSnapshot(
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val hourly: List<HourlyForecast>,
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

    fun fetch(latitude: Double, longitude: Double, celsius: Boolean): WeatherSnapshot {
        val url = buildUrl(latitude, longitude, celsius)
        val body = request(url)
        return try {
            parse(body)
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.NET_PARSE, "parse failed: ${t.javaClass.simpleName}")
            throw WeatherException(ErrorCode.NET_PARSE, "weather parse failed", t)
        }
    }

    private fun buildUrl(latitude: Double, longitude: Double, celsius: Boolean): String {
        val unit = if (celsius) "celsius" else "fahrenheit"
        return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude" +
            "&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,precipitation,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max" +
            "&timezone=auto&forecast_days=7&wind_speed_unit=ms" +
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
        val precipProbs = daily.optJSONArray("precipitation_probability_max")
        val precipSums = daily.optJSONArray("precipitation_sum")
        val windMaxes = daily.optJSONArray("wind_speed_10m_max")

        val forecast = (0 until minOf(dates.length(), codes.length(), maxes.length(), mins.length()))
            .map { i ->
                DailyForecast(
                    date = LocalDate.parse(dates.getString(i)),
                    weatherCode = codes.getInt(i),
                    max = maxes.getDouble(i),
                    min = mins.getDouble(i),
                    precipitationProbability = precipProbs?.intAt(i) ?: 0,
                    precipitationSum = precipSums?.doubleAt(i) ?: 0.0,
                    windMax = windMaxes?.doubleAt(i) ?: 0.0,
                )
            }

        val hourly = root.optJSONObject("hourly")?.let { h ->
            val times = h.optJSONArray("time") ?: return@let emptyList<HourlyForecast>()
            val temperatures = h.optJSONArray("temperature_2m") ?: return@let emptyList<HourlyForecast>()
            val hourCodes = h.optJSONArray("weather_code") ?: return@let emptyList<HourlyForecast>()
            val hourProbs = h.optJSONArray("precipitation_probability")
            val hourPrecips = h.optJSONArray("precipitation")
            val hourIsDay = h.optJSONArray("is_day")
            val start = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
            (0 until minOf(times.length(), temperatures.length(), hourCodes.length()))
                .mapNotNull { i ->
                    val time = runCatching { LocalDateTime.parse(times.getString(i)) }.getOrNull()
                        ?: return@mapNotNull null
                    if (time.isBefore(start) || time.hour % 3 != 0) return@mapNotNull null
                    HourlyForecast(
                        time = time,
                        temperature = temperatures.doubleAt(i),
                        weatherCode = hourCodes.intAt(i),
                        precipitationProbability = hourProbs?.intAt(i) ?: 0,
                        precipitation = hourPrecips?.doubleAt(i) ?: 0.0,
                        isDay = hourIsDay?.optInt(i, 1) != 0,
                    )
                }
                .take(8)
        } ?: emptyList()

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
            hourly = hourly,
            fetchedAt = LocalDateTime.now(),
        )
    }

    private fun org.json.JSONArray.intAt(index: Int): Int =
        if (isNull(index)) 0 else optInt(index, 0)

    private fun org.json.JSONArray.doubleAt(index: Int): Double =
        if (isNull(index)) 0.0 else optDouble(index, 0.0)
}
