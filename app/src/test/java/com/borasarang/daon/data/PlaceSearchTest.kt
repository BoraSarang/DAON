package com.borasarang.daon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchTest {

    @Test
    fun `parses name and detail from nominatim result`() {
        val body = """
            [{"place_id":224003993,"lat":"36.9115864","lon":"127.9931536",
              "category":"place","type":"town","name":"살미면",
              "display_name":"살미면, 충주시, 충청북도, 27493, 대한민국"}]
        """.trimIndent()

        val results = PlaceSearch.parseResults(body)

        assertEquals(1, results.size)
        assertEquals("살미면", results[0].name)
        assertEquals(36.9115864, results[0].latitude, 1e-6)
        assertEquals(127.9931536, results[0].longitude, 1e-6)
        // 우편번호(27493)는 읽기 어려워 뺀다.
        assertEquals("충주시, 충청북도, 대한민국", results[0].detail)
        assertEquals("살미면, 충주시", results[0].label)
    }

    @Test
    fun `skips entries without coordinates`() {
        val body = """
            [{"lat":"36.9","lon":"127.9","name":"가","display_name":"가, 나"},
             {"name":"좌표없음","display_name":"좌표없음"}]
        """.trimIndent()

        val results = PlaceSearch.parseResults(body)

        assertEquals(1, results.size)
        assertEquals("가", results[0].name)
    }

    @Test
    fun `empty array gives empty list`() {
        assertTrue(PlaceSearch.parseResults("[]").isEmpty())
    }
}
