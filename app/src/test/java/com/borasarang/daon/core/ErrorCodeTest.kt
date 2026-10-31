package com.borasarang.daon.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorCodeTest {

    @Test
    fun `format pads the number to four digits`() {
        assertEquals("E-AND-NET-0007", ErrorCode.format("AND", "NET", 7))
    }

    @Test
    fun `format matches the shared error code shape`() {
        val code = ErrorCode.format("AND", "STORE", 42)
        assertEquals(true, Regex("^E-[A-Z]{3}-[A-Z]+-\\d{4}$").matches(code))
    }

    @Test
    fun `declared codes use the shared shape`() {
        listOf(ErrorCode.NET_TIMEOUT, ErrorCode.NET_PARSE, ErrorCode.NET_HTTP, ErrorCode.STORE_WRITE)
            .forEach { code ->
                assertEquals("bad shape: $code", true, Regex("^E-AND-[A-Z]+-\\d{4}$").matches(code))
            }
    }
}
