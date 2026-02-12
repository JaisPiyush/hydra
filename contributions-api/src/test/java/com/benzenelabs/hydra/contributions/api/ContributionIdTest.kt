package com.benzenelabs.hydra.contributions.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ContributionIdTest {

    @Test
    fun `valid id is accepted`() {
        val id = ContributionId("acme.tool")
        assertEquals("acme.tool", id.value)
        assertEquals("acme", id.author)
        assertEquals("tool", id.name)
        assertEquals("acme.tool", id.toString())
    }

    @Test
    fun `valid id with underscores and hyphens is accepted`() {
        val id = ContributionId("org_x.tool-1")
        assertEquals("org_x.tool-1", id.value)
        assertEquals("org_x", id.author)
        assertEquals("tool-1", id.name)
    }

    @Test
    fun `invalid format throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            ContributionId("invalid") // Missing dot
        }
        assertThrows(IllegalArgumentException::class.java) { ContributionId("Space in.Author") }
        assertThrows(IllegalArgumentException::class.java) { ContributionId(".starts_with_dot") }
        assertThrows(IllegalArgumentException::class.java) { ContributionId("ends_with_dot.") }
        assertThrows(IllegalArgumentException::class.java) { ContributionId("multiple..dots") }
    }
}
