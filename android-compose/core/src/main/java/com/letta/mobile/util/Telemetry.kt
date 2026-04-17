package com.letta.mobile.util

import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Telemetry — unified event stream for latency, lifecycle, and errors.
 *
 * Design goals:
 * - **Zero setup for callers.** Call [Telemetry.event] from anywhere, no DI.
 * - **Structured data.** Events carry a tag (subsystem), name (verb),
 *   optional duration, and a bag of key/value attributes.
 * - **Multiple sinks.** Always logs to Logcat; also keeps the most recent
 *   1000 events in a ring buffer so a Dev screen can display them.
 * - **Cheap.** Producing an event is lock-free (ConcurrentLinkedDeque). A
 *   single AtomicBoolean gates emission entirely for release builds.
 *
 * Usage patterns:
 *
 * ```
 * // Simple point event
 * Telemetry.event("TimelineSync", "localAppended", "otid" to otid)
 *
 * // Measured block
 * val result = Telemetry.measure("TimelineSync", "hydrate") {
 *     messageApi.listConversationMessages(...)
 * }
 *
 * // Manual timer (when the end is in a different coroutine/callback)
 * val timer = Telemetry.startTimer("Send", "roundtrip")
 * // ... later ...
 * timer.stop("otid" to otid, "events" to eventCount)
 * ```
 */
object Telemetry {
    /**
     * Master switch. Flip to `false` in release builds to make every call a
     * no-op (single volatile read, no allocation).
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val enabled = AtomicBoolean(true)

    private const val TAG_PREFIX = "Telemetry"
    private const val MAX_RING_SIZE = 1000

    private val ring = ConcurrentLinkedDeque<Event>()
    private val _eventsFlow = MutableStateFlow<List<Event>>(emptyList())

    /**
     * Snapshot of the last N events (newest first). Backed by a StateFlow so
     * the dev UI can observe without polling.
     */
    val events: StateFlow<List<Event>> = _eventsFlow.asStateFlow()

    /** Record a single event. Optional [durationMs] for phase measurements. */
    fun event(
        tag: String,
        name: String,
        vararg attrs: Pair<String, Any?>,
        durationMs: Long? = null,
        level: Level = Level.INFO,
    ) {
        if (!enabled.get()) return

        val ev = Event(
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            name = name,
            durationMs = durationMs,
            attrs = attrs.toMap(),
            level = level,
        )
        emit(ev)
    }

    /** Record an error event with throwable detail. */
    fun error(
        tag: String,
        name: String,
        throwable: Throwable,
        vararg attrs: Pair<String, Any?>,
    ) {
        if (!enabled.get()) return

        val allAttrs = attrs.toMutableList()
        allAttrs += ("errorClass" to throwable.javaClass.simpleName)
        allAttrs += ("errorMessage" to (throwable.message ?: "<none>"))
        val ev = Event(
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            name = name,
            durationMs = null,
            attrs = allAttrs.toMap(),
            level = Level.ERROR,
            throwable = throwable,
        )
        emit(ev)
    }

    /** Run [block] and record a timed event. */
    inline fun <T> measure(
        tag: String,
        name: String,
        vararg attrs: Pair<String, Any?>,
        block: () -> T,
    ): T {
        val start = System.currentTimeMillis()
        return try {
            val result = block()
            event(tag, name, *attrs, durationMs = System.currentTimeMillis() - start)
            result
        } catch (t: Throwable) {
            error(tag, "$name:failed", t, "durationMs" to (System.currentTimeMillis() - start), *attrs)
            throw t
        }
    }

    /** Start a manual timer. Call [Timer.stop] when the phase completes. */
    fun startTimer(tag: String, name: String): Timer = Timer(tag, name, System.currentTimeMillis())

    /** Clear the in-memory ring buffer (does not affect Logcat history). */
    fun clear() {
        ring.clear()
        _eventsFlow.value = emptyList()
    }

    fun snapshot(): List<Event> = _eventsFlow.value

    private fun emit(ev: Event) {
        // Logcat side channel — always.
        val tagStr = "$TAG_PREFIX/${ev.tag}"
        val body = buildString {
            append(ev.name)
            if (ev.durationMs != null) append(" (${ev.durationMs}ms)")
            if (ev.attrs.isNotEmpty()) {
                append(" ")
                ev.attrs.entries.joinTo(this, separator = " ") { (k, v) -> "$k=$v" }
            }
        }
        when (ev.level) {
            Level.DEBUG -> Log.d(tagStr, body)
            Level.INFO -> Log.i(tagStr, body)
            Level.WARN -> Log.w(tagStr, body)
            Level.ERROR -> if (ev.throwable != null) Log.e(tagStr, body, ev.throwable) else Log.e(tagStr, body)
        }

        // Ring buffer.
        ring.addFirst(ev)
        while (ring.size > MAX_RING_SIZE) ring.pollLast()
        _eventsFlow.value = ring.toList()
    }

    /**
     * Serialize the ring to a human-readable string. For sharing from the
     * dev UI (clipboard / file export).
     */
    fun exportText(): String = buildString {
        append("# Letta Mobile Telemetry Dump (${ring.size} events)\n")
        append("# Generated: ${java.time.Instant.now()}\n\n")
        ring.forEach { ev ->
            append(ev.toLine())
            append('\n')
        }
    }

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class Event(
        val timestampMs: Long,
        val tag: String,
        val name: String,
        val durationMs: Long?,
        val attrs: Map<String, Any?>,
        val level: Level,
        val throwable: Throwable? = null,
    ) {
        fun toLine(): String = buildString {
            append(java.time.Instant.ofEpochMilli(timestampMs))
            append(" [").append(level).append("] ")
            append(tag).append('/').append(name)
            if (durationMs != null) append(" (${durationMs}ms)")
            if (attrs.isNotEmpty()) {
                append(" {")
                attrs.entries.joinTo(this, separator = ", ") { (k, v) -> "$k=$v" }
                append("}")
            }
            throwable?.let {
                append("\n  error: ")
                append(it.javaClass.simpleName).append(": ").append(it.message)
            }
        }
    }

    class Timer internal constructor(
        private val tag: String,
        private val name: String,
        private val startMs: Long,
    ) {
        fun stop(vararg attrs: Pair<String, Any?>) {
            event(tag, name, *attrs, durationMs = System.currentTimeMillis() - startMs)
        }

        fun stopError(throwable: Throwable, vararg attrs: Pair<String, Any?>) {
            error(
                tag,
                "$name:failed",
                throwable,
                "durationMs" to (System.currentTimeMillis() - startMs),
                *attrs,
            )
        }
    }
}
