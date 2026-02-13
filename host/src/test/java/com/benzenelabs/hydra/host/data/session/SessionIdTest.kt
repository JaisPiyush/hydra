package com.benzenelabs.hydra.host.data.session

import java.util.UUID
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SessionIdTest {

    @Test
    fun `generate produces valid UUID string`() {
        val id = SessionId.generate()
        // UUID.fromString throws if invalid
        UUID.fromString(id.value)
    }

    @Test
    fun `generate produces distinct values`() {
        val id1 = SessionId.generate()
        val id2 = SessionId.generate()
        assertNotEquals(id1.value, id2.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank value throws`() {
        SessionId("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid UUID string throws`() {
        SessionId("not-a-uuid")
    }
}
