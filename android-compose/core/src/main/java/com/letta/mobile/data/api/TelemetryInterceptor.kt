package com.letta.mobile.data.api

import com.letta.mobile.util.Telemetry
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that records one Telemetry event per HTTP round-trip.
 *
 * Emits `Http` / `request` with attributes:
 *   - method        (GET/POST/…)
 *   - path          (everything after /v1 to keep noise low)
 *   - status        (HTTP status code)
 *   - durationMs    (wire time)
 *   - bytes         (response content length if known)
 *
 * Errors (IOException, etc.) are emitted as `Http` / `request:failed`.
 *
 * This lets the dev screen answer questions like:
 *   - What is the p50/p95 latency for GET /runs/{id}/steps?
 *   - How many concurrent requests are in-flight at any moment?
 *   - Which screen fired this burst of traffic?
 *
 * Low-overhead: just two System.currentTimeMillis() calls + a single Event
 * allocation. Safe to leave on in dev builds; controlled by Telemetry.enabled.
 */
internal object TelemetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val start = System.currentTimeMillis()

        val response = try {
            chain.proceed(request)
        } catch (t: Throwable) {
            Telemetry.error(
                "Http", "request:failed", t,
                "method" to method,
                "path" to path,
                "durationMs" to (System.currentTimeMillis() - start),
            )
            throw t
        }

        val duration = System.currentTimeMillis() - start
        val bytes = response.header("Content-Length")?.toLongOrNull() ?: -1L
        val level = if (response.code >= 400) Telemetry.Level.WARN else Telemetry.Level.DEBUG

        Telemetry.event(
            "Http", "request",
            "method" to method,
            "path" to path,
            "status" to response.code,
            "bytes" to bytes,
            durationMs = duration,
            level = level,
        )

        return response
    }
}
