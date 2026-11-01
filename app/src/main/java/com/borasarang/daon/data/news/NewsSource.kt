package com.borasarang.daon.data.news

/**
 * 뉴스 출처. 키·계정 없이 열려 있는 RSS 만 쓴다.
 *
 * Fire OS 7.3.1.2 는 오래된 루트 인증서만 신뢰하므로(시스템 CA 137개),
 * 새 루트가 필요한 Google News RSS 는 대상 기기에서 HTTPS 가 실패한다.
 * 두 출처 모두 기기 신뢰 저장소에 루트가 있는 것을 확인했다(2026-11-01).
 */
enum class NewsSource(
    val id: String,
    val labelKo: String,
    val labelEn: String,
    val categoryKo: String,
    val categoryEn: String,
    val feedUrl: String,
) {
    /** 연합뉴스 최신기사. 94KB · 120건 */
    YONHAP(
        id = "yonhap",
        labelKo = "연합뉴스",
        labelEn = "Yonhap",
        categoryKo = "최신",
        categoryEn = "Latest",
        feedUrl = "https://www.yna.co.kr/rss/news.xml",
    ),

    /** SBS 사회. 187KB · 29건. 전체 속보(776KB)는 매시간 받기에 너무 크다. */
    SBS(
        id = "sbs",
        labelKo = "SBS",
        labelEn = "SBS",
        categoryKo = "사회",
        categoryEn = "Society",
        feedUrl = "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=03",
    ),
    ;

    /** 화면에 보여 줄 출처 이름. 한국어 UI 에서는 통신사 표기를 그대로 쓴다. */
    fun displayName(korean: Boolean): String = if (korean) labelKo else labelEn

    fun displayCategory(korean: Boolean): String = if (korean) categoryKo else categoryEn

    companion object {
        fun fromId(id: String?): NewsSource? = entries.firstOrNull { it.id == id }

        /** 기사 주소의 호스트로 출처를 알아낸다. 모르는 곳이면 null. */
        fun fromLink(link: String?): NewsSource? {
            val host = runCatching { java.net.URI(link).host }.getOrNull() ?: return null
            return entries.firstOrNull { host == it.feedHost || host.endsWith(".${it.feedHost}") }
        }
    }

    private val feedHost: String
        get() = runCatching { java.net.URI(feedUrl).host }.getOrDefault("")
}
