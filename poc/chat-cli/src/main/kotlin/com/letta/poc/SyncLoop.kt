package com.letta.poc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * The ONE place that mutates the Timeline.
 *
 * Responsibilities:
 * - Apply local sends (append Local event)
 * - Process stream events (append Confirmed events, swap Local→Confirmed on otid match)
 * - Reconcile with GET /messages after each send
 *
 * Anti-requirements (things it must NOT do):
 * - Must not call any sort function on the timeline
 * - Must not use content hashing for matching
 * - Must not have multiple parallel write paths
 */
class SyncLoop(
    private val api: LettaApi,
    private val conversationId: String,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(Timeline(conversationId))
    val state: StateFlow<Timeline> = _state.asStateFlow()

    // Serialize all mutations so append/replace logic is safe.
    private val writeMutex = Mutex()

    // Queue of outgoing sends. Letta API only allows ONE request per conversation at a time
    // (returns 409 Conflict otherwise). This queue serializes sends.
    private val sendQueue = Channel<PendingSend>(Channel.UNLIMITED)

    init {
        scope.launch { processSendQueue() }
    }

    private val _events = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private data class PendingSend(val otid: String, val content: String)

    /** Load initial history. */
    suspend fun hydrate(limit: Int = 50) = writeMutex.withLock {
        val messages = api.listMessages(conversationId, limit = limit, order = "asc")
        val converted = messages.mapIndexedNotNull { idx, msg -> msg.toConfirmed(idx.toDouble()) }
        _state.value = Timeline(
            conversationId = conversationId,
            events = converted,
            liveCursor = converted.lastOrNull()?.serverId,
        )
        _events.emit(SyncEvent.Hydrated(converted.size))
    }

    /**
     * Send a message. Returns the otid immediately; the full response flows via [state].
     *
     * The Local event is appended IMMEDIATELY so the UI can show it, but the actual
     * HTTP request is queued because the Letta server only accepts one request per
     * conversation at a time (409 Conflict otherwise).
     */
    suspend fun send(content: String): String {
        val otid = newOtid()

        // Atomically: reserve position, append Local, AND enqueue — all under one lock.
        // This guarantees that the timeline position and the queue order match,
        // even under concurrent sends from multiple coroutines.
        writeMutex.withLock {
            val local = TimelineEvent.Local(
                position = _state.value.nextLocalPosition(),
                otid = otid,
                content = content,
                role = Role.USER,
                sentAt = Instant.now(),
                deliveryState = DeliveryState.SENDING,
            )
            _state.value = _state.value.append(local)
            sendQueue.send(PendingSend(otid, content))  // unlimited capacity — never suspends
        }
        _events.emit(SyncEvent.LocalAppended(otid))

        return otid
    }

    /** Background worker: processes one send at a time from the queue. */
    private suspend fun processSendQueue() {
        for (pending in sendQueue) {
            try {
                streamAndReconcile(pending.content, pending.otid)
            } catch (e: Exception) {
                _events.emit(SyncEvent.StreamError("queue-worker", e.message ?: "unknown"))
                writeMutex.withLock {
                    _state.value = _state.value.markFailed(pending.otid)
                }
            }
        }
    }

    private suspend fun streamAndReconcile(content: String, otid: String) {
        try {
            // 1. Open stream — each event appends a Confirmed event
            api.streamSend(conversationId, content, otid).collect { event ->
                when (event) {
                    is StreamEvent.Ping -> {
                        // No state change
                    }
                    is StreamEvent.Message -> {
                        writeMutex.withLock {
                            val confirmed = event.message.toConfirmed(_state.value.nextLocalPosition())
                                ?: return@withLock
                            // Dedupe: if this otid already exists, skip (e.g., reconcile fetched it)
                            if (_state.value.findByOtid(confirmed.otid) != null) return@withLock
                            _state.value = _state.value.append(confirmed)
                        }
                        _events.emit(SyncEvent.ServerEvent(event.message))
                    }
                    is StreamEvent.StopReason -> {
                        _events.emit(SyncEvent.StopReason(event.reason))
                    }
                    is StreamEvent.Error -> {
                        if (!event.isBenignCleanupError) {
                            _events.emit(SyncEvent.StreamError(event.type, event.message))
                        }
                    }
                }
            }

            // 2. Mark local send as SENT (the stream finished successfully)
            writeMutex.withLock {
                _state.value = _state.value.markSent(otid)
            }

            // 3. Fetch stored messages to reconcile our Local user event with the server-confirmed version
            reconcileAfterSend(otid)
        } catch (e: Exception) {
            writeMutex.withLock {
                _state.value = _state.value.markFailed(otid)
            }
            _events.emit(SyncEvent.StreamError("exception", e.message ?: "unknown"))
        }
    }

    /**
     * After a send completes, fetch recent messages and swap our Local user event
     * for the server-confirmed version (by otid match).
     *
     * Also pulls in any messages we missed (e.g., if the stream dropped).
     */
    private suspend fun reconcileAfterSend(otid: String) = writeMutex.withLock {
        try {
            val serverMessages = api.listMessages(conversationId, limit = 50, order = "desc").reversed()

            // 1. Find our user message by otid and replace the Local with Confirmed
            val myMatch = serverMessages.firstOrNull { it.otid == otid }
            if (myMatch != null) {
                val existingLocal = _state.value.findByOtid(otid)
                if (existingLocal is TimelineEvent.Local) {
                    val confirmed = myMatch.toConfirmed(existingLocal.position)
                    if (confirmed != null) {
                        _state.value = _state.value.replaceLocal(otid, confirmed)
                        _events.emit(SyncEvent.LocalConfirmed(otid, myMatch.id))
                    }
                }
            }

            // 2. Pull in any server messages we don't have yet (missed stream events, etc.)
            serverMessages.forEach { msg ->
                if (msg.otid != null && _state.value.findByOtid(msg.otid) == null) {
                    val pos = _state.value.nextLocalPosition()
                    val confirmed = msg.toConfirmed(pos) ?: return@forEach
                    _state.value = _state.value.append(confirmed)
                }
            }

            // 3. Update liveCursor
            serverMessages.lastOrNull()?.id?.let {
                _state.value = _state.value.copy(liveCursor = it)
            }
        } catch (e: Exception) {
            _events.emit(SyncEvent.ReconcileError(e.message ?: "unknown"))
        }
    }
}

