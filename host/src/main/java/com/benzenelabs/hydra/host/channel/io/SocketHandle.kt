package com.benzenelabs.hydra.host.channel.io

import java.util.UUID

/**
 * A handle to an open socket connection managed by [SocketManager].
 */
data class SocketHandle(
    val id: String,
    val url: String,
    val protocol: SocketProtocol
) {
    init {
        require(id.isNotBlank()) { "SocketHandle.id must not be blank" }
        require(url.isNotBlank()) { "SocketHandle.url must not be blank" }
        UUID.fromString(id)
    }
}

enum class SocketProtocol {
    WEBSOCKET,
    TCP
}
