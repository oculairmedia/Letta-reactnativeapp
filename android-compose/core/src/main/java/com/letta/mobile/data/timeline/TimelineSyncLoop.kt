package com.letta.mobile.data.timeline

import android.util.Log
import com.letta.mobile.core.BuildConfig
import com.letta.mobile.data.api.MessageApi
import com.letta.mobile.util.Telemetry
import com.letta.mobile.data.model.AssistantMessage
import com.letta.mobile.data.model.LettaMessage
import com.letta.mobile.data.model.MessageContentPart
import com.letta.mobile.data.model.MessageCreate
import com.letta.mobile.data.model.MessageCreateRequest
import com.letta.mobile.data.model.buildContentParts
import com.letta.mobile.data.model.toJsonArray
import com.letta.mobile.data.model.ApprovalRequestMessage
import com.letta.mobile.data.model.ApprovalResponseMessage
import com.letta.mobile.data.model.EventMessage
import com.letta.mobile.data.model.HiddenReasoningMessage
import com.letta.mobile.data.model.PingMessage
import com.letta.mobile.data.model.ReasoningMessage
import com.letta.mobile.data.model.StopReason
import com.letta.mobile.data.model.SystemMessage
import com.letta.mobile.data.model.ToolCallMessage
import com.letta.mobile.data.model.ToolReturnMessage
import com.letta.mobile.data.model.UnknownMessage
import com.letta.mobile.data.model.UsageStatistics
import com.letta.mobile.data.model.UserMessage
import com.letta.mobile.data.stream.SseParser
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Single sync loop per conversation.
 *
 * This class is the ONLY place that mutates a [Timeline]. All other code
 * observes it via [state] (read-only) or emits commands via [send].
 *
 * Responsibilities:
 * - Accept outbound sends, optimistically append Local events, enqueue for transmission
 * - Serialize sends per-conversation (the Letta server rejects concurrent sends with 409)
 * - Stream assistant/reasoning/tool responses and append Confirmed events
 * - Reconcile with GET /messages to replace Local→Confirmed once the server echoes our otid
 *
 * Anti-requirements (things it must NOT do):
 * - Must not call any sort function on the timeline (order is preserved by position)
 * - Must not use content hashing for event matching (use otid only)
 * - Must not have multiple parallel write paths (single mutex-guarded mutator)
 *
 * See `docs/architecture/poc-validation-results.md` for scenario validation.
 */
