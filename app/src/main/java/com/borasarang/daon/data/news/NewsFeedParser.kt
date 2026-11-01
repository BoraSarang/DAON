package com.borasarang.daon.data.news

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * RSS 2.0 피드 파서. 두 출처(연합뉴스·SBS)만 상대하므로 필요한 태그만 읽는다.
 *
 * `XmlPullParser` 대신 문자열 스캐너를 쓰는 이유:
 * - 두 피드 모두 `<item>` 아래 평평한 구조이고 CDATA 로 감싼 값이 전부다.
 * - 단위 테스트(JVM)에서 안드로이드 XML API 스텁을 쓰지 않아도 된다.
 * - 피드에 들어 있는 `&nbsp;` 처럼 XML 에 없는 HTML 엔티티를 직접 다룰 수 있다.
 *
 * 깨진 항목은 조용히 건너뛰고 나머지를 살린다. 0건이면 빈 목록을 돌려주고,
 * 형식 오류 여부는 호출자([NewsRepository])가 판단한다.
 */
object NewsFeedParser {

    /** 제목이 이보다 짧으면 전적 잔여물로 보고 뺀다. */
    const val MIN_TITLE_LENGTH = 12

    private val ITEM = Regex("<item(?:\\s[^>]*)?>(.*?)</item>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val TAG = Regex("<[^>]*>")
    private val SPACE = Regex("[\\s\\u00A0\\u200B]+")
    private val CONTROL = Regex("[\\u0000-\\u001F\\u007F]")
    private val AGGREGATION = Regex("\\s*\\((?:종합[0-9]*보?|[0-9]+보)\\)\\s*$")
    private val BRACKET = Regex("\\[([^\\]]{1,40})\\]")
    private val ENTITY = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});")

    /** 전적·순위·단신·자막은 소식이 아니라 표/영상이라 목록에서 뺀다. */
    private val EXCLUDED_TAGS = listOf("전적", "순위", "단신", "자막")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to "\u00A0", "middot" to "·", "hellip" to "…", "bull" to "•",
        "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
        "ndash" to "–", "mdash" to "—", "prime" to "′", "Prime" to "″",
        "laquo" to "«", "raquo" to "»", "rarr" to "→", "larr" to "←",
        "deg" to "°", "copy" to "©", "reg" to "®", "trade" to "™",
        "times" to "×", "divide" to "÷", "euro" to "€", "pound" to "£",
        "yen" to "¥", "won" to "₩",
    )

    /**
     * 피드 한 개를 읽어 최신순으로 돌려준다.
     * 전적·순위·자막 제목, 12자 미만 제목, 주소·발행 시각이 없는 항목은 뺀다.
     */
    fun parse(source: NewsSource, xml: String): List<NewsItem> {
        if (xml.isBlank()) return emptyList()
        val body = xml.replace("<![CDATA[", "").replace("]]>", "")
        return ITEM.findAll(body)
            .mapNotNull { match -> parseItem(source, match.groupValues[1]) }
            .sortedByDescending { it.publishedAt }
            .toList()
    }

    /**
     * 두 출처를 한 목록으로 합친다. 출처별로 최대 [perSource] 건씩 먼저 담고,
     * 한쪽이 모자라면 다른 쪽으로 [total] 까지 채운다.
     *
     * 같은 제목(공백만 다른 경우 포함)은 최신 발행 시각의 한 건만 남긴다.
     */
    fun merge(
        items: List<NewsItem>,
        perSource: Int = NewsItem.PER_SOURCE,
        total: Int = NewsItem.LIST_SIZE,
    ): List<NewsItem> {
        if (items.isEmpty() || total <= 0) return emptyList()
        val sorted = items.sortedWith(
            compareByDescending<NewsItem> { it.publishedAt }.thenBy { it.title },
        )
        val picked = ArrayList<NewsItem>(total)
        val seen = HashSet<String>()
        val counts = HashMap<NewsSource, Int>()
        val overflow = ArrayList<NewsItem>()

        for (item in sorted) {
            if (picked.size >= total) break
            val key = titleKey(item.title)
            if (key.isEmpty() || key in seen) continue
            val used = counts[item.source] ?: 0
            if (used >= perSource) {
                overflow.add(item)
                continue
            }
            seen.add(key)
            counts[item.source] = used + 1
            picked.add(item)
        }

        for (item in overflow) {
            if (picked.size >= total) break
            val key = titleKey(item.title)
            if (key.isEmpty() || key in seen) continue
            seen.add(key)
            picked.add(item)
        }

        return picked.sortedByDescending { it.publishedAt }
    }

    /** 제목만 비교할 때 쓰는 키. 띄어쓰기 차이만 다른 같은 제목을 합친다. */
    fun titleKey(title: String): String = title.filterNot { it.isWhitespace() }

    /** 태그·엔티티·제어문자를 걷어 낸 한 줄 텍스트. */
    fun cleanText(raw: String): String {
        val once = decodeEntities(TAG.replace(raw, " "))
        return decodeEntities(once)
            .replace(CONTROL, " ")
            .replace(SPACE, " ")
            .trim()
    }

    /**
     * 화면에 보여 줄 제목. 끝의 `(종합)` `(종합2보)` `(1보)` 는 떼어
     * 같은 사건이 두 출처에 중복으로 남지 않게 한다.
     */
    fun cleanTitle(raw: String): String = cleanText(raw).replace(AGGREGATION, "").trim()

    /** 전적·순위·단신·자막 제목인지. `[프로축구 서울전적]`, `[자막뉴스]` 처럼 대괄호 안에 표시가 있다. */
    fun isExcluded(title: String): Boolean =
        BRACKET.findAll(title).any { match ->
            val tag = match.groupValues[1]
            EXCLUDED_TAGS.any { tag.contains(it) }
        }

    private fun parseItem(source: NewsSource, block: String): NewsItem? {
        val title = cleanTitle(tag(block, "title"))
        if (title.length < MIN_TITLE_LENGTH || isExcluded(title)) return null

        val link = cleanText(tag(block, "link"))
        if (!link.startsWith("http://") && !link.startsWith("https://")) return null

        val publishedAt = parseDate(cleanText(tag(block, "pubDate"))) ?: return null
        val summary = cleanText(tag(block, "description")).take(MAX_SUMMARY_LENGTH)

        return NewsItem(
            source = source,
            title = title,
            link = link,
            summary = summary,
            publishedAt = publishedAt,
        )
    }

    private fun tag(block: String, name: String): String {
        val match = Regex(
            "<$name(?:\\s[^>]*)?>(.*?)</$name>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        ).find(block) ?: return ""
        return match.groupValues[1]
    }

    private fun parseDate(raw: String): Instant? {
        if (raw.isBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching {
                java.time.ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            }.getOrNull()
            ?: runCatching {
                java.time.ZonedDateTime.parse(
                    raw,
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                ).toInstant()
            }.getOrNull()
    }

    private fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return ENTITY.replace(text) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                        ?: match.value
                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                        ?: match.value
                else -> NAMED_ENTITIES[body] ?: NAMED_ENTITIES[body.lowercase()] ?: match.value
            }
        }
    }

    /** RSS 요약은 1~2문장이지만, 형식이 깨진 피드가 긴 글을 넣을 수 있어 상한을 둔다. */
    private const val MAX_SUMMARY_LENGTH = 600
}
