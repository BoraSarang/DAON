package com.borasarang.daon.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NewsRetentionTest {

    private val now = Instant.parse("2026-11-01T09:00:00Z")

    @Test
    fun `prune drops items older than 48 hours and keeps the rest`() {
        val items = listOf(
            item(NewsSource.YONHAP, "최근 기사", hoursAgo = 1),
            item(NewsSource.YONHAP, "이틀 조금 전 기사", hoursAgo = 47),
            item(NewsSource.SBS, "사흘 지난 기사", hoursAgo = 49),
        )

        val pruned = NewsRetention.prune(items, now)

        assertEquals(listOf("최근 기사", "이틀 조금 전 기사"), pruned.map { it.title })
        assertTrue(pruned.all { it.source == NewsSource.YONHAP })
    }

    @Test
    fun `prune keeps at most thirty items per source`() {
        val items = (0 until 35).map { index ->
            item(NewsSource.SBS, "SBS 기사 $index", hoursAgo = index.toLong())
        }

        val pruned = NewsRetention.prune(items, now)

        assertEquals(NewsItem.MAX_PER_SOURCE, pruned.size)
        assertEquals("SBS 기사 0", pruned.first().title)
        assertEquals("SBS 기사 29", pruned.last().title)
    }

    @Test
    fun `successful replacement drops items missing from the new feed`() {
        val current = listOf(
            item(NewsSource.YONHAP, "연합 옛 기사", hoursAgo = 3),
            item(NewsSource.SBS, "SBS 유지 기사", hoursAgo = 2),
        )
        val fresh = listOf(
            item(NewsSource.YONHAP, "연합 새 기사", hoursAgo = 0),
            item(NewsSource.YONHAP, "연합 새 기사 둘", hoursAgo = 1),
        )

        val updated = NewsRetention.replace(current, NewsSource.YONHAP, fresh, now)

        assertEquals(
            listOf("연합 새 기사", "연합 새 기사 둘", "SBS 유지 기사"),
            updated.map { it.title },
        )
    }

    @Test
    fun `one source failing keeps its last list`() {
        val current = listOf(
            item(NewsSource.YONHAP, "연합 옛 기사", hoursAgo = 3),
            item(NewsSource.SBS, "SBS 유지 기사", hoursAgo = 2),
        )
        val fresh = mapOf(
            NewsSource.YONHAP to listOf(item(NewsSource.YONHAP, "연합 새 기사", hoursAgo = 0)),
        )

        val updated = NewsRetention.mergeSuccessful(current, fresh, now)

        assertEquals(2, updated.size)
        assertTrue(updated.any { it.title == "SBS 유지 기사" })
        assertTrue(updated.any { it.title == "연합 새 기사" })
        assertTrue(updated.none { it.title == "연합 옛 기사" })
    }

    @Test
    fun `both sources failing changes nothing`() {
        val current = listOf(
            item(NewsSource.YONHAP, "연합 기사", hoursAgo = 3),
            item(NewsSource.SBS, "SBS 기사", hoursAgo = 2),
        )

        val updated = NewsRetention.mergeSuccessful(current, emptyMap(), now)

        assertEquals(current.sortedByDescending { it.publishedAt }, updated)
    }

    private fun item(source: NewsSource, title: String, hoursAgo: Long) = NewsItem(
        source = source,
        title = title,
        link = "https://example.com/${source.id}/$title",
        summary = "요약",
        publishedAt = now.minusSeconds(hoursAgo * 3_600L),
    )
}
