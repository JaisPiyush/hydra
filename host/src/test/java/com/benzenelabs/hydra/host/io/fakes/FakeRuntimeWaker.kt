package com.benzenelabs.hydra.host.io.fakes

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.data.session.SessionId
import com.benzenelabs.hydra.host.io.dispatch.RuntimeWaker

class FakeRuntimeWaker : RuntimeWaker {

    val wakeCalls = mutableListOf<Pair<SessionId, ChannelId>>()

    override fun wake(sessionId: SessionId, channelId: ChannelId) {
        wakeCalls.add(sessionId to channelId)
    }
}
