package com.benzenelabs.hydra.host.channel

import app.cash.turbine.test
import com.benzenelabs.hydra.host.channel.fakes.FakeChannelBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelRegistryTest {

        private val testDispatcher = UnconfinedTestDispatcher()
        private val testScope = TestScope(testDispatcher)
        private val registry = ChannelRegistryImpl(testScope)

        @Test
        fun `register and find`() =
                testScope.runTest {
                        val cid = ChannelId("com.example")
                        val meta = createMetadata(cid)
                        val bridge = FakeChannelBridge(cid)

                        registry.register(meta, bridge)

                        val reg = registry.find(cid)
                        assertNotNull(reg)
                        assertEquals(ChannelState.REGISTERED, reg?.state)
                        assertEquals(meta, reg?.metadata)

                        assertEquals(bridge, registry.getBridge(cid))

                        // Cleanup
                        registry.unregister(cid)
                }

        @Test(expected = ChannelAlreadyRegisteredException::class)
        fun `register duplicate throws`() =
                testScope.runTest {
                        val cid = ChannelId("com.example")
                        registry.register(createMetadata(cid), FakeChannelBridge(cid))
                        registry.register(createMetadata(cid), FakeChannelBridge(cid))
                }

        @Test
        fun `unregister removes`() =
                testScope.runTest {
                        val cid = ChannelId("com.example")
                        registry.register(createMetadata(cid), FakeChannelBridge(cid))

                        registry.unregister(cid)

                        assertNull(registry.find(cid))
                        assertNull(registry.getBridge(cid))
                }

        @Test
        fun `unregister is idempotent`() =
                testScope.runTest { registry.unregister(ChannelId("com.unknown")) }

        @Test
        fun `findByState returns matching`() =
                testScope.runTest {
                        val c1 = ChannelId("com.c1")
                        val c2 = ChannelId("com.c2")
                        registry.register(createMetadata(c1), FakeChannelBridge(c1))
                        registry.register(createMetadata(c2), FakeChannelBridge(c2))

                        registry.updateState(c1, ChannelState.CONNECTING)

                        val connecting = registry.findByState(ChannelState.CONNECTING)
                        assertEquals(1, connecting.size)
                        assertEquals(c1, connecting[0].channelId)

                        val registered = registry.findByState(ChannelState.REGISTERED)
                        assertEquals(1, registered.size)
                        assertEquals(c2, registered[0].channelId)

                        registry.unregister(c1)
                        registry.unregister(c2)
                }

        @Test
        fun `updateState valid transition`() =
                testScope.runTest {
                        val cid = ChannelId("com.c1")
                        registry.register(createMetadata(cid), FakeChannelBridge(cid))

                        registry.updateState(cid, ChannelState.CONNECTING)
                        assertEquals(ChannelState.CONNECTING, registry.find(cid)?.state)

                        registry.updateState(cid, ChannelState.AUTHENTICATING)
                        assertEquals(ChannelState.AUTHENTICATING, registry.find(cid)?.state)

                        registry.unregister(cid)
                }

        @Test(expected = InvalidChannelTransitionException::class)
        fun `updateState invalid transition throws`() =
                testScope.runTest {
                        val cid = ChannelId("com.c1")
                        registry.register(createMetadata(cid), FakeChannelBridge(cid))

                        try {
                                // REGISTERED -> CONNECTED is invalid
                                registry.updateState(cid, ChannelState.CONNECTED)
                        } finally {
                                registry.unregister(cid)
                        }
                }

        @Test
        fun `observeAll emits updates`() =
                testScope.runTest {
                        registry.observeAll().test {
                                assertEquals(emptyList<ChannelRegistration>(), awaitItem())

                                val cid = ChannelId("com.c1")
                                registry.register(createMetadata(cid), FakeChannelBridge(cid))
                                val list1 = awaitItem()
                                assertEquals(1, list1.size)
                                assertEquals(ChannelState.REGISTERED, list1[0].state)

                                registry.updateState(cid, ChannelState.CONNECTING)
                                val list2 = awaitItem()
                                assertEquals(ChannelState.CONNECTING, list2[0].state)

                                registry.unregister(cid)
                                val list3 = awaitItem()
                                assertTrue(list3.isEmpty())

                                cancelAndIgnoreRemainingEvents()
                        }
                }

        @Test
        fun `events flow forwards from bridge`() =
                testScope.runTest {
                        val cid = ChannelId("com.c1")
                        val bridge = FakeChannelBridge(cid)
                        registry.register(createMetadata(cid), bridge)

                        registry.events.test {
                                // bridge emits logic
                                val msg =
                                        ChannelMessage(
                                                "m1",
                                                com.benzenelabs.hydra.host.data.session.SessionId
                                                        .generate(),
                                                cid,
                                                MessageDirection.OUTBOUND,
                                                MessageContent.Text("hi"),
                                                null,
                                                1000,
                                                null
                                        )
                                bridge.sendMessage(msg) // Fake bridge emits MessageSent

                                val event = awaitItem()
                                assertTrue(event is ChannelEvent.MessageSent)
                                assertEquals("m1", (event as ChannelEvent.MessageSent).message.id)

                                registry.unregister(cid)
                                cancelAndIgnoreRemainingEvents()
                        }
                }

        private fun createMetadata(id: ChannelId) =
                ChannelMetadata(id, "Name", "Desc", null, true, ChannelAuthType.NONE, emptySet())
}
