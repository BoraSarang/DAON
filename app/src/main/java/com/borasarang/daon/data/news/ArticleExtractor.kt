package com.borasarang.daon.data.news

import org.json.JSONArray
import org.json.JSONObject

/**
 * 기사 본문 추출. 원문 페이지를 앱 안에서 글자만 뽑는다.
 * WebView·브라우저를 쓰지 않으므로 화면 고정(키오스크)이 유지된다.
 *
 * 두 곳의 방식이 다르다.
 * - 연합뉴스: `<article id="articleWrap">` 안에서 `<figure>`(사진·기자 사진)를 먼저 지우고 `<p>` 만 모은다.
 * - SBS: JSON-LD `@graph[0].articleBody` 한 덩어리를 문장 단위로 끊어 문단으로 만든다.
 *
 * 실패하면 오류를 내지 않고 RSS 요약으로 조용히 대체한다([ArticleContent.usedSummary]).
 */
object ArticleExtractor {

    /** 본문 문단과, 요약으로 대체했는지 여부. */
    data class ArticleContent(
        val paragraphs: List<String>,
        val usedSummary: Boolean = false,
    ) {
        val isEmpty: Boolean get() = paragraphs.isEmpty()
    }

    private val REGEX_OPTIONS = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)

    private val FIGURE = Regex("<figure\\b[^>]*>.*?</figure>", REGEX_OPTIONS)
    private val SCRIPT = Regex("<script\\b[^>]*>.*?</script>", REGEX_OPTIONS)
    private val STYLE = Regex("<style\\b[^>]*>.*?</style>", REGEX_OPTIONS)
    private val PARAGRAPH = Regex("<p(\\s[^>]*)?>(.*?)</p>", REGEX_OPTIONS)
    private val JSON_LD = Regex(
        "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
        REGEX_OPTIONS,
    )
    private val CLASS_ATTR = Regex("class\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
    private val SENTENCE_END = Regex("(?<=[.!?])\\s+")
    private val EMAIL = Regex("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")
    private val BR = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    /** `<p>` 중에서도 사진 설명·저작권·기자 메일은 본문이 아니다. */
    private val SKIP_CLASSES = listOf(
        "txt-desc", "txt-link", "txt-copyright", "adrs", "caption", "byline", "img-", "txt-caption",
    )

    /** 한 문단에 모을 문장 수. 600dp 패널에서 27px 로 읽기 좋은 길이. */
    private const val SENTENCES_PER_PARAGRAPH = 2
    private const val MAX_PARAGRAPH_CHARS = 260

    fun extract(source: NewsSource, html: String, summary: String): ArticleContent {
        val paragraphs = when (source) {
            NewsSource.YONHAP -> yonhap(html)
            NewsSource.SBS -> sbs(html)
        }.filter { it.length >= MIN_PARAGRAPH_CHARS }

        if (paragraphs.isNotEmpty()) return ArticleContent(paragraphs)

        val fallback = NewsFeedParser.cleanText(summary)
        return if (fallback.isBlank()) {
            ArticleContent(emptyList(), usedSummary = true)
        } else {
            ArticleContent(listOf(fallback), usedSummary = true)
        }
    }

    /** 연합뉴스: 기사 범위를 잡고 사진 블록을 지운 뒤 `<p>` 만 남긴다. */
    private fun yonhap(html: String): List<String> {
        val marker = html.indexOf("id=\"articleWrap\"").takeIf { it >= 0 }
            ?: html.indexOf("story-news article").takeIf { it >= 0 }
            ?: return emptyList()
        val start = html.lastIndexOf("<article", marker).takeIf { it >= 0 }
            ?: html.lastIndexOf("<div", marker).takeIf { it >= 0 }
            ?: marker
        val end = listOf(
            "<div class=\"related-zone",
            "<div class=\"keyword-zone",
            "</article>",
        ).map { html.indexOf(it, start) }
            .filter { it > start }
            .minOrNull()
            ?: (start + MAX_SCOPE_CHARS).coerceAtMost(html.length)

        val block = STYLE.replace(SCRIPT.replace(FIGURE.replace(html.substring(start, end), " "), " "), " ")
        return PARAGRAPH.findAll(block).mapNotNull { match ->
            val attributes = match.groupValues[1]
            val classes = CLASS_ATTR.find(attributes)?.groupValues?.get(1)?.lowercase().orEmpty()
            if (SKIP_CLASSES.any { classes.contains(it) }) return@mapNotNull null

            val text = NewsFeedParser.cleanText(match.groupValues[2])
            when {
                text.isBlank() -> null
                text == "(끝)" -> null
                isByline(text) -> null
                isPhotoCredit(text) -> null
                else -> text
            }
        }.toList()
    }

    /** SBS: JSON-LD 본문을 우선 쓰고, 없으면 본문 영역의 `<br>` 줄을 쓴다. */
    private fun sbs(html: String): List<String> {
        jsonLdArticleBody(html)?.let { body ->
            val sentences = splitSentences(body)
                .filterNot { isCaption(it) }
                .map { NewsFeedParser.cleanText(it) }
                .filter { it.length >= MIN_SENTENCE_CHARS }
            val paragraphs = groupSentences(sentences)
            if (paragraphs.isNotEmpty()) return paragraphs
        }

        val marker = html.indexOf("itemprop=\"articleBody\"").takeIf { it >= 0 }
            ?: html.indexOf("class=\"text_area\"").takeIf { it >= 0 }
            ?: return emptyList()
        val start = html.lastIndexOf("<div", marker).takeIf { it >= 0 } ?: marker
        val end = listOf("copyrightsbs", "article_relation_area", "w_article_side")
            .map { html.indexOf(it, start) }
            .filter { it > start }
            .minOrNull()
            ?: (start + MAX_SCOPE_CHARS).coerceAtMost(html.length)

        return BR.split(html.substring(start, end))
            .map { NewsFeedParser.cleanText(it) }
            .filter { it.length >= MIN_PARAGRAPH_CHARS }
            .filterNot { isCaption(it) || isNoise(it) }
    }

    /** JSON-LD 안의 `articleBody` 를 찾는다. 형식이 깨졌으면 null. */
    private fun jsonLdArticleBody(html: String): String? {
        for (match in JSON_LD.findAll(html)) {
            val body = runCatching {
                val root = JSONObject(match.groupValues[1].trim())
                bodyOf(root) ?: bodyOfGraph(root)
            }.getOrNull()
            if (!body.isNullOrBlank()) return body
        }
        return null
    }

    private fun bodyOf(json: JSONObject): String? =
        json.optString("articleBody").takeIf { it.isNotBlank() }

    private fun bodyOfGraph(json: JSONObject): String? {
        val graph = json.opt("@graph") ?: return null
        val array = graph as? JSONArray ?: return null
        for (index in 0 until array.length()) {
            val node = array.optJSONObject(index) ?: continue
            val body = node.optString("articleBody")
            if (body.isNotBlank()) return body
        }
        return null
    }

    /** `다.` `요.` `까?` 같은 문장 끝 뒤 공백에서 끊는다. */
    internal fun splitSentences(text: String): List<String> =
        text.replace(BR, " ")
            .split(SENTENCE_END)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** 문장을 두 개씩 묶어 문단으로 만든다. 마지막 한 문장은 앞 문단에 붙인다. */
    internal fun groupSentences(sentences: List<String>): List<String> {
        if (sentences.isEmpty()) return emptyList()
        val paragraphs = ArrayList<String>()
        val sentenceCounts = ArrayList<Int>()
        val buffer = StringBuilder()
        var count = 0

        fun flush() {
            if (buffer.isEmpty()) return
            paragraphs.add(buffer.toString().trim())
            sentenceCounts.add(count)
            buffer.setLength(0)
            count = 0
        }

        for (sentence in sentences) {
            if (buffer.isNotEmpty()) buffer.append(' ')
            buffer.append(sentence)
            count++
            if (count >= SENTENCES_PER_PARAGRAPH || buffer.length >= MAX_PARAGRAPH_CHARS) flush()
        }
        flush()

        if (paragraphs.size >= 2 && sentenceCounts.last() == 1 &&
            paragraphs[paragraphs.size - 2].length + paragraphs.last().length <= MAX_PARAGRAPH_CHARS
        ) {
            val tail = paragraphs.removeAt(paragraphs.size - 1)
            paragraphs[paragraphs.size - 1] = "${paragraphs.last()} $tail"
        }
        return paragraphs
    }

    private fun isByline(text: String): Boolean =
        EMAIL.matches(text) || (text.count { it == '@' } == 1 && ' ' !in text)

    /** `▲ …`, `(사진=…)`, `[사진] …` 같은 사진 설명. */
    private fun isCaption(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith("▲") ||
            trimmed.startsWith("(사진") ||
            trimmed.startsWith("[사진") ||
            trimmed.startsWith("사진=")
    }

    /** 사진 제공 표기: `[한국프로축구연맹 제공. 재판매 및 DB 금지]` */
    private fun isPhotoCredit(text: String): Boolean =
        text.startsWith("[") && text.endsWith("]") &&
            (text.contains("제공") || text.contains("사진") || text.contains("자료"))

    private fun isNoise(text: String): Boolean =
        text.contains("영문 기사 보기") ||
            text.contains("무단 전재") ||
            text.contains("재배포 금지") ||
            text.contains("저작권자")

    private const val MIN_PARAGRAPH_CHARS = 10
    private const val MIN_SENTENCE_CHARS = 2
    private const val MAX_SCOPE_CHARS = 60_000
}
