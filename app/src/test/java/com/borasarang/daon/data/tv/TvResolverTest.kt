package com.borasarang.daon.data.tv

import com.borasarang.daon.core.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TvResolverTest {

    @Test
    fun `kbs json returns service url`() {
        val body = """{"channel_item":[{"service_url":"https://cf.example/kbs1/playlist.m3u8?Policy=x"}]}"""

        assertEquals(
            "https://cf.example/kbs1/playlist.m3u8?Policy=x",
            TvResolver.parseKbs(body),
        )
    }

    @Test
    fun `kbs json without service url throws`() {
        val error = assertThrows(TvException::class.java) {
            TvResolver.parseKbs("""{"channel_item":[]}""")
        }

        assertEquals(ErrorCode.TV_RESOLVE, error.code)
    }

    @Test
    fun `mbc json returns media url`() {
        val body = """
            {"Msg":"OK","MediaInfo":{"MediaURL":"https://mbc.example/live/playlist.m3u8?k=x"}}
        """.trimIndent()

        assertEquals(
            "https://mbc.example/live/playlist.m3u8?k=x",
            TvResolver.parseMbc(body),
        )
    }

    @Test
    fun `mbc protocol relative url becomes https`() {
        val body = """{"Msg":"OK","MediaInfo":{"MediaURL":"//mbc.example/live/playlist.m3u8"}}"""

        assertEquals("https://mbc.example/live/playlist.m3u8", TvResolver.parseMbc(body))
    }

    @Test
    fun `mbc error message throws`() {
        val error = assertThrows(TvException::class.java) {
            TvResolver.parseMbc("""{"Msg":"FAIL","MediaInfo":{}}""")
        }

        assertEquals(ErrorCode.TV_RESOLVE, error.code)
    }

    @Test
    fun `sbs json prefers the single media source`() {
        val body = """
            {"onair":{"source":{
              "mediasource":{"mediaurl":"https://sbs.example/single/playlist.m3u8"},
              "mediasourcelist":[{"mediaurl":"https://sbs.example/list/playlist.m3u8"}]
            }}}
        """.trimIndent()

        assertEquals("https://sbs.example/single/playlist.m3u8", TvResolver.parseSbs(body))
    }

    @Test
    fun `sbs json falls back to the last media source`() {
        val body = """
            {"onair":{"source":{"mediasourcelist":[
              {"mediaurl":"https://sbs.example/old/playlist.m3u8"},
              {"mediaurl":"https://sbs.example/new/playlist.m3u8"}
            ]}}}
        """.trimIndent()

        assertEquals("https://sbs.example/new/playlist.m3u8", TvResolver.parseSbs(body))
    }

    @Test
    fun `sbs plain http url is upgraded to https`() {
        val body = """{"onair":{"source":{"mediasource":{"mediaurl":"http://sbs.example/live/playlist.m3u8"}}}}"""

        assertEquals("https://sbs.example/live/playlist.m3u8", TvResolver.parseSbs(body))
    }

    @Test
    fun `sbs json without media url throws`() {
        val error = assertThrows(TvException::class.java) {
            TvResolver.parseSbs("""{"onair":{"source":{}}}""")
        }

        assertEquals(ErrorCode.TV_RESOLVE, error.code)
    }
}
