package com.borasarang.daon.data.news

import com.borasarang.daon.core.DebugLogger
import com.borasarang.daon.core.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale

class NewsException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 연합뉴스·SBS RSS 와 기사 본문을 받는다. 호출은 IO 디스패처에서 수행할 것.
 *
 * 두 피드는 **병렬**로 받는다. 직렬로 받으면 대기 시간이 두 배가 된다.
 * 한 출처가 실패해도 다른 출처는 그대로 살린다([FeedResult.error]).
 */
class NewsRepository {

    data class FeedResult(
        val source: NewsSource,
        val items: List<NewsItem>,
        val error: Throwable? = null,
    )

    suspend fun fetchAll(): List<FeedResult> = coroutineScope {
        NewsSource.entries.map { source ->
            async(Dispatchers.IO) {
                try {
                    FeedResult(source = source, items = fetchFeed(source))
                } catch (t: Throwable) {
                    DebugLogger.e(
                        (t as? NewsException)?.code ?: ErrorCode.NEWS_FEED,
                        "feed ${source.id} failed: ${t.message ?: t.javaClass.simpleName}",
                    )
                    FeedResult(source = source, items = emptyList(), error = t)
                }
            }
        }.awaitAll()
    }

    fun fetchFeed(source: NewsSource): List<NewsItem> {
        val body = request(source.feedUrl, FEED_ACCEPT, MAX_FEED_BYTES)
        val items = NewsFeedParser.parse(source, body)
        if (items.isEmpty()) {
            DebugLogger.e(ErrorCode.NEWS_FORMAT, "feed ${source.id} parsed 0 items")
            throw NewsException(ErrorCode.NEWS_FORMAT, "feed ${source.id} has no usable items")
        }
        return items
    }

    /**
     * 기사 본문을 받아 글자만 뽑는다. 어떤 이유로 실패하든 오류를 던지지 않고
     * RSS 요약으로 대체한다([ArticleExtractor.ArticleContent.usedSummary]).
     */
    fun fetchArticle(item: NewsItem): ArticleExtractor.ArticleContent =
        try {
            val html = request(item.link, ARTICLE_ACCEPT, MAX_ARTICLE_BYTES)
            ArticleExtractor.extract(item.source, html, item.summary)
        } catch (t: Throwable) {
            DebugLogger.e(
                ErrorCode.NEWS_ARTICLE,
                "article ${item.source.id} failed: ${t.message ?: t.javaClass.simpleName}",
            )
            ArticleExtractor.extract(item.source, "", item.summary)
        }

    private fun request(url: String, accept: String, limit: Int): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", accept)
                setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.5")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw NewsException(ErrorCode.NEWS_FEED, "http status $status")
            }
            val charset = charsetOf(connection.contentType)
            connection.inputStream.bufferedReader(charset).use { reader ->
                val buffer = CharArray(8 * 1024)
                val out = StringBuilder()
                while (out.length < limit) {
                    val read = reader.read(buffer, 0, minOf(buffer.size, limit - out.length))
                    if (read <= 0) break
                    out.append(buffer, 0, read)
                }
                return out.toString()
            }
        } catch (e: NewsException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw NewsException(ErrorCode.NEWS_FEED, "timeout", e)
        } catch (e: IOException) {
            throw NewsException(ErrorCode.NEWS_FEED, "io: ${e.javaClass.simpleName}", e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun charsetOf(contentType: String?): Charset {
        val name = contentType
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', '\'', ' ')
            ?: return Charsets.UTF_8
        return runCatching { Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 12_000
        const val MAX_FEED_BYTES = 512 * 1024
        const val MAX_ARTICLE_BYTES = 800 * 1024

        const val FEED_ACCEPT = "application/rss+xml, application/xml;q=0.9, */*;q=0.5"
        const val ARTICLE_ACCEPT = "text/html, application/xhtml+xml;q=0.9, */*;q=0.5"

        /** 일부 언론사가 기본 Java UA 를 막으므로 앱을 밝히는 값을 보낸다. */
        val USER_AGENT: String = String.format(
            Locale.US,
            "Mozilla/5.0 (Linux; Android 9; KFMAWI) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) DAON/0.9",
        )
    }
}
