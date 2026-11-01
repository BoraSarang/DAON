package com.borasarang.daon.data.news

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * 뉴스 캐시를 SharedPreferences 한 키에 넣기 위한 JSON 변환.
 * 출처별 최대 30건(합계 60건)이라 JSON 25~35KB 로 끝난다.
 *
 * 깨진 항목은 조용히 건너뛰고 나머지를 살린다. 본문은 저장하지 않는다.
 */
object NewsJson {

    private const val KEY_SOURCE = "s"
    private const val KEY_TITLE = "t"
    private const val KEY_LINK = "l"
    private const val KEY_SUMMARY = "m"
    private const val KEY_PUBLISHED = "p"

    fun encode(items: List<NewsItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put(KEY_SOURCE, item.source.id)
                    put(KEY_TITLE, item.title)
                    put(KEY_LINK, item.link)
                    put(KEY_SUMMARY, item.summary)
                    put(KEY_PUBLISHED, item.publishedAt.toEpochMilli())
                },
            )
        }
        return array.toString()
    }

    fun decode(text: String?): List<NewsItem> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val source = NewsSource.fromId(json.optString(KEY_SOURCE)) ?: continue
                    val title = json.optString(KEY_TITLE).trim()
                    val link = json.optString(KEY_LINK).trim()
                    val published = json.optLong(KEY_PUBLISHED, 0L)
                    if (title.isEmpty() || link.isEmpty() || published <= 0L) continue
                    add(
                        NewsItem(
                            source = source,
                            title = title,
                            link = link,
                            summary = json.optString(KEY_SUMMARY),
                            publishedAt = Instant.ofEpochMilli(published),
                        ),
                    )
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
