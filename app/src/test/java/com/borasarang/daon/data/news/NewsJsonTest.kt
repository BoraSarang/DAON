package com.borasarang.daon.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NewsJsonTest {

    @Test
    fun `round trip keeps both sources and ordering`() {
        val items = (0 until 60).map { index ->
            NewsItem(
                source = if (index % 2 == 0) NewsSource.YONHAP else NewsSource.SBS,
                title = "기사 제목 $index",
                link = "https://example.com/$index",
                summary = "요약 문장 $index",
                publishedAt = Instant.parse("2026-11-01T08:00:00Z").minusSeconds(index * 60L),
            )
        }

        val decoded = NewsJson.decode(NewsJson.encode(items))

        assertEquals(items, decoded)
    }

    @Test
    fun `decode skips malformed entries and keeps the rest`() {
        val text = """
            [
              {"s":"yonhap","t":"정상 기사","l":"https://www.yna.co.kr/view/1","m":"요약","p":1761984000000},
              {"s":"unknown","t":"모르는 출처","l":"https://example.com/2","m":"","p":1761984000000},
              {"s":"sbs","t":"","l":"https://news.sbs.co.kr/3","m":"","p":1761984000000},
              {"s":"sbs","t":"주소 없음","l":"","m":"","p":1761984000000},
              {"s":"sbs","t":"시각 없음","l":"https://news.sbs.co.kr/4","m":"","p":0},
              "not an object"
            ]
        """.trimIndent()

        val decoded = NewsJson.decode(text)

        assertEquals(1, decoded.size)
        assertEquals("정상 기사", decoded.first().title)
    }

    @Test
    fun `decode tolerates empty and broken json`() {
        assertTrue(NewsJson.decode(null).isEmpty())
        assertTrue(NewsJson.decode("").isEmpty())
        assertTrue(NewsJson.decode("not json").isEmpty())
        assertTrue(NewsJson.decode("{}").isEmpty())
    }
}
