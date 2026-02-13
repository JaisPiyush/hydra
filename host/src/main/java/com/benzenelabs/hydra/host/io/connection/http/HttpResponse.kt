package com.benzenelabs.hydra.host.io.connection.http

/**
 * HTTP response model for [HttpClient].
 */
data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
    val contentType: String? = null
) {
    /** True for 2xx status codes. */
    val isSuccess: Boolean get() = statusCode in 200..299

    /** True for 4xx status codes. */
    val isClientError: Boolean get() = statusCode in 400..499

    /** True for 5xx status codes. */
    val isServerError: Boolean get() = statusCode in 500..599

    /** Decodes response body as UTF-8 text. */
    fun bodyAsText(): String = body.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpResponse) return false
        return statusCode == other.statusCode &&
            headers == other.headers &&
            body.contentEquals(other.body) &&
            contentType == other.contentType
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        return result
    }
}
