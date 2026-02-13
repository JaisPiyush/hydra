package com.benzenelabs.hydra.host.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IoFrameTest {
    @Test
    fun `invalid args throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            IoFrame(
                    connectionId = "",
                    payload = ByteArray(0),
                    frameType = FrameType.BINARY,
                    receivedAt = 100L
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            IoFrame(
                    connectionId = "id",
                    payload = ByteArray(0),
                    frameType = FrameType.BINARY,
                    receivedAt = 0L
            )
        }
    }

    @Test
    fun `equality checks content`() {
        val f1 = IoFrame("c1", byteArrayOf(1, 2, 3), FrameType.BINARY, 1000L)
        val f2 = IoFrame("c1", byteArrayOf(1, 2, 3), FrameType.BINARY, 1000L)
        val f3 = IoFrame("c1", byteArrayOf(1, 2, 4), FrameType.BINARY, 1000L)

        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())

        // Assert inequality
        assert(f1 != f3)
    }

    @Test
    fun `payloadAsText decodes utf8`() {
        val text = "Hello World"
        val frame = IoFrame("c1", text.toByteArray(Charsets.UTF_8), FrameType.TEXT, 1000L)
        assertEquals(text, frame.payloadAsText())
    }

    @Test
    fun `frame types existence`() {
        assertEquals(2, FrameType.values().size)
        FrameType.valueOf("BINARY")
        FrameType.valueOf("TEXT")
    }
}
