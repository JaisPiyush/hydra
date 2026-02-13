package com.benzenelabs.hydra.host.io.routing

import com.benzenelabs.hydra.host.channel.ChannelId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboundConnectionRouterImplTest {

    private val router = InboundConnectionRouterImpl()
    private val ch1 = ChannelId("ch.1")
    private val addr1 = RemoteAddress("1.1.1.1", 1000)

    @Test
    fun `register and resolve`() {
        router.register(addr1, ch1)
        assertEquals(ch1, router.resolve(addr1))
    }

    @Test
    fun `resolve unknown returns null`() {
        assertNull(router.resolve(addr1))
    }

    @Test
    fun `unregister`() {
        router.register(addr1, ch1)
        router.unregister(addr1)
        assertNull(router.resolve(addr1))
    }

    @Test
    fun `activeConnections`() {
        val addr2 = RemoteAddress("1.1.1.1", 1001)
        router.register(addr1, ch1)
        router.register(addr2, ch1)

        val active = router.activeConnections(ch1)
        assertEquals(2, active.size)
        assertTrue(active.contains(addr1))
        assertTrue(active.contains(addr2))
    }

    @Test
    fun `unregisterAll`() {
        router.register(addr1, ch1)
        router.register(RemoteAddress("2.2.2.2", 2000), ChannelId("ch.2"))

        router.unregisterAll(ch1)

        assertNull(router.resolve(addr1))
        assertEquals(ChannelId("ch.2"), router.resolve(RemoteAddress("2.2.2.2", 2000)))
    }
}
