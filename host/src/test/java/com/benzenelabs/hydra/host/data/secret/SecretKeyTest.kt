package com.benzenelabs.hydra.host.data.secret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SecretKeyTest {

    @Test
    fun `valid keys are accepted`() {
        val validKeys = listOf("api_key", "OAuth-Token", "key123", "A-B_C")
        validKeys.forEach { assertEquals(it, SecretKey(it).value) }
    }

    @Test
    fun `toString returns value`() {
        assertEquals("api_key", SecretKey("api_key").toString())
    }

    @Test
    fun `invalid keys throw IllegalArgumentException`() {
        val invalidKeys =
                listOf(
                        "", // blank
                        "   ", // blank with spaces
                        "key with space",
                        "key.with.dot",
                        "key/with/slash",
                        "key@with@at",
                        "a".repeat(129) // too long
                )

        invalidKeys.forEach { key ->
            assertThrows(IllegalArgumentException::class.java) { SecretKey(key) }
        }
    }
}
