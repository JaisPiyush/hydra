package com.benzenelabs.hydra.host.channel

import com.benzenelabs.hydra.host.data.session.SessionId
import com.benzenelabs.hydra.host.data.session.SessionState
import org.junit.Test

class ChannelEventTest {

    @Test
    fun `verify subtypes`() {
        val cid = ChannelId("com.example")
        val sid = SessionId.generate()

        ChannelEvent.StateChanged(cid, ChannelState.REGISTERED, ChannelState.CONNECTING, null)

        // MessageReceived
        val msg =
                ChannelMessage(
                        "m1",
                        sid,
                        cid,
                        MessageDirection.INBOUND,
                        MessageContent.Text("Hi"),
                        null,
                        1000,
                        null
                )
        ChannelEvent.MessageReceived(msg)
        ChannelEvent.MessageSent(msg)

        ChannelEvent.MessageSendFailed(sid, cid, "Timeout", RuntimeException("bang"))

        ChannelEvent.SessionCreated(sid, cid)
        ChannelEvent.SessionStateChanged(sid, cid, SessionState.ACTIVE)

        ChannelEvent.AuthRequired(cid, AuthRequest.TokenEntry("Enter token"))
    }
}
