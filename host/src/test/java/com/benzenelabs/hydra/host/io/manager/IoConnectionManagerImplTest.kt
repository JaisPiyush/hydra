package com.benzenelabs.hydra.host.io.manager

import app.cash.turbine.test
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.ConnectionType
import com.benzenelabs.hydra.host.io.connection.http.HttpRequest
import com.benzenelabs.hydra.host.io.connection.http.HttpResponse
import com.benzenelabs.hydra.host.io.fakes.FakeConfigStore
import com.benzenelabs.hydra.host.io.fakes.FakeHttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class IoConnectionManagerImplTest {

    @Test
    fun `openPipe registers handle and reaches connected`() = runTest {
        val manager = IoConnectionManagerImpl(
            scope = backgroundScope,
            configStore = FakeConfigStore(),
            httpClient = FakeHttpClient()
        )

        val handle = manager.openPipe(ChannelId("c1.h"))

        assertEquals(ConnectionType.PIPE, handle.type)
        assertEquals(ConnectionState.CONNECTED, handle.state)
        assertEquals(ConnectionState.CONNECTED, manager.stateOf(handle))
    }

    @Test
    fun `send on open pipe does not throw`() = runTest {
        val manager = IoConnectionManagerImpl(
            scope = backgroundScope,
            configStore = FakeConfigStore(),
            httpClient = FakeHttpClient()
        )

        val handle = manager.openPipe(ChannelId("c1.h"))

        manager.send(handle, byteArrayOf(1, 2, 3))
    }

    @Test
    fun `closeAll removes all handles for one channel`() = runTest {
        val manager = IoConnectionManagerImpl(
            scope = backgroundScope,
            configStore = FakeConfigStore(),
            httpClient = FakeHttpClient()
        )

        val h1 = manager.openPipe(ChannelId("c1.h"))
        val h2 = manager.openPipe(ChannelId("c1.h"))
        val h3 = manager.openPipe(ChannelId("c2.h"))

        manager.closeAll(ChannelId("c1.h"))

        assertNull(manager.stateOf(h1))
        assertNull(manager.stateOf(h2))
        assertEquals(ConnectionState.CONNECTED, manager.stateOf(h3))
    }

    @Test
    fun `connectionStateUpdates emits updates for open connection`() = runTest {
        val manager = IoConnectionManagerImpl(
            scope = backgroundScope,
            configStore = FakeConfigStore(),
            httpClient = FakeHttpClient()
        )

        manager.connectionStateUpdates.test {
            val handle = manager.openPipe(ChannelId("c1.h"))
            runCurrent()

            val update1 = awaitItem()
            assertEquals(handle.id, update1.id)

            if (update1.state != ConnectionState.CONNECTED) {
                val maybeSecond = awaitItem()
                assertEquals(handle.id, maybeSecond.id)
                assertEquals(ConnectionState.CONNECTED, maybeSecond.state)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `http delegates to http client`() = runTest {
        val fakeHttpClient = FakeHttpClient().apply {
            queueResponse(HttpResponse(statusCode = 202, body = "ok".toByteArray()))
        }

        val manager = IoConnectionManagerImpl(
            scope = backgroundScope,
            configStore = FakeConfigStore(),
            httpClient = fakeHttpClient
        )

        val response = manager.http(HttpRequest(url = "https://example.com"))

        assertEquals(202, response.statusCode)
        assertEquals("https://example.com", fakeHttpClient.requests.single().url)
    }
}
