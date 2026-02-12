package com.benzenelabs.hydra.host.data.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ConfigFieldTest {

    @Test
    fun `valid non-secret field constructs`() {
        val field = ConfigField(name = "theme", defaultValue = "dark", isSecret = false)
        assertEquals("theme", field.name)
        assertEquals("dark", field.defaultValue)
    }

    @Test
    fun `secret field with null default constructs`() {
        val field = ConfigField(name = "api_key", defaultValue = null, isSecret = true)
        assertEquals("api_key", field.name)
    }

    @Test
    fun `secret field with defaultValue throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfigField(name = "api_key", defaultValue = "default", isSecret = true)
        }
    }

    @Test
    fun `blank name throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConfigField(name = "", defaultValue = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConfigField(name = "   ", defaultValue = null)
        }
    }

    @Test
    fun `secretKey matches name`() {
        val name = "my_setting"
        val field = ConfigField(name = name)
        assertEquals(name, field.secretKey.value)
    }
}
