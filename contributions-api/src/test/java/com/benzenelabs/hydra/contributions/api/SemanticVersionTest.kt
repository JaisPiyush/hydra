package com.benzenelabs.hydra.contributions.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticVersionTest {

    @Test
    fun `parses valid version string`() {
        val v = SemanticVersion.parse("1.2.3")
        assertEquals(1, v.major)
        assertEquals(2, v.minor)
        assertEquals(3, v.patch)
        assertEquals("1.2.3", v.toString())
    }

    @Test
    fun `parses version with multiple digits`() {
        val v = SemanticVersion.parse("10.200.3000")
        assertEquals(10, v.major)
        assertEquals(200, v.minor)
        assertEquals(3000, v.patch)
    }

    @Test
    fun `parse throws IllegalArgumentException for invalid format`() {
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("1.2") }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("1.2.3.4") }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("invalid") }
    }

    @Test
    fun `parse throws IllegalStateException for non-integer components`() {
        assertThrows(IllegalStateException::class.java) { SemanticVersion.parse("1.2.a") }
        assertThrows(IllegalStateException::class.java) { SemanticVersion.parse("1.b.3") }
        assertThrows(IllegalStateException::class.java) { SemanticVersion.parse("c.2.3") }
    }

    @Test
    fun `init block validates non-negative components`() {
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion(-1, 0, 0) }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion(0, -1, 0) }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion(0, 0, -1) }
    }

    @Test
    fun `compareTo correctly orders versions`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(1, 0, 1)
        val v3 = SemanticVersion(1, 1, 0)
        val v4 = SemanticVersion(2, 0, 0)

        assertTrue(v1 < v2)
        assertTrue(v2 < v3)
        assertTrue(v3 < v4)

        // Reflexive
        assertTrue(v1.compareTo(SemanticVersion(1, 0, 0)) == 0)
    }
}
