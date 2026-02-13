package com.benzenelabs.hydra.host.io.connection.longpoll

import app.cash.turbine.test
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.ConnectionType
import com.benzenelabs.hydra.host.io.connection.http.HttpResponse
import com.benzenelabs.hydra.host.io.fakes.FakeConfigStore
import com.benzenelabs.hydra.host.io.fakes.FakeHttpClient
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LongPollConnectionImplTest {

    @Test
    fun `poll loop emits frames`() = runTest {
        val configStore = FakeConfigStore()
        val httpClient = FakeHttpClient()
        val scope = TestScope(StandardTestDispatcher())

        val channelId = ChannelId("ch.poll")
        val config =
                LongPollConfig(
                        url = "http://poll.com/{cursor}",
                        intervalMs = 1000L,
                        cursorConfigKey = "cursor"
                )

        val handle =
                ConnectionHandle(
                        id = UUID.randomUUID().toString(),
                        type = ConnectionType.LONG_POLL,
                        channelId = channelId,
                        remoteAddress = config.url,
                        state = ConnectionState.IDLE
                )

        // Setup initial cursor
        configStore.put(StorageScope.Extension(channelId), "cursor", "123")

        // Setup response
        val responseBody = "1234".toByteArray()
        httpClient.queueResponse(HttpResponse(200, body = responseBody))

        val connection =
                LongPollConnectionImpl(
                        handle = handle,
                        config = config,
                        channelId = channelId,
                        configStore = configStore,
                        httpClient = httpClient,
                        scope = scope
                )

        connection.frames.test {
            connection.open()

            // Should poll immediately
            scope.advanceTimeBy(100)

            val frame = awaitItem()
            assertEquals("123", httpClient.requests.last().url.split("/").last())
            assertEquals("1234", frame.payloadAsText()) // Simple check it's the right frame

            connection.close()
        }
    }
}
