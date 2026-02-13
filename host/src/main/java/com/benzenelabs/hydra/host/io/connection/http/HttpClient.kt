package com.benzenelabs.hydra.host.io.connection.http

/**
 * Outbound HTTP client for REST and long-poll requests.
 */
interface HttpClient {

    /**
     * Executes [request] and returns a complete [HttpResponse].
     *
     * Error HTTP statuses are returned normally and are not thrown.
     *
     * @throws HttpRequestException on network failure/timeout.
     * @throws HttpCancelledException if cancelled via [cancel].
     */
    suspend fun execute(request: HttpRequest): HttpResponse

    /** Cancels in-flight requests for [cancellationKey]. Idempotent. */
    fun cancel(cancellationKey: String)
}

/** HTTP network execution failure. */
class HttpRequestException(url: String, cause: Throwable? = null) :
    RuntimeException("HTTP request failed: $url", cause)

/** HTTP request cancellation failure. */
class HttpCancelledException(cancellationKey: String) :
    RuntimeException("HTTP request cancelled: $cancellationKey")
