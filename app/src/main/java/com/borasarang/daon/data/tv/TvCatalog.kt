package com.borasarang.daon.data.tv

import org.json.JSONArray

/** TV 스트림을 어떤 방식으로 확인하는지. */
enum class TvSourceType { KBS, MBC, SBS, DIRECT }

/** 채널 목록 한 줄. 방송사 공식 무료 스트림만 넣는다. */
data class TvChannel(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: String,
    val type: TvSourceType,
    val apiUrl: String? = null,
    val streamUrl: String? = null,
) {
    fun displayName(korean: Boolean): String = if (korean) name else nameEn
}

/**
 * `assets/tv_channels.json` 파서.
 *
 * 깨진 항목은 조용히 건너뛴다. 목록이 비면 화면에서 `채널 정보를 찾지 못했습니다` 로 안내한다.
 */
object TvCatalog {

    fun parse(text: String?): List<TvChannel> {
        if (text.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        val channels = ArrayList<TvChannel>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            if (id.isEmpty() || name.isEmpty()) continue
            val type = runCatching {
                TvSourceType.valueOf(item.optString("type").trim().uppercase())
            }.getOrDefault(TvSourceType.DIRECT)
            val apiUrl = item.optString("apiUrl").trim().takeIf { it.startsWith("http") }
            val streamUrl = item.optString("streamUrl").trim().takeIf { it.startsWith("http") }
            if (type == TvSourceType.DIRECT && streamUrl == null) continue
            if (type != TvSourceType.DIRECT && apiUrl == null) continue
            channels += TvChannel(
                id = id,
                name = name,
                nameEn = item.optString("nameEn").trim().ifEmpty { name },
                category = item.optString("category").trim(),
                type = type,
                apiUrl = apiUrl,
                streamUrl = streamUrl,
            )
        }
        return channels
    }
}
