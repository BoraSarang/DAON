package com.borasarang.daon.data.tv

import com.borasarang.daon.core.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** TV 주소를 확인하지 못했을 때. [code] 는 [ErrorCode] 의 TV 코드다. */
class TvException(val code: String, message: String) : Exception(message)

/**
 * 확인된 재생 주소.
 *
 * MBC CDN 은 `Referer` 가 없으면 403 을 준다. 방송사별 헤더를 주소와 함께 넘긴다.
 * 유튜브 라이브는 매니페스트 대신 공식 임베드 플레이어로 재생하므로 영상 ID 만 넘긴다.
 */
sealed interface TvStream {

    /** ExoPlayer 로 재생하는 HLS 주소. */
    data class Hls(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
    ) : TvStream

    /** 유튜브 공식 임베드 플레이어로 재생하는 라이브. */
    data class YouTube(val videoId: String) : TvStream
}

/**
 * 방송사별 TV 스트림 주소 확인.
 *
 * 라디오와 마찬가지로 재생할 때마다 새로 확인한다(SBS 는 60초짜리 JWT, MBC 는 `_lsu_sa_` 토큰).
 * KBS 는 라디오와 같은 랜딩 API 를 채널 코드만 바꿔 쓴다.
 */
class TvResolver(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 12_000,
) {

    suspend fun resolve(channel: TvChannel): TvStream = withContext(Dispatchers.IO) {
        when (channel.type) {
            TvSourceType.DIRECT -> TvStream.Hls(
                channel.streamUrl?.takeIf { it.startsWith("http") }
                    ?: throw TvException(ErrorCode.TV_CHANNEL, "no direct stream url: ${channel.id}"),
            )
            TvSourceType.KBS -> TvStream.Hls(parseKbs(fetch(channel, USER_AGENT)))
            TvSourceType.MBC -> TvStream.Hls(parseMbc(fetch(channel, USER_AGENT)), MBC_HEADERS)
            TvSourceType.SBS -> TvStream.Hls(parseSbs(fetch(channel, USER_AGENT)))
            TvSourceType.YOUTUBE -> TvStream.YouTube(
                channel.videoId
                    ?: throw TvException(ErrorCode.TV_CHANNEL, "no video id: ${channel.id}"),
            )
        }
    }

    /** 토큰이 방금 만료됐을 수 있으므로 한 번 더 시도한다. */
    suspend fun resolveWithRetry(channel: TvChannel, attempts: Int = 2): TvStream {
        var last: TvException? = null
        for (attempt in 0 until attempts) {
            try {
                return resolve(channel)
            } catch (e: TvException) {
                last = e
            }
        }
        throw last ?: TvException(ErrorCode.TV_RESOLVE, "resolve failed: ${channel.id}")
    }

    private fun fetch(channel: TvChannel, userAgent: String): String {
        val address = channel.apiUrl?.takeIf { it.startsWith("http") }
            ?: throw TvException(ErrorCode.TV_CHANNEL, "bad api url: ${channel.id}")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(address).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Referer", REFERER)
                setRequestProperty("Origin", ORIGIN)
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw TvException(ErrorCode.TV_RESOLVE, "http $status ${channel.id}")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: TvException) {
            throw e
        } catch (e: IOException) {
            throw TvException(
                ErrorCode.TV_RESOLVE,
                "io ${e.javaClass.simpleName} ${channel.id}",
            )
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 9; KFMAWI) DAON/0.4"
        private const val REFERER = "https://onair.imbc.com/"
        private const val ORIGIN = "https://onair.imbc.com"

        /** MBC CDN 이 Referer 없이는 403 을 준다. 재생 요청에도 같은 헤더를 붙인다. */
        val MBC_HEADERS = mapOf("Referer" to REFERER, "Origin" to ORIGIN)

        /** KBS JSON: `channel_item[0].service_url` */
        fun parseKbs(body: String): String {
            val serviceUrl = runCatching {
                JSONObject(body)
                    .optJSONArray("channel_item")
                    ?.optJSONObject(0)
                    ?.optString("service_url")
            }.getOrNull()
            return serviceUrl?.takeIf { it.startsWith("http") }
                ?: throw TvException(ErrorCode.TV_RESOLVE, "kbs json has no service_url")
        }

        /** MBC JSON: `MediaInfo.MediaURL`. 없으면 실패. */
        fun parseMbc(body: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
                ?: throw TvException(ErrorCode.TV_RESOLVE, "mbc json invalid")
            if (json.optString("Msg") != "OK") {
                throw TvException(ErrorCode.TV_RESOLVE, "mbc msg=${json.optString("Msg")}")
            }
            val url = json.optJSONObject("MediaInfo")
                ?.optString("MediaURL")
                ?.trim()
                ?.let { if (it.startsWith("//")) "https:$it" else it }
                ?.takeIf { it.startsWith("http") }
            return url ?: throw TvException(ErrorCode.TV_RESOLVE, "mbc json has no media url")
        }

        /** SBS JSON: `onair.source.mediasource.mediaurl`, 없으면 목록의 마지막. */
        fun parseSbs(body: String): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
                ?: throw TvException(ErrorCode.TV_RESOLVE, "sbs json invalid")
            val source = json.optJSONObject("onair")?.optJSONObject("source")
            val single = source?.optJSONObject("mediasource")?.optString("mediaurl")
            val list = source?.optJSONArray("mediasourcelist")
            val fromList = if (list != null && list.length() > 0) {
                list.optJSONObject(list.length() - 1)?.optString("mediaurl")
            } else {
                null
            }
            val url = sequenceOf(single, fromList)
                .filterNotNull()
                .map { it.trim() }
                .firstOrNull { it.startsWith("http") }
                ?: throw TvException(ErrorCode.TV_RESOLVE, "sbs json has no media url")
            // SBS 는 http 로 주는 경우가 있다. 기기는 평문 HTTP 를 막으므로 https 로 올린다.
            return if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
        }
    }
}
