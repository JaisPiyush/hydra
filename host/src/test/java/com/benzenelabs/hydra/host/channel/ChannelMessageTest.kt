package com.benzenelabs.hydra.host.channel

import com.benzenelabs.hydra.host.data.session.SessionId
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChannelMessageTest {

    @Test
    fun `generateId produces distinct values`() {
        assertNotEquals(ChannelMessage.generateId(), ChannelMessage.generateId())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id throws`() {
        createMessage(id = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `timestamp zero throws`() {
        createMessage(timestamp = 0)
    }

    @Test
    fun `Text content`() {
        MessageContent.Text("Hello")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Text empty body throws`() {
        MessageContent.Text("")
    }

    @Test
    fun `File content`() {
        MessageContent.File("blob1", "doc.pdf", "application/pdf")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `File blank fileName throws`() {
        MessageContent.File("blob1", "  ", "mime")
    }

    @Test
    fun `Reaction content`() {
        MessageContent.Reaction("👍", "msg1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Reaction blank emoji throws`() {
        MessageContent.Reaction("  ", "msg1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Reaction blank targetId throws`() {
        MessageContent.Reaction("👍", "  ")
    }

    @Test
    fun `Deletion content`() {
        MessageContent.Deletion("msg1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Deletion blank targetId throws`() {
        MessageContent.Deletion("  ")
    }

    @Test
    fun `Event content`() {
        MessageContent.Event("typing", "on")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Event blank type throws`() {
        MessageContent.Event("  ", "payload")
    }

    private fun createMessage(
            id: String = "msg1",
            timestamp: Long = 1000,
            content: MessageContent = MessageContent.Text("Hi")
    ) =
            ChannelMessage(
                    id = id,
                    sessionId = SessionId.generate(),
                    channelId = ChannelId("com.c1"),
                    direction = MessageDirection.INBOUND,
                    content = content,
                    replyToId = null,
                    timestamp = timestamp,
                    metadata = null
            )
}