/** Observable sync events for CLI/UI to subscribe to. */
sealed class SyncEvent {
    data class Hydrated(val messageCount: Int) : SyncEvent()
    data class LocalAppended(val otid: String) : SyncEvent()
    data class LocalConfirmed(val otid: String, val serverId: String) : SyncEvent()
    data class ServerEvent(val message: ServerMessage) : SyncEvent()
    data class StopReason(val reason: String) : SyncEvent()
    data class StreamError(val type: String, val message: String) : SyncEvent()
    data class ReconcileError(val message: String) : SyncEvent()
}

/** Convert a ServerMessage to a Confirmed timeline event. */
private fun ServerMessage.toConfirmed(position: Double): TimelineEvent.Confirmed? {
    val type = when (messageType) {
        "user_message" -> MessageType.USER
        "assistant_message" -> MessageType.ASSISTANT
        "reasoning_message" -> MessageType.REASONING
        "tool_call_message" -> MessageType.TOOL_CALL
        "tool_return_message" -> MessageType.TOOL_RETURN
        "system_message" -> MessageType.SYSTEM
        else -> return null  // ignore pings, stop reasons, etc.
    }
    // We need an otid even if server didn't set one (shouldn't happen for real messages)
    val effectiveOtid = otid ?: "server-$id"
    return TimelineEvent.Confirmed(
        position = position,
        otid = effectiveOtid,
        content = content,
        serverId = id,
        messageType = type,
        date = runCatching { Instant.parse(date) }.getOrElse { Instant.now() },
        runId = runId,
        stepId = stepId,
    )
}
