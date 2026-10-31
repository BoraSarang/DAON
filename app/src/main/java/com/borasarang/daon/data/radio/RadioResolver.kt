package com.borasarang.daon.data.radio

import com.borasarang.daon.core.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 라디오 스트림 주소를 확인하지 못했을 때. [code] 는 [ErrorCode] 의 라디오 코드다. */
class RadioException(val code: String, message: String) : Exception(message)

/**
 * 방송사별 스트림 주소 확인.
 *
 * 방송사는 재생할 때마다 새 주소를 준다(CloudFront 서명·JWT·`_lsu_sa_` 토큰).
 * 그래서 주소를 저장하지 않고 매 재생마다 [resolve] 한다.
 */
class RadioResolver(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 12_000,
) {

    /** 채널의 지금 재생 가능한 m3u8 주소를 돌려준다. */
    suspend fun resolve(channel: RadioChannel): String = withContext(Dispatchers.IO) {
        when (channel.type) {
            RadioSourceType.DIRECT -> channel.streamUrl?.takeIf { it.startsWith("http") }
                ?: throw RadioException(
                    ErrorCode.RADIO_CHANNEL,
                    "no direct stream url: ${channel.id}",
                )
            RadioSourceType.KBS -> parseKbs(fetch(channel, userAgent = null))
            RadioSourceType.MBC -> parsePlainText(fetch(channel, userAgent = USER_AGENT))
            RadioSourceType.SBS -> parsePlainText(fetch(channel, userAgent = null))
        }
    }

    /** 토큰이 방금 만료됐을 수 있으므로 한 번 더 시도한다. */
    suspend fun resolveWithRetry(channel: RadioChannel, attempts: Int = 2): String {
        var last: RadioException? = null
        for (attempt in 0 until attempts) {
            try {
                return resolve(channel)
            } catch (e: RadioException) {
                last = e
            }
        }
        throw last ?: RadioException(ErrorCode.RADIO_RESOLVE, "resolve failed: ${channel.id}")
    }

    private fun fetch(channel: RadioChannel, userAgent: String?): String {
        val address = channel.apiUrl?.takeIf { it.startsWith("http") }
            ?: throw RadioException(ErrorCode.RADIO_CHANNEL, "bad api url: ${channel.id}")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(address).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                if (userAgent != null) setRequestProperty("User-Agent", userAgent)
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw RadioException(ErrorCode.RADIO_RESOLVE, "http $status ${channel.id}")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: RadioException) {
            throw e
        } catch (e: IOException) {
            throw RadioException(
                ErrorCode.RADIO_RESOLVE,
                "io ${e.javaClass.simpleName} ${channel.id}",
            )
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) DAON/0.3"

        /** KBS JSON: `channel_item[0].service_url` */
        fun parseKbs(body: String): String {
            val serviceUrl = runCatching {
                JSONObject(body)
                    .optJSONArray("channel_item")
                    ?.optJSONObject(0)
                    ?.optString("service_url")
            }.getOrNull()
            return serviceUrl?.takeIf { it.startsWith("http") }
                ?: throw RadioException(ErrorCode.RADIO_RESOLVE, "kbs json has no service_url")
        }

        /** MBC·SBS: 응답 본문이 m3u8 주소 한 줄이다. 앞뒤 공백·BOM 을 걷어낸다. */
        fun parsePlainText(body: String): String {
            val address = body.lineSequence()
                .map { it.removePrefix("\uFEFF").trim() }
                .firstOrNull { it.startsWith("http") }
            return address
                ?: throw RadioException(ErrorCode.RADIO_RESOLVE, "plain text has no url")
        }
    }
}
