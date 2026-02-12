package com.benzenelabs.hydra.host.data.vector

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class VectorRecordTest {

    @Test
    fun `equals works for identical content`() {
        val v1 = VectorRecord("id1", floatArrayOf(1f, 2f), "meta")
        val v2 = VectorRecord("id1", floatArrayOf(1f, 2f), "meta")

        assertEquals(v1, v2)
        assertEquals(v1.hashCode(), v2.hashCode())
    }

    @Test
    fun `equals fails for different vector content`() {
        val v1 = VectorRecord("id1", floatArrayOf(1f, 2f), "meta")
        val v2 = VectorRecord("id1", floatArrayOf(1f, 3f), "meta")

        assertNotEquals(v1, v2)
    }

    @Test
    fun `equals fails for different id`() {
        val v1 = VectorRecord("id1", floatArrayOf(1f, 2f), "meta")
        val v2 = VectorRecord("id2", floatArrayOf(1f, 2f), "meta")

        assertNotEquals(v1, v2)
    }

    @Test
    fun `metadata can be null`() {
        val v = VectorRecord("id", floatArrayOf(0f), null)
        assertEquals("id", v.id)
        assertEquals(null, v.metadata)
    }
}
