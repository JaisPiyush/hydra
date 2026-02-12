package com.benzenelabs.hydra.host.data.db

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublicTableRegistryTest {

    @AfterEach
    fun tearDown() {
        setPublicTables(emptySet())
    }

    @Test
    fun `isPublic returns false for empty or unknown tables`() {
        assertFalse(PublicTableRegistry.isPublic(""))
        assertFalse(PublicTableRegistry.isPublic("unknown_table"))
    }

    @Test
    fun `isPublic returns true for registered table (case insensitive)`() {
        setPublicTables(setOf("test_table"))

        assertTrue(PublicTableRegistry.isPublic("test_table"))
        assertTrue(PublicTableRegistry.isPublic("TEST_TABLE"))
        assertTrue(PublicTableRegistry.isPublic("Test_Table"))
    }

    @Test
    fun `isPublic returns false for partial matches`() {
        setPublicTables(setOf("test_table"))

        assertFalse(PublicTableRegistry.isPublic("test_table_suffix"))
        assertFalse(PublicTableRegistry.isPublic("prefix_test_table"))
        assertFalse(PublicTableRegistry.isPublic("test"))
    }

    private fun setPublicTables(tables: Set<String>) {
        val field = PublicTableRegistry::class.java.getDeclaredField("PUBLIC_TABLES")
        field.isAccessible = true

        // Remove final modifier if necessary (JVM 12+ might complain, but works on Android/standard
        // 8-11 usually)
        // Since it's a Kotlin object val, it's a static final field in Java.
        // We might need to use a specialized library or just try basic reflection.
        // For simplicity, trying basic reflection first.

        // Note: Modifying final static fields via reflection is risky and JDK version dependent.
        // However, given the constraints, this is the only way to test "adding" without changing
        // code.

        try {
            field.set(PublicTableRegistry, tables)
        } catch (e: Exception) {
            // Fallback for newer JDKs or specific restrictions if needed,
            // but for now we assume standard reflection capability.
            throw RuntimeException("Failed to inject tables via reflection", e)
        }
    }
}
