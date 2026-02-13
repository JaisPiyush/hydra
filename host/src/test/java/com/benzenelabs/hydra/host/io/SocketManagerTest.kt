package com.benzenelabs.hydra.host.io

import app.cash.turbine.test
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import com.benzenelabs.hydra.host.io.SocketManagerImpl
import com.benzenelabs.hydra.host.io.SocketProtocol
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.robolectric.RobolectricTestRunner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SocketManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var socketManager: SocketManagerImpl
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
//        socketManager = SocketManagerImpl(testScope)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connect websocket success`() = runTest {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                webSocket.close(code, reason) // respond properly
            }
        }))

        val cid = ChannelId("com.test")
        val url = server.url("/ws").toString()
//            .replace("http://", "ws://") // http://... but OkHttp handles ws:// upgrade
        socketManager = SocketManagerImpl(this)

        // Subscribe before connecting to ensure we catch the Open event
        socketManager.connectionEvents(cid).test {
            val handle = socketManager.connect(cid, url, SocketProtocol.WEBSOCKET)

            assertNotNull(handle)
            assertEquals(url, handle.url)

            // First event should be Opened
            val event = awaitItem()
            assertTrue(event is ConnectionEvent.Opened)
            assertEquals(handle.id, (event as ConnectionEvent.Opened).socketId)

            // Close to finish cleanly
            socketManager.close(handle, "Done")
            val closeEvent = awaitItem()
            assertTrue(closeEvent is ConnectionEvent.Closed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events flow test`() = runTest {
        val cid = ChannelId("com.testflow")
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                webSocket.close(code, reason) // respond properly
            }
        }))
        val url = server.url("/ws").toString()
        socketManager = SocketManagerImpl(this)

        socketManager.connectionEvents(cid).test(
                        timeout = 5.seconds
                ) {
            val handle = socketManager.connect(cid, url, SocketProtocol.WEBSOCKET)

            val openEvent = awaitItem()
            assertTrue(openEvent is ConnectionEvent.Opened)
            assertEquals(handle.id, (openEvent as ConnectionEvent.Opened).socketId)

            socketManager.close(handle, "Test")

            val closeEvent = awaitItem()
            assertTrue(closeEvent is ConnectionEvent.Closed)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
