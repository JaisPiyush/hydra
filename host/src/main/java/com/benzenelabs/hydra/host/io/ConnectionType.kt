package com.benzenelabs.hydra.host.io

/** Transport type managed by the host I/O layer. */
enum class ConnectionType {
    /** Outbound WebSocket (e.g. WhatsApp Web, Discord Gateway). */
    WEBSOCKET,
    /** Outbound raw TCP socket (e.g. Telegram MTProto). */
    TCP,
    /** Host-managed HTTP long-poll loop. */
    LONG_POLL,
    /** In-process Kotlin coroutine channel for app chat integration. */
    PIPE,
    /** Single outbound HTTP request (non-persistent). */
    HTTP
}
