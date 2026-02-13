package com.benzenelabs.hydra.host.io.connection.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpTest {
    @Test
    fun `HttpRequest validations`() {
        assertThrows(IllegalArgumentException::class.java) { HttpRequest(url = "") }

        assertThrows(IllegalArgumentException::class.java) {
            HttpRequest(url = "http://url", method = HttpMethod.GET, body = byteArrayOf(1))
        }

        assertThrows(IllegalArgumentException::class.java) {
            HttpRequest(url = "http://url", method = HttpMethod.HEAD, body = byteArrayOf(1))
        }
    }

    @Test
    fun `HttpResponse status checks`() {
        assertTrue(HttpResponse(200).isSuccess)
        assertTrue(HttpResponse(299).isSuccess)
        assertFalse(HttpResponse(300).isSuccess)

        assertTrue(HttpResponse(400).isClientError)
        assertTrue(HttpResponse(499).isClientError)

        assertTrue(HttpResponse(500).isServerError)
        assertTrue(HttpResponse(599).isServerError)
    }

    @Test
    fun `HttpResponse bodyAsText`() {
        val resp = HttpResponse(200, body = "hello".toByteArray())
        assertEquals("hello", resp.bodyAsText())
    }
}