class TimelineSyncLoop(
    private val messageApi: MessageApi,
    private val conversationId: String,
    private val scope: CoroutineScope,
    private val logTag: String = "TimelineSync",
) {
    private val previewJson = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    private val _state = MutableStateFlow(Timeline(conversationId))
    val state: StateFlow<Timeline> = _state.asStateFlow()

    // Serialize all mutations so append/replace logic is safe under concurrency.
    private val writeMutex = Mutex()

    // Queue of outgoing sends. Letta API returns 409 Conflict for concurrent
    // requests on the same conversation, so we must serialize client-side.
    private val sendQueue = Channel<PendingSend>(Channel.UNLIMITED)

    // replay=1 so late subscribers (e.g. AdminChatViewModel subscribing after
    // hydrate has already fired during getOrCreate) still receive Hydrated
    // and can clear their loading state.
    private val _events = MutableSharedFlow<TimelineSyncEvent>(replay = 1, extraBufferCapacity = 64)

    /** Signal that hydrate failed — emitted by [TimelineRepository]. */
    internal suspend fun emitHydrateFailed(message: String) {
        _events.emit(TimelineSyncEvent.HydrateFailed(message))
    }
    val events: SharedFlow<TimelineSyncEvent> = _events.asSharedFlow()

    init {
        scope.launch { processSendQueue() }
    }

    private data class PendingSend(
        val otid: String,
        val content: String,
        val attachments: List<MessageContentPart.Image> = emptyList(),
    )

    /**
     * Load initial history from the server.
     *
     * Replaces the current timeline entirely. Should be called once when a
     * conversation is opened.
     */
    suspend fun hydrate(limit: Int = 50) = writeMutex.withLock {
        val timer = Telemetry.startTimer("TimelineSync", "hydrate")
        try {
            val response = messageApi.listConversationMessages(
                conversationId = conversationId,
                limit = limit,
                order = "asc",
            )
            val converted = response.mapIndexedNotNull { idx, msg ->
                msg.toTimelineEvent(position = (idx + 1).toDouble())
            }
            _state.value = Timeline(
                conversationId = conversationId,
                events = converted,
                liveCursor = converted.lastOrNull()?.serverId,
            )
            _events.emit(TimelineSyncEvent.Hydrated(converted.size))
            timer.stop(
                "conversationId" to conversationId,
                "rawCount" to response.size,
                "eventCount" to converted.size,
            )
        } catch (t: Throwable) {
            timer.stopError(t, "conversationId" to conversationId)
            throw t
        }
    }

    /**
     * Send a user message. Returns the generated otid immediately; the full
     * response (stream + reconcile) flows into [state] asynchronously.
     *
     * Atomicity: under a single lock, we both reserve the Local event's
     * position AND enqueue the send. This guarantees that timeline ordering
     * matches send ordering, even under concurrent calls.
     */
    suspend fun send(
        content: String,
        attachments: List<MessageContentPart.Image> = emptyList(),
    ): String {
        val otid = newOtid()
        writeMutex.withLock {
            val local = TimelineEvent.Local(
                position = _state.value.nextLocalPosition(),
                otid = otid,
                content = content,
                role = Role.USER,
                sentAt = Instant.now(),
                deliveryState = DeliveryState.SENDING,
                attachments = attachments,
            )
            _state.value = _state.value.append(local)
            sendQueue.send(PendingSend(otid, content, attachments))  // unlimited capacity → never suspends
        }
        _events.emit(TimelineSyncEvent.LocalAppended(otid))
        Telemetry.event(
            "TimelineSync", "send.localAppended",
            "otid" to otid,
            "conversationId" to conversationId,
            "contentLength" to content.length,
        )
        return otid
    }

    /** Retry a failed send by re-enqueueing it. */
    suspend fun retry(otid: String) = writeMutex.withLock {
        val existing = _state.value.findByOtid(otid)
        if (existing !is TimelineEvent.Local || existing.deliveryState != DeliveryState.FAILED) return@withLock
        
        _state.value = _state.value.copy(events = _state.value.events.map {
            if (it.otid == otid && it is TimelineEvent.Local) {
                it.copy(deliveryState = DeliveryState.SENDING)
            } else it
        })
        sendQueue.send(PendingSend(otid, existing.content, existing.attachments))
    }

    /** Background worker: processes one send at a time from the queue. */
    private suspend fun processSendQueue() {
        for (pending in sendQueue) {
            val roundtrip = Telemetry.startTimer("TimelineSync", "send.roundtrip")
            Telemetry.event(
                "TimelineSync", "send.dequeued",
                "otid" to pending.otid,
                "conversationId" to conversationId,
            )
            try {
                streamAndReconcile(pending.content, pending.otid, pending.attachments)
                roundtrip.stop("otid" to pending.otid)
            } catch (t: Throwable) {
                Telemetry.error(
                    "TimelineSync", "send.failed", t,
                    "otid" to pending.otid,
                    "conversationId" to conversationId,
                )
                writeMutex.withLock {
                    _state.value = _state.value.markFailed(pending.otid)
                }
                _events.emit(TimelineSyncEvent.StreamError("send", t.message ?: "unknown"))
            }
        }
    }

    /**
     * 1. Open stream — each assistant/reasoning/tool event appends a Confirmed event.
     * 2. On stream complete, mark the Local as SENT.
     * 3. Fetch GET /messages to locate our user message (by otid) and swap Local→Confirmed.
     */
    private suspend fun streamAndReconcile(
        content: String,
        otid: String,
        attachments: List<MessageContentPart.Image> = emptyList(),
    ) {
        // Text-only path keeps the legacy JSON-string content; multimodal uses
        // the content-parts JsonArray accepted by Letta/OpenAI-compatible APIs.
        val contentElement: kotlinx.serialization.json.JsonElement = if (attachments.isEmpty()) {
            JsonPrimitive(content)
        } else {
            buildContentParts(content, attachments).toJsonArray()
        }
        val request = MessageCreateRequest(
            messages = listOf(
                kotlinx.serialization.json.Json.encodeToJsonElement(
                    MessageCreate.serializer(),
                    MessageCreate(
                        role = "user",
                        content = contentElement,
                        otid = otid,
                    )
                )
            ),
            streaming = true,
            includeReturnMessageTypes = DEFAULT_INCLUDE_TYPES,
        )
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "send.requestBody otid=$otid preview=${previewRequest(request)}")
        }
        val postTimer = Telemetry.startTimer("TimelineSync", "send.post")
        val channel = messageApi.sendConversationMessage(conversationId, request)
        postTimer.stop("otid" to otid)

        val streamTimer = Telemetry.startTimer("TimelineSync", "send.stream")
        var eventCount = 0
        var firstEventLogged = false
        val firstEventTimer = Telemetry.startTimer("TimelineSync", "send.firstEvent")

        SseParser.parse(channel).collect { message ->
            eventCount++
            if (!firstEventLogged) {
                firstEventLogged = true
                firstEventTimer.stop("otid" to otid)
            }
            writeMutex.withLock {
                val confirmed = message.toTimelineEvent(position = _state.value.nextLocalPosition())
                    ?: return@withLock
                if (_state.value.findByOtid(confirmed.otid) != null) return@withLock  // dedupe
                _state.value = _state.value.append(confirmed)
            }
            _events.emit(TimelineSyncEvent.ServerEvent(message))
        }

        streamTimer.stop("otid" to otid, "eventCount" to eventCount)

        // Stream completed successfully → mark our local send as SENT
        writeMutex.withLock {
            _state.value = _state.value.markSent(otid)
        }

        // Now fetch & reconcile to pull in the authoritative user message record
        reconcileAfterSend(otid)
    }

    private fun previewRequest(req: MessageCreateRequest): String {
        val root = previewJson.encodeToJsonElement(MessageCreateRequest.serializer(), req)
        val sanitizedRoot = root.jsonObject.let { rootObject ->
            val messages = rootObject["messages"]?.jsonArray?.map { redactMessage(it) }
            if (messages == null) {
                rootObject
            } else {
                JsonObject(rootObject + ("messages" to JsonArray(messages)))
            }
        }
        val preview = previewJson.encodeToString(JsonElement.serializer(), sanitizedRoot)
        return if (preview.length <= REQUEST_PREVIEW_MAX_CHARS) {
            preview
        } else {
            preview.take(REQUEST_PREVIEW_MAX_CHARS - 1) + "…"
        }
    }

    private fun redactMessage(element: JsonElement): JsonElement {
        val message = element.jsonObject
        val content = message["content"]
        if (content !is JsonArray) return element
        return JsonObject(message + ("content" to redactContentParts(content)))
    }

    private fun redactContentParts(content: JsonArray): JsonArray = JsonArray(
        content.map { part ->
            val partObject = part.jsonObject
            val type = partObject["type"]?.jsonPrimitive?.contentOrNull
            if (type != "image_url") return@map part
            val imageUrl = partObject["image_url"]?.jsonObject ?: return@map part
            val url = imageUrl["url"]?.jsonPrimitive?.contentOrNull ?: return@map part
            if (!url.startsWith("data:")) return@map part
            JsonObject(
                partObject + (
                    "image_url" to JsonObject(
                        imageUrl + ("url" to JsonPrimitive(previewDataUrl(url)))
                    )
                )
            )
        }
    )

    private fun previewDataUrl(url: String): String {
        val prefix = "data:"
        val separator = ";base64,"
        val separatorIndex = url.indexOf(separator)
        if (!url.startsWith(prefix) || separatorIndex < 0) {
            return "[unsupported data url, totalLen=${url.length}]"
        }
        val mediaType = url.substring(prefix.length, separatorIndex)
        val base64 = url.substring(separatorIndex + separator.length)
        return "data:$mediaType;base64,${base64.take(DATA_URL_PREVIEW_CHARS)}…[truncated, totalLen=${url.length}]"
    }

    /**
     * After a send completes, fetch recent messages and swap our Local user
     * event for the server-confirmed version, and pull in any missed events.
     */
    private suspend fun reconcileAfterSend(otid: String) = writeMutex.withLock {
        val timer = Telemetry.startTimer("TimelineSync", "reconcile")
        var confirmedLocal = false
        var appendedMissing = 0
        try {
            val serverMessages = messageApi.listConversationMessages(
                conversationId = conversationId,
                limit = RECONCILE_LIMIT,
                order = "desc",
            ).reversed()

            // 1. Swap Local→Confirmed for our outbound message
            val myMatch = serverMessages.firstOrNull { it.otid == otid }
            if (myMatch != null) {
                val existing = _state.value.findByOtid(otid)
                if (existing is TimelineEvent.Local) {
                    val confirmed = myMatch.toTimelineEvent(position = existing.position)
                    if (confirmed != null) {
                        _state.value = _state.value.replaceLocal(otid, confirmed)
                        _events.emit(TimelineSyncEvent.LocalConfirmed(otid, myMatch.id))
                        confirmedLocal = true
                    }
                }
            }

            // 2. Pull in any server messages we don't yet have (missed stream events)
            serverMessages.forEach { msg ->
                val msgOtid = msg.otid ?: return@forEach
                if (_state.value.findByOtid(msgOtid) == null) {
                    val pos = _state.value.nextLocalPosition()
                    val confirmed = msg.toTimelineEvent(position = pos) ?: return@forEach
                    _state.value = _state.value.append(confirmed)
                    appendedMissing++
                }
            }

            // 3. Advance liveCursor
            serverMessages.lastOrNull()?.id?.let {
                _state.value = _state.value.copy(liveCursor = it)
            }
            timer.stop(
                "otid" to otid,
                "serverCount" to serverMessages.size,
                "confirmedLocal" to confirmedLocal,
                "appendedMissing" to appendedMissing,
            )
        } catch (t: Throwable) {
            timer.stopError(t, "otid" to otid)
            _events.emit(TimelineSyncEvent.ReconcileError(t.message ?: "unknown"))
        }
    }

    companion object {
        private const val DATA_URL_PREVIEW_CHARS = 32
        private const val REQUEST_PREVIEW_MAX_CHARS = 2_048

        // Most sends produce 1-3 server messages (user echo + assistant + maybe
        // reasoning). Fetching only what we need keeps reconcile snappy.
        private const val RECONCILE_LIMIT = 10

        internal val DEFAULT_INCLUDE_TYPES = listOf(
            "assistant_message",
            "reasoning_message",
            "tool_call_message",
            "tool_return_message",
        )
    }
}

