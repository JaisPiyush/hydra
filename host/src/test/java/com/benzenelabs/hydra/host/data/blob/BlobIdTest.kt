package com.benzenelabs.hydra.host.data.blob

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BlobIdTest {

    @Test
    fun `generate produces valid UUID`() {
        val id = BlobId.generate()
        // Should not throw
        UUID.fromString(id.value)
    }

    @Test
    fun `generate produces unique IDs`() {
        val id1 = BlobId.generate()
        val id2 = BlobId.generate()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `valid UUID constructs`() {
        val uuid = UUID.randomUUID().toString()
        val blobId = BlobId(uuid)
        assertEquals(uuid, blobId.value)
    }

    @Test
    fun `invalid UUID throws`() {
        assertThrows(IllegalArgumentException::class.java) { BlobId("not-a-uuid") }
    }

    @Test
    fun `blank string throws`() {
        assertThrows(IllegalArgumentException::class.java) { BlobId("") }
    }
}
