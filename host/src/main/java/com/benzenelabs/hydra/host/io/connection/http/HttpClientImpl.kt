package com.benzenelabs.hydra.host.io.connection.http

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [HttpClient] backed by OkHttp.
 */
class HttpClientImpl(
    private val okHttpClient: OkHttpClient
) : HttpClient {

    private val callsByKey = ConcurrentHashMap<String, CopyOnWriteArraySet<Call>>()

    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val client = if (request.timeoutMs > 0) {
            okHttpClient.newBuilder()
                .callTimeout(request.timeoutMs, TimeUnit.MILLISECONDS)
                .build()
        } else {
            okHttpClient
        }

        val okhttpRequest = buildRequest(request)
        val call = client.newCall(okhttpRequest)

        request.cancellationKey?.let { key ->
            callsByKey.getOrPut(key) { CopyOnWriteArraySet() }.add(call)
        }

        try {
            call.execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: ByteArray(0)
                val headers = response.headers.toMultimap()
                    .mapValues { (_, values) -> values.joinToString(",") }
                HttpResponse(
                    statusCode = response.code,
                    headers = headers,
                    body = bodyBytes,
                    contentType = response.body?.contentType()?.toString()
                )
            }
        } catch (e: IOException) {
            if (call.isCanceled()) {
                throw HttpCancelledException(request.cancellationKey ?: request.url)
            }
            throw HttpRequestException(request.url, e)
        } finally {
            request.cancellationKey?.let { key ->
                callsByKey[key]?.let { set ->
                    set.remove(call)
                    if (set.isEmpty()) {
                        callsByKey.remove(key, set)
                    }
                }
            }
        }
    }

    override fun cancel(cancellationKey: String) {
        val calls = callsByKey.remove(cancellationKey) ?: return
        calls.forEach { it.cancel() }
    }

    private fun buildRequest(request: HttpRequest): Request {
        val body = when {
            request.body == null -> null
            else -> request.body.toRequestBody(request.contentType?.toMediaTypeOrNull())
        }

        return Request.Builder()
            .url(request.url)
            .apply {
                request.headers.forEach { (key, value) -> addHeader(key, value) }
                when (request.method) {
                    HttpMethod.GET -> get()
                    HttpMethod.POST -> post(body ?: ByteArray(0).toRequestBody(null))
                    HttpMethod.PUT -> put(body ?: ByteArray(0).toRequestBody(null))
                    HttpMethod.PATCH -> patch(body ?: ByteArray(0).toRequestBody(null))
                    HttpMethod.DELETE -> {
                        if (body == null) delete() else delete(body)
                    }
                    HttpMethod.HEAD -> head()
                }
            }
            .build()
    }
}
