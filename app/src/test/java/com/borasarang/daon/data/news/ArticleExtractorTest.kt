package com.borasarang.daon.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleExtractorTest {

    @Test
    fun `yonhap keeps paragraphs and drops figures bylines and copyright`() {
        val content = ArticleExtractor.extract(
            NewsSource.YONHAP,
            YONHAP_HTML,
            summary = "RSS 요약",
        )

        assertFalse(content.usedSummary)
        assertEquals(3, content.paragraphs.size)
        assertEquals("(서울=연합뉴스) 배진남 기자 = 첫 문단입니다.", content.paragraphs[0])
        assertTrue(content.paragraphs[1].contains("'따옴표'"))
        assertTrue(content.paragraphs[1].contains("\"인용\""))
        assertEquals("셋째 문단은 조금 더 길게 적었습니다.", content.paragraphs[2])
        assertFalse(content.paragraphs.any { it.contains("캡션") })
        assertFalse(content.paragraphs.any { it.contains("저작권") })
        assertFalse(content.paragraphs.any { it.contains("@yna.co.kr") })
        assertFalse(content.paragraphs.any { it.contains("관련 기사") })
    }

    @Test
    fun `sbs splits json-ld body into paragraphs and removes photo captions`() {
        val content = ArticleExtractor.extract(
            NewsSource.SBS,
            SBS_JSON_LD_HTML,
            summary = "RSS 요약",
        )

        assertFalse(content.usedSummary)
        assertEquals(2, content.paragraphs.size)
        assertTrue(content.paragraphs.first().startsWith("첫 번째 문장입니다."))
        assertTrue(content.paragraphs.last().contains("네 번째 문장입니다."))
        assertFalse(content.paragraphs.any { it.contains("▲") })
        assertFalse(content.paragraphs.any { it.contains("사진=") })
    }

    @Test
    fun `sbs falls back to br separated body when json-ld is missing`() {
        val content = ArticleExtractor.extract(
            NewsSource.SBS,
            SBS_BR_HTML,
            summary = "RSS 요약",
        )

        assertFalse(content.usedSummary)
        assertEquals(2, content.paragraphs.size)
        assertEquals("첫 문단입니다. 같은 줄입니다.", content.paragraphs[0])
        assertEquals("둘째 문단은 조금 더 길게 적었습니다.", content.paragraphs[1])
    }

    @Test
    fun `falls back to rss summary without raising errors`() {
        val empty = ArticleExtractor.extract(NewsSource.YONHAP, "<html></html>", "  ")
        assertTrue(empty.isEmpty)
        assertTrue(empty.usedSummary)

        val broken = ArticleExtractor.extract(
            NewsSource.SBS,
            "<script type=\"application/ld+json\">{broken json</script>",
            "마지막으로 받은 요약 문장입니다.&nbsp;다시 연결되면 갱신합니다.",
        )
        assertTrue(broken.usedSummary)
        assertEquals(1, broken.paragraphs.size)
        assertEquals("마지막으로 받은 요약 문장입니다. 다시 연결되면 갱신합니다.", broken.paragraphs.first())
    }

    @Test
    fun `unknown markup never throws`() {
        val content = ArticleExtractor.extract(
            NewsSource.SBS,
            "<html><body><div><p>짧음</p></div></body></html>",
            "",
        )
        assertTrue(content.isEmpty)
    }

    private companion object {
        val YONHAP_HTML = """
            <article id="articleWrap" class="article-wrap01">
            <div class="story-news article">
              <div class="writer-zone01">
                <figure class="img-con01"><img src="reporter.jpg" alt="기자"></figure>
              </div>
              <div class="comp-box photo-group">
                <figure class="image-zone01">
                  <div class="img-con01"><img src="photo.jpg" alt="사진"></div>
                  <figcaption class="desc-con">
                    <strong class="tit-cap">사진 캡션</strong>
                    <p class="txt-desc">[한국프로축구연맹 제공. 재판매 및 DB 금지]</p>
                  </figcaption>
                </figure>
              </div>
              <p> (서울=연합뉴스) 배진남 기자 = 첫 문단입니다.</p>
              <p> 둘째 문단에는 &apos;따옴표&apos;와 &quot;인용&quot;이 있습니다.</p>
              <p> 셋째 문단은 조금 더 길게 적었습니다.</p>
              <p> hosu1@yna.co.kr<br/></p>
              <div class="related-zone rel"><p>관련 기사 링크 문단입니다.</p></div>
              <p class="txt-copyright adrs">저작권 문단입니다.</p>
            </div>
            </article>
        """.trimIndent()

        val SBS_JSON_LD_HTML = """
            <html><head>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[{"@type":"NewsArticle",
             "headline":"부하 직원에 아이는 남편에게 보라 해",
             "articleBody":"▲ 사진 설명입니다. 첫 번째 문장입니다. 두 번째 문장입니다. 세 번째 문장입니다. 네 번째 문장입니다. (사진=연합뉴스)"}]}
            </script>
            </head><body></body></html>
        """.trimIndent()

        val SBS_BR_HTML = """
            <html><body>
            <div class="main_text">
              <div class="text_area" itemprop="articleBody">
                <div style="text-align:center"><strong>▲ 사진 설명</strong></div>
                <br> 첫 문단입니다. 같은 줄입니다.
                <br><br> 둘째 문단은 조금 더 길게 적었습니다.
                <br> (사진=연합뉴스)
              </div>
            </div>
            <div class="copyrightsbs">무단 전재 및 재배포 금지</div>
            </body></html>
        """.trimIndent()
    }
}
