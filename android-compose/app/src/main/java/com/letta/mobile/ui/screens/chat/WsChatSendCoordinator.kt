package com.letta.mobile.ui.screens.chat

import com.letta.mobile.BuildConfig
import com.letta.mobile.data.model.MessageContentPart
import com.letta.mobile.data.model.LettaConfig
import com.letta.mobile.data.timeline.TimelineRepository
import com.letta.mobile.data.transport.ChannelTransport
import com.letta.mobile.data.transport.WsChatBridge
import com.letta.mobile.data.transport.WsTimelineEvent
import com.letta.mobile.util.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** Owns the admin-shim mobile WebSocket send path. */
internal class WsChatSendCoordinator(
    private val scope: CoroutineScope,
    private val agentId: String,
    private val activeConfig: () -> LettaConfig?,
    private val wsChatBridge: WsChatBridge,
    private val timelineRepository: TimelineRepository,
    private val uiState: MutableStateFlow<ChatUiState>,
    private val clearComposerAfterSend: () -> Unit,
    private val activeConversationId: () -> String?,
    private val setActiveConversationId: (String) -> Unit,
    private val startTimelineObserver: (String) -> Unit,
) {
    @Volatile private var activeWsConversationId: String? = null
    @Volatile private var activeWsOtid: String? = null

    init {
        scope.launch {
            wsChatBridge.events.collect { event -> handleEvent(event) }
        }
    }

    fun send(
        text: String,
        attachments: List<MessageContentPart.Image> = emptyList(),
    ): Job = scope.launch {
        val timer = Telemetry.startTimer("AdminChatVM", "send.ws.enqueue")
        val config = activeConfig()
        if (config == null) {
            failSend("No active backend is configured")
            return@launch
        }
        if (config.accessToken.isNullOrBlank()) {
            failSend("Admin-shim WebSocket requires an API token")
            return@launch
        }
        if (attachments.isNotEmpty()) {
            failSend("Image attachments are not supported over the admin-shim WebSocket yet")
            return@launch
        }

        val conversationId = defaultShimConversationId(agentId)
        activeWsConversationId = conversationId
        setActiveConversationId(conversationId)
        startTimelineObserver(conversationId)

        val connected = ensureConnected(config)
        if (!connected) {
            failSend("Admin-shim WebSocket is not connected")
            timer.stop("accepted" to false, "reason" to "not_connected")
            return@launch
        }

        val otid = "cm-android-${UUID.randomUUID()}"
        val accepted = wsChatBridge.send(
            agentId = agentId,
            conversationId = conversationId,
            text = text,
            otid = otid,
        )
        if (!accepted) {
            failSend("A WebSocket chat turn is already in flight")
            timer.stop("accepted" to false, "reason" to "busy")
            return@launch
        }
        activeWsOtid = otid

        timelineRepository.appendExternalTransportLocal(
            conversationId = conversationId,
            content = text,
            otid = otid,
            attachments = attachments,
        )
        clearComposerAfterSend()
        uiState.value = uiState.value.copy(
            conversationState = ConversationState.Ready(conversationId),
            isStreaming = true,
            isAgentTyping = true,
        )
        timer.stop("accepted" to true, "conversationId" to conversationId, "otid" to otid)
    }

    fun cancel(): Boolean = wsChatBridge.cancel()

    private suspend fun ensureConnected(config: LettaConfig): Boolean {
        if (wsChatBridge.state.value is ChannelTransport.State.Connected) return true
        runCatching {
            wsChatBridge.connect(
                baseShimUrl = config.serverUrl,
                token = config.accessToken.orEmpty(),
                deviceId = "android-letta-mobile",
                clientVersion = "letta-mobile/${BuildConfig.VERSION_NAME} (android)",
            )
        }.onFailure { error ->
            Telemetry.error("AdminChatVM", "ws.connect.failed", error)
            return false
        }
        return withTimeoutOrNull(CONNECT_WAIT_MS) {
            wsChatBridge.state.filter { it is ChannelTransport.State.Connected }.first()
            true
        } ?: false
    }

    private suspend fun handleEvent(event: WsTimelineEvent) {
        when (event) {
            is WsTimelineEvent.TurnStarted -> {
                activeWsConversationId = event.conversationId
                setActiveConversationId(event.conversationId)
                startTimelineObserver(event.conversationId)
                uiState.value = uiState.value.copy(
                    conversationState = ConversationState.Ready(event.conversationId),
                    isStreaming = true,
                    isAgentTyping = true,
                    error = null,
                )
            }
            is WsTimelineEvent.MessageDelta -> {
                val conversationId = activeWsConversationId ?: activeConversationId() ?: return
                timelineRepository.ingestExternalTransportMessage(conversationId, event.message)
            }
            is WsTimelineEvent.TurnDone -> {
                val conversationId = activeWsConversationId ?: defaultShimConversationId(agentId)
                activeWsOtid?.let { otid ->
                    timelineRepository.reconcileExternalTransportSend(
                        conversationId = conversationId,
                        agentId = agentId,
                        externalConversationId = defaultShimConversationId(agentId),
                        otid = otid,
                    )
                }
                activeWsOtid = null
                uiState.value = uiState.value.copy(isStreaming = false, isAgentTyping = false)
            }
            is WsTimelineEvent.Error -> {
                uiState.value = uiState.value.copy(
                    error = event.message.ifBlank { event.code },
                    isStreaming = false,
                    isAgentTyping = false,
                )
            }
            is WsTimelineEvent.Disconnected -> {
                uiState.value = uiState.value.copy(
                    error = event.reason.ifBlank { "WebSocket disconnected" },
                    isStreaming = false,
                    isAgentTyping = false,
                )
            }
            is WsTimelineEvent.StopReason,
            is WsTimelineEvent.UsageStatistics -> Unit
        }
    }

    private fun failSend(message: String) {
        uiState.value = uiState.value.copy(
            error = message,
            isStreaming = false,
            isAgentTyping = false,
        )
    }

    private companion object {
        private const val CONNECT_WAIT_MS = 1_500L
        private fun defaultShimConversationId(agentId: String): String = "conv-default-$agentId"
    }
}