/** Observable events emitted by the sync loop for UI/log subscribers. */
sealed class TimelineSyncEvent {
    data class Hydrated(val messageCount: Int) : TimelineSyncEvent()
    data class LocalAppended(val otid: String) : TimelineSyncEvent()
    data class LocalConfirmed(val otid: String, val serverId: String) : TimelineSyncEvent()
    data class ServerEvent(val message: LettaMessage) : TimelineSyncEvent()
    data class StreamError(val type: String, val message: String) : TimelineSyncEvent()
    data class ReconcileError(val message: String) : TimelineSyncEvent()
    data class HydrateFailed(val message: String) : TimelineSyncEvent()
}

/**
 * Convert a server [LettaMessage] to a Confirmed timeline event.
 *
 * Returns null for message types we don't display (pings, stop reasons, etc.).
 */
internal fun LettaMessage.toTimelineEvent(position: Double): TimelineEvent.Confirmed? {
    val (type, text) = when (this) {
        is UserMessage -> TimelineMessageType.USER to content
        is AssistantMessage -> TimelineMessageType.ASSISTANT to content
        is ReasoningMessage -> TimelineMessageType.REASONING to reasoning
        is ToolCallMessage -> TimelineMessageType.TOOL_CALL to (effectiveToolCalls.firstOrNull()?.name ?: "tool_call")
        is ToolReturnMessage -> TimelineMessageType.TOOL_RETURN to (toolReturn.funcResponse ?: "")
        is SystemMessage -> TimelineMessageType.SYSTEM to content
        else -> return null
    }
    val attachments = when (this) {
        is UserMessage -> this.attachments
        is AssistantMessage -> this.attachments
        is SystemMessage -> this.attachments
        is ReasoningMessage, is ToolCallMessage, is ToolReturnMessage, is ApprovalRequestMessage,
        is ApprovalResponseMessage, is HiddenReasoningMessage, is EventMessage,
        is PingMessage, is UnknownMessage, is StopReason, is UsageStatistics -> emptyList()
    }
    val effectiveOtid = otid ?: "server-$id"
    val date = runCatching { date?.let(Instant::parse) ?: Instant.now() }.getOrElse { Instant.now() }
    return TimelineEvent.Confirmed(
        position = position,
        otid = effectiveOtid,
        content = text,
        serverId = id,
        messageType = type,
        date = date,
        runId = runId,
        stepId = stepId,
        attachments = attachments,
    )
}
