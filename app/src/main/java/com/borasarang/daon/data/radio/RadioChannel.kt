package com.borasarang.daon.data.radio

import org.json.JSONArray

/** 방송사별 스트림 주소 확인 방식. 방송사마다 응답 형식이 다르다. */
enum class RadioSourceType {
    /** KBS: JSON `channel_item[0].service_url` */
    KBS,

    /** MBC: 응답 본문이 m3u8 주소 한 줄 (User-Agent 필요) */
    MBC,

    /** SBS: 응답 본문이 m3u8 주소 한 줄 (JWT 포함) */
    SBS,

    /** 고정 m3u8 주소 */
    DIRECT,
    ;

    companion object {
        fun fromName(name: String?): RadioSourceType =
            entries.firstOrNull { it.name == name } ?: DIRECT
    }
}

/**
 * 라디오 채널 한 개.
 *
 * 스트림 주소는 매 재생마다 [RadioResolver] 가 새로 받는다(토큰 만료 대응).
 * [apiUrl] 은 확인용 주소, [streamUrl] 은 만료가 없는 고정 주소다.
 */
data class RadioChannel(
    val id: String,
    val name: String,
    val nameEn: String,
    val freq: String,
    val type: RadioSourceType,
    val apiUrl: String? = null,
    val streamUrl: String? = null,
) {
    /** 화면에 보여 줄 이름. 한국어 UI 에서는 방송사 표기를 그대로 쓴다. */
    fun displayName(korean: Boolean): String = if (korean) name else nameEn
}

/**
 * 번들 채널 목록. 방송사 공식 무료 스트림만 넣는다.
 * 깨진 항목은 조용히 건너뛰고 나머지를 살린다.
 */
object RadioCatalog {

    const val DEFAULT_ID = "kbs_1radio"

    fun parse(text: String?): List<RadioChannel> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(KEY_ID).takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString(KEY_NAME).takeIf { it.isNotBlank() } ?: continue
                    val freq = item.optString(KEY_FREQ).takeIf { it.isNotBlank() } ?: continue
                    val type = RadioSourceType.fromName(item.optString(KEY_TYPE))
                    val apiUrl = item.optString(KEY_API_URL).takeIf { it.startsWith("http") }
                    val streamUrl = item.optString(KEY_STREAM_URL).takeIf { it.startsWith("http") }
                    // 확인 주소가 필요한 방식인데 주소가 없으면 재생할 수 없으므로 뺀다.
                    if (type != RadioSourceType.DIRECT && apiUrl == null) continue
                    if (type == RadioSourceType.DIRECT && streamUrl == null) continue
                    add(
                        RadioChannel(
                            id = id,
                            name = name,
                            nameEn = item.optString(KEY_NAME_EN).takeIf { it.isNotBlank() } ?: name,
                            freq = freq,
                            type = type,
                            apiUrl = apiUrl,
                            streamUrl = streamUrl,
                        ),
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_NAME_EN = "nameEn"
    private const val KEY_FREQ = "freq"
    private const val KEY_TYPE = "type"
    private const val KEY_API_URL = "apiUrl"
    private const val KEY_STREAM_URL = "streamUrl"
}
