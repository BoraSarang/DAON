package com.borasarang.daon.data.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvCatalogTest {

    @Test
    fun `parses bundled catalog`() {
        val text = """
            [
              {"id":"kbs1","name":"KBS 1TV","nameEn":"KBS 1TV","category":"지상파","type":"KBS",
               "apiUrl":"https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/11"},
              {"id":"ebs1","name":"EBS 1TV","nameEn":"EBS 1TV","category":"교육","type":"DIRECT",
               "streamUrl":"https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8"}
            ]
        """.trimIndent()

        val channels = TvCatalog.parse(text)

        assertEquals(2, channels.size)
        assertEquals("kbs1", channels[0].id)
        assertEquals(TvSourceType.KBS, channels[0].type)
        assertEquals("https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/11", channels[0].apiUrl)
        assertEquals(TvSourceType.DIRECT, channels[1].type)
        assertEquals(
            "https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8",
            channels[1].streamUrl,
        )
    }

    @Test
    fun `skips broken rows and keeps the rest`() {
        val text = """
            [
              {"id":"","name":"no id","type":"DIRECT","streamUrl":"https://a/1.m3u8"},
              {"id":"x","name":"no url","type":"DIRECT"},
              {"id":"y","name":"no api","type":"KBS"},
              {"id":"ok","name":"OK","type":"DIRECT","streamUrl":"https://a/ok.m3u8"}
            ]
        """.trimIndent()

        val channels = TvCatalog.parse(text)

        assertEquals(1, channels.size)
        assertEquals("ok", channels[0].id)
    }

    @Test
    fun `unknown type falls back to direct`() {
        val text = """[{"id":"z","name":"Z","type":"NEWTYPE","streamUrl":"https://a/z.m3u8"}]"""

        val channels = TvCatalog.parse(text)

        assertEquals(TvSourceType.DIRECT, channels.single().type)
    }

    @Test
    fun `broken json returns empty list`() {
        assertTrue(TvCatalog.parse("{not json").isEmpty())
        assertTrue(TvCatalog.parse(null).isEmpty())
        assertTrue(TvCatalog.parse("").isEmpty())
    }

    @Test
    fun `display name follows language`() {
        val channel = TvChannel(
            id = "kbs1",
            name = "KBS 1TV",
            nameEn = "KBS 1",
            category = "지상파",
            type = TvSourceType.KBS,
            apiUrl = "https://example.com",
        )

        assertEquals("KBS 1TV", channel.displayName(korean = true))
        assertEquals("KBS 1", channel.displayName(korean = false))
    }
}
