package com.benzenelabs.hydra.host.channel

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelIdTest {
    @Test
    fun `is alias for ContributionId`() {
        val cid = ChannelId("foo.bar")
        assertEquals("foo", cid.author)
        assertEquals("bar", cid.name)
        assertEquals("foo.bar", cid.value)
    }
}
