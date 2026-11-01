package com.borasarang.daon.data.tv

import org.json.JSONArray

/**
 * TV 스트림을 어떤 방식으로 확인하는지.
 *
 * [YOUTUBE] 는 방송사 공식 유튜브 라이브다. 유튜브는 HLS/DASH 매니페스트를 밖으로 주지 않으므로
 * ExoPlayer 대신 유튜브 공식 임베드 플레이어(`yt_player.html`)로 재생한다.
 */
enum class TvSourceType { KBS, MBC, SBS, DIRECT, YOUTUBE }

/** 채널 목록 한 줄. 방송사 공식 무료 스트림만 넣는다. */
data class TvChannel(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: String,
    val type: TvSourceType,
    val apiUrl: String? = null,
    val streamUrl: String? = null,
    val videoId: String? = null,
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
            val videoId = videoIdFrom(item.optString("videoId").trim())
                ?: videoIdFrom(item.optString("streamUrl").trim())
            if (type == TvSourceType.DIRECT && streamUrl == null) continue
            if (type == TvSourceType.YOUTUBE && videoId == null) continue
            if (type != TvSourceType.DIRECT && type != TvSourceType.YOUTUBE && apiUrl == null) continue
            channels += TvChannel(
                id = id,
                name = name,
                nameEn = item.optString("nameEn").trim().ifEmpty { name },
                category = item.optString("category").trim(),
                type = type,
                apiUrl = apiUrl,
                streamUrl = streamUrl,
                videoId = videoId,
            )
        }
        return channels
    }

    /**
     * 유튜브 영상 ID 11자.
     *
     * `videoId` 칸에는 ID 만, 예전 형식과의 호환을 위해 `streamUrl` 에는
     * `watch?v=`, `youtu.be/`, `/embed/`, `/live/` 주소를 넣어도 받아 준다.
     */
    fun videoIdFrom(raw: String): String? {
        if (raw.isEmpty()) return null
        if (VIDEO_ID.matches(raw)) return raw
        val marker = listOf("v=", "youtu.be/", "/embed/", "/live/", "/shorts/")
            .firstOrNull { raw.contains(it) } ?: return null
        val tail = raw.substringAfter(marker)
        val candidate = tail.substringBefore('&').substringBefore('?').substringBefore('/')
        return candidate.takeIf { VIDEO_ID.matches(it) }
    }

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
}
