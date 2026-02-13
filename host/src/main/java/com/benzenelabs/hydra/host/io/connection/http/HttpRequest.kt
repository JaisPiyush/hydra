package com.benzenelabs.hydra.host.io.connection.http

/**
 * Outbound HTTP request model.
 */
data class HttpRequest(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val contentType: String? = null,
    val timeoutMs: Long = 0L,
    val cancellationKey: String? = null
) {
    init {
        require(url.isNotBlank()) { "HttpRequest.url must not be blank" }
        if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
            require(body == null) { "$method requests must not have a body" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpRequest) return false
        return url == other.url &&
            method == other.method &&
            headers == other.headers &&
            ((body == null && other.body == null) || (body != null && other.body != null && body.contentEquals(other.body))) &&
            contentType == other.contentType &&
            timeoutMs == other.timeoutMs &&
            cancellationKey == other.cancellationKey
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + timeoutMs.hashCode()
        result = 31 * result + (cancellationKey?.hashCode() ?: 0)
        return result
    }
}
