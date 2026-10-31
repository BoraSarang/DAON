package com.borasarang.daon.data.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioCatalogTest {

    private val sample = """
        [
          {"id":"a","name":"KBS 1라디오","nameEn":"KBS 1 Radio","freq":"97.3","type":"KBS","apiUrl":"https://example.com/kbs"},
          {"id":"b","name":"MBC 표준FM","freq":"95.9","type":"MBC","apiUrl":"https://example.com/mbc"},
          {"id":"c","name":"EBS FM","freq":"104.5","type":"DIRECT","streamUrl":"https://example.com/ebs.m3u8"},
          {"id":"bad1","name":"No URL","freq":"1.0","type":"KBS"},
          {"id":"bad2","name":"No Stream","freq":"2.0","type":"DIRECT"},
          {"id":"","name":"No id","freq":"3.0","type":"DIRECT","streamUrl":"https://example.com/x.m3u8"},
          {"name":"No id key","freq":"4.0","type":"DIRECT","streamUrl":"https://example.com/y.m3u8"}
        ]
    """.trimIndent()

    @Test
    fun `parses valid channels and skips broken entries`() {
        val channels = RadioCatalog.parse(sample)

        assertEquals(listOf("a", "b", "c"), channels.map { it.id })
        assertEquals("KBS 1 Radio", channels[0].nameEn)
        assertEquals("MBC 표준FM", channels[1].nameEn)
        assertEquals(RadioSourceType.DIRECT, channels[2].type)
    }

    @Test
    fun `blank or malformed json yields empty list`() {
        assertTrue(RadioCatalog.parse(null).isEmpty())
        assertTrue(RadioCatalog.parse("").isEmpty())
        assertTrue(RadioCatalog.parse("{not json").isEmpty())
    }

    @Test
    fun `unknown source type falls back to direct`() {
        val channels = RadioCatalog.parse(
            """[{"id":"x","name":"X","freq":"1","type":"NOPE","streamUrl":"https://e/x.m3u8"}]""",
        )

        assertEquals(RadioSourceType.DIRECT, channels.single().type)
    }

    @Test
    fun `display name follows the language`() {
        val channel = RadioCatalog.parse(sample).first()

        assertEquals("KBS 1라디오", channel.displayName(korean = true))
        assertEquals("KBS 1 Radio", channel.displayName(korean = false))
    }
}
