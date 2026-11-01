package com.borasarang.daon.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NewsFeedParserTest {

    @Test
    fun `parses real feed excerpts with cdata and entities`() {
        val items = NewsFeedParser.parse(NewsSource.YONHAP, YONHAP_EXCERPT)

        assertEquals(1, items.size)
        val item = items.first()
        assertEquals(NewsSource.YONHAP, item.source)
        assertEquals(
            "'에릭 멀티 골' 울산, 강원에 2-1 역전승… K리그1 2위 사수",
            item.title,
        )
        assertEquals("https://www.yna.co.kr/view/AKR20261101033951007", item.link)
        assertEquals(Instant.parse("2026-11-01T09:49:33Z"), item.publishedAt)
        assertTrue(item.summary.startsWith("(서울=연합뉴스) 배진남 기자 ="))
        assertTrue(item.summary.contains("에릭의 멀티 골"))
        assertFalse(item.summary.contains("&nbsp;"))
    }

    @Test
    fun `drops score tables rankings subtitle clips and short titles`() {
        val items = NewsFeedParser.parse(NewsSource.YONHAP, MIXED_EXCERPT)

        assertEquals(listOf("순천서 탱크로리 액화산소 누출…인명 피해 없어"), items.map { it.title })
    }

    @Test
    fun `merges duplicate titles across sources and keeps the newest`() {
        val items = NewsFeedParser.parse(NewsSource.YONHAP, DUPLICATE_EXCERPT) +
            NewsFeedParser.parse(NewsSource.SBS, DUPLICATE_EXCERPT_SBS)

        val merged = NewsFeedParser.merge(items)

        assertEquals(1, merged.size)
        assertEquals(NewsSource.SBS, merged.first().source)
        assertEquals("서울 관악구 숙박업소 화재로 16명 대피… 사상자 없어", merged.first().title)
    }

    @Test
    fun `merges ten per source and fills shortfalls from the other`() {
        val items = buildItems(NewsSource.YONHAP, count = 18, startMinute = 0) +
            buildItems(NewsSource.SBS, count = 6, startMinute = 30)

        val merged = NewsFeedParser.merge(items)

        assertEquals(NewsItem.LIST_SIZE, merged.size)
        assertEquals(14, merged.count { it.source == NewsSource.YONHAP })
        assertEquals(6, merged.count { it.source == NewsSource.SBS })
        assertEquals(merged.sortedByDescending { it.publishedAt }, merged)
    }

    @Test
    fun `tolerates broken feeds and keeps valid items`() {
        assertTrue(NewsFeedParser.parse(NewsSource.SBS, "").isEmpty())
        assertTrue(NewsFeedParser.parse(NewsSource.SBS, "<rss><channel></channel></rss>").isEmpty())
        assertTrue(NewsFeedParser.parse(NewsSource.SBS, "not xml at all").isEmpty())

        val items = NewsFeedParser.parse(NewsSource.SBS, PARTIAL_EXCERPT)

        assertEquals(1, items.size)
        assertEquals("국립공원 산불 예방…148개 탐방로 한 달 출입 통제", items.first().title)
        assertTrue(items.first().summary.contains("▲"))
        assertTrue(items.first().title.contains("…"))
    }

    @Test
    fun `time label distinguishes today yesterday and older`() {
        val now = LocalDateTime.of(2026, 11, 1, 18, 30)
        val zone = ZoneId.of("Asia/Seoul")
        fun item(at: LocalDateTime) = NewsItem(
            source = NewsSource.SBS,
            title = "서울 관악구 숙박업소 화재로 16명 대피",
            link = "https://news.sbs.co.kr/news/endPage.do?news_id=N1",
            summary = "",
            publishedAt = at.atZone(zone).toInstant(),
        )

        assertEquals("18:49", item(LocalDateTime.of(2026, 11, 1, 18, 49)).timeLabel(now, true, zone))
        assertTrue(
            item(LocalDateTime.of(2026, 10, 31, 9, 5)).timeLabel(now, true, zone)
                .startsWith("어제 09:05"),
        )
        assertEquals("10/29 07:03", item(LocalDateTime.of(2026, 10, 29, 7, 3)).timeLabel(now, true, zone))
    }

    private fun buildItems(
        source: NewsSource,
        count: Int,
        startMinute: Int,
    ): List<NewsItem> {
        val base = Instant.parse("2026-11-01T00:00:00Z")
        return (0 until count).map { index ->
            NewsItem(
                source = source,
                title = "${source.id} 소식 $index 번째 기사 제목",
                link = "https://example.com/${source.id}/$index",
                summary = "요약 $index",
                publishedAt = base.plusSeconds((startMinute + index).toLong() * 60L),
            )
        }
    }

    private companion object {
        val YONHAP_EXCERPT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <item>
                <title><![CDATA['에릭 멀티 골' 울산, 강원에 2-1 역전승… K리그1 2위 사수(종합)]]></title>
                <link>https://www.yna.co.kr/view/AKR20261101033951007</link>
                <pubDate>Sun, 1 Nov 2026 18:49:33 +0900</pubDate>
                <description><![CDATA[(서울=연합뉴스) 배진남 기자 = 프로축구 K리그1 울산 HD가&nbsp;에릭의 멀티 골로 강원FC에 역전승을 거뒀다.]]></description>
              </item>
            </channel></rss>
        """.trimIndent()

        val MIXED_EXCERPT = """
            <rss version="2.0"><channel>
              <item>
                <title><![CDATA[[프로축구 서울전적] 인천 1-0 서울]]></title>
                <link>https://www.yna.co.kr/view/AKR1</link>
                <pubDate>Sun, 1 Nov 2026 18:40:00 +0900</pubDate>
                <description>전적</description>
              </item>
              <item>
                <title><![CDATA[[프로농구 중간순위] 1일]]></title>
                <link>https://www.yna.co.kr/view/AKR2</link>
                <pubDate>Sun, 1 Nov 2026 18:30:00 +0900</pubDate>
                <description>순위</description>
              </item>
              <item>
                <title><![CDATA[[자막뉴스] '내기 골프' 7천만 원 털렸는데…CCTV에 포착된 '수상한 커피']]></title>
                <link>https://news.sbs.co.kr/news/endPage.do?news_id=N1</link>
                <pubDate>Sun, 1 Nov 2026 17:54:00 +0900</pubDate>
                <description>자막</description>
              </item>
              <item>
                <title><![CDATA[속보]]></title>
                <link>https://www.yna.co.kr/view/AKR3</link>
                <pubDate>Sun, 1 Nov 2026 17:50:00 +0900</pubDate>
                <description>짧은 제목</description>
              </item>
              <item>
                <title><![CDATA[[북한단신] 오늘의 북한 소식 모음]]></title>
                <link>https://www.yna.co.kr/view/AKR4</link>
                <pubDate>Sun, 1 Nov 2026 17:45:00 +0900</pubDate>
                <description>단신</description>
              </item>
              <item>
                <title><![CDATA[순천서 탱크로리 액화산소 누출…인명 피해 없어]]></title>
                <link>https://www.yna.co.kr/view/AKR5</link>
                <pubDate>Sun, 1 Nov 2026 17:30:00 +0900</pubDate>
                <description>사고 소식</description>
              </item>
            </channel></rss>
        """.trimIndent()

        val DUPLICATE_EXCERPT = """
            <rss version="2.0"><channel>
              <item>
                <title><![CDATA[서울 관악구 숙박업소 화재로 16명 대피… 사상자 없어(종합)]]></title>
                <link>https://www.yna.co.kr/view/AKR9</link>
                <pubDate>Sun, 1 Nov 2026 17:09:00 +0900</pubDate>
                <description>연합 요약</description>
              </item>
            </channel></rss>
        """.trimIndent()

        val DUPLICATE_EXCERPT_SBS = """
            <rss version="2.0"><channel>
              <item>
                <title><![CDATA[서울 관악구 숙박업소 화재로 16명 대피… 사상자 없어]]></title>
                <link>https://news.sbs.co.kr/news/endPage.do?news_id=N2</link>
                <pubDate>Sun, 1 Nov 2026 17:45:00 +0900</pubDate>
                <description>SBS 요약</description>
              </item>
            </channel></rss>
        """.trimIndent()

        val PARTIAL_EXCERPT = """
            <rss version="2.0"><channel>
              <item>
                <title><![CDATA[국립공원 산불 예방…148개 탐방로 한 달 출입 통제]]></title>
                <link><![CDATA[https://news.sbs.co.kr/news/endPage.do?news_id=N3]]></link>
                <pubDate>Sun, 1 Nov 2026 16:20:00 +0900</pubDate>
                <description><![CDATA[&#9650; 사진 설명입니다.&nbsp;산림청이 탐방로를 통제한다.]]></description>
              </item>
              <item>
                <title><![CDATA[발행 시각이 없는 기사 제목입니다]]></title>
                <link>https://news.sbs.co.kr/news/endPage.do?news_id=N4</link>
                <description>시각 없음</description>
              </item>
              <item>
                <link>https://news.sbs.co.kr/news/endPage.do?news_id=N5</link>
                <pubDate>Sun, 1 Nov 2026 16:00:00 +0900</pubDate>
                <description>제목 없음</description>
              </item>
            </channel></rss>
        """.trimIndent()
    }
}
