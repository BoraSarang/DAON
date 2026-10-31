package com.borasarang.daon.data.radio

import com.borasarang.daon.core.ErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RadioResolverTest {

    @Test
    fun `kbs json returns service url`() {
        val body = """{"channel_item":[{"service_url":"https://cf.example/abc/playlist.m3u8?Policy=x"}]}"""

        assertEquals(
            "https://cf.example/abc/playlist.m3u8?Policy=x",
            RadioResolver.parseKbs(body),
        )
    }

    @Test
    fun `kbs json without service url throws`() {
        val error = assertThrows(RadioException::class.java) {
            RadioResolver.parseKbs("""{"channel_item":[]}""")
        }

        assertEquals(ErrorCode.RADIO_RESOLVE, error.code)
    }

    @Test
    fun `plain text trims whitespace and bom`() {
        val body = "\uFEFF  https://example.com/live.m3u8?token=abc  \r\n"

        assertEquals(
            "https://example.com/live.m3u8?token=abc",
            RadioResolver.parsePlainText(body),
        )
    }

    @Test
    fun `plain text without url throws`() {
        val error = assertThrows(RadioException::class.java) {
            RadioResolver.parsePlainText("no url here")
        }

        assertEquals(ErrorCode.RADIO_RESOLVE, error.code)
    }
}
