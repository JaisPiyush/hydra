package com.benzenelabs.hydra.host.io.fakes

import com.benzenelabs.hydra.host.io.connection.http.HttpCancelledException
import com.benzenelabs.hydra.host.io.connection.http.HttpClient
import com.benzenelabs.hydra.host.io.connection.http.HttpRequest
import com.benzenelabs.hydra.host.io.connection.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay

class FakeHttpClient : HttpClient {

    val requests = mutableListOf<HttpRequest>()
    var nextResponse: HttpResponse = HttpResponse(200)
    var nextException: Throwable? = null
    var delayMs: Long = 0

    private val cancelledKeys = ConcurrentHashMap.newKeySet<String>()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        if (request.cancellationKey != null && cancelledKeys.contains(request.cancellationKey)) {
            throw HttpCancelledException(request.cancellationKey)
        }

        requests.add(request)

        if (delayMs > 0) delay(delayMs)

        // check cancellation again after delay
        if (request.cancellationKey != null && cancelledKeys.contains(request.cancellationKey)) {
            throw HttpCancelledException(request.cancellationKey)
        }

        nextException?.let { throw it }
        return nextResponse
    }

    override fun cancel(cancellationKey: String) {
        cancelledKeys.add(cancellationKey)
    }

    fun queueResponse(response: HttpResponse) {
        nextResponse = response
        nextException = null
    }

    fun queueError(error: Throwable) {
        nextException = error
    }
}
