package com.benzenelabs.hydra.host.io.connection.longpoll

import org.junit.Assert.assertEquals
import org.junit.Test

class LongPollCursorTest {
    @Test
    fun `initial is null`() {
        assertEquals(null, LongPollCursor.INITIAL.value)
    }

    @Test
    fun `value checks`() {
        val c1 = LongPollCursor("abc")
        assertEquals("abc", c1.value)

        val c2 = LongPollCursor("abc")
        assertEquals(c1, c2)
    }
}
