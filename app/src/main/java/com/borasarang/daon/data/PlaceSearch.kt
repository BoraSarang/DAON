package com.borasarang.daon.data

import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

/** 검색 결과 한 건. [name] 은 "살미면", [detail] 은 "충주시, 충청북도, 대한민국". */
data class PlaceResult(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** 저장할 때 쓰는 표시 이름. 예: "살미면, 충주시". */
    val label: String
        get() {
            val area = detail.substringBefore(',').trim()
            return if (area.isBlank()) name else "$name, $area"
        }
}

/**
 * OpenStreetMap Nominatim 으로 동네를 찾는다. API 키가 필요 없다.
 *
 * Fire 태블릿은 GPS 가 없고 네트워크 위치(HERE)도 좌표를 주지 않아,
 * 사용자가 설정에서 한 번 검색해 좌표를 저장하는 방식의 백엔드다.
 * Nominatim 이용 정책상 **사용자가 누를 때만** 호출한다(초당 1회 이하).
 */
class PlaceSearch {

    fun search(query: String): List<PlaceResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val body = request(buildUrl(trimmed))
        return try {
            parseResults(body)
        } catch (t: Throwable) {
            DebugLogger.e(ErrorCode.NET_PARSE, "place parse failed: ${t.javaClass.simpleName}")
            throw WeatherException(ErrorCode.NET_PARSE, "place parse failed", t)
        }
    }

    private fun buildUrl(query: String): String =
        "https://nominatim.openstreetmap.org/search" +
            "?q=${URLEncoder.encode(query, "UTF-8")}" +
            "&format=jsonv2" +
            "&limit=5" +
            "&accept-language=ko" +
            "&countrycodes=kr"

    private fun request(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            // Nominatim 은 기본 User-Agent 를 막는다. 앱 이름과 버전을 밝힌다.
            setRequestProperty("User-Agent", "DAON-kiosk/0.2 (Android tablet)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                DebugLogger.e(ErrorCode.NET_HTTP, "place http_status=$code")
                throw WeatherException(ErrorCode.NET_HTTP, "place http $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (t: SocketTimeoutException) {
            DebugLogger.e(ErrorCode.NET_TIMEOUT, "place timeout")
            throw WeatherException(ErrorCode.NET_TIMEOUT, "place timeout", t)
        } catch (t: WeatherException) {
            throw t
        } catch (t: IOException) {
            DebugLogger.e(ErrorCode.NET_HTTP, "place io: ${t.javaClass.simpleName}")
            throw WeatherException(ErrorCode.NET_HTTP, "place io", t)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** JSON 배열을 [PlaceResult] 로 바꾼다. 좌표가 없는 항목은 버린다. */
        fun parseResults(body: String): List<PlaceResult> {
            val array = JSONArray(body)
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val latitude = item.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
                val longitude = item.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
                val display = item.optString("display_name").trim()
                val name = item.optString("name").trim().ifBlank {
                    display.substringBefore(',').trim()
                }
                if (name.isBlank()) return@mapNotNull null
                PlaceResult(
                    name = name,
                    detail = detailOf(display, name),
                    latitude = latitude,
                    longitude = longitude,
                )
            }
        }

        /** "살미면, 충주시, 충청북도, 27493, 대한민국" → "충주시, 충청북도, 대한민국". */
        private fun detailOf(display: String, name: String): String {
            val parts = display.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val rest = if (parts.firstOrNull() == name) parts.drop(1) else parts
            // 우편번호처럼 숫자만 있는 조각은 읽기 어려워 뺀다.
            return rest.filterNot { part -> part.all { it.isDigit() } }
                .take(3)
                .joinToString(", ")
        }
    }
}
