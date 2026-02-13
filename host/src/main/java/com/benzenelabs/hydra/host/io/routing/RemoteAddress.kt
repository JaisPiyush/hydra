package com.benzenelabs.hydra.host.io.routing

/**
 * IP host and port of an inbound client connection.
 */
data class RemoteAddress(val host: String, val port: Int) {
    init {
        require(host.isNotBlank()) { "RemoteAddress.host must not be blank" }
        require(port in 1..65535) { "RemoteAddress.port must be 1..65535, got $port" }
    }

    override fun toString(): String = "$host:$port"

    companion object {
        /** Parses a host:port tuple. */
        fun parse(value: String): RemoteAddress {
            val idx = value.lastIndexOf(':')
            require(idx > 0) { "Invalid remote address: $value" }
            val host = value.substring(0, idx)
            val port = value.substring(idx + 1).toIntOrNull()
                ?: throw IllegalArgumentException("Invalid port in: $value")
            return RemoteAddress(host, port)
        }
    }
}
