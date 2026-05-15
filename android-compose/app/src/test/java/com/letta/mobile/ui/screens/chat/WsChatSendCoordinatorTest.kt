package com.letta.mobile.ui.screens.chat

import com.letta.mobile.data.model.LettaConfig
import com.letta.mobile.data.timeline.TimelineRepository
import com.letta.mobile.data.transport.ChannelTransport
import com.letta.mobile.data.transport.WsChatBridge
import com.letta.mobile.data.transport.WsTimelineEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WsChatSendCoordinatorTest {
    @Test
    fun `send dispatches through ws bridge and appends optimistic local with android otid`() = runTest {
        val settingsRepository = settingsRepository()
        val wsChatBridge = mockBridge(sendAccepted = true)
        val timelineRepository = mockk<TimelineRepository>(relaxed = true)
        val otid = slot<String>()
        coEvery {
            timelineRepository.appendExternalTransportLocal("conv-default-agent-1", "hello", capture(otid), emptyList())
        } answers { otid.captured }
        var cleared = false
        var activeConversation: String? = null
        var observedConversation: String? = null
        val uiState = MutableStateFlow(ChatUiState(agentName = "Agent"))

        val coordinator = WsChatSendCoordinator(
            scope = backgroundScope,
            agentId = "agent-1",
            activeConfig = settingsRepository,
            wsChatBridge = wsChatBridge,
            timelineRepository = timelineRepository,
            uiState = uiState,
            clearComposerAfterSend = { cleared = true },
            activeConversationId = { activeConversation },
            setActiveConversationId = { activeConversation = it },
            startTimelineObserver = { observedConversation = it },
        )

        coordinator.send("hello").join()

        assertTrue(otid.captured.startsWith("cm-android-"))
        assertTrue(cleared)
        assertEquals("conv-default-agent-1", activeConversation)
        assertEquals("conv-default-agent-1", observedConversation)
        assertTrue(uiState.value.isStreaming)
        verify {
            wsChatBridge.send(
                agentId = "agent-1",
                conversationId = "conv-default-agent-1",
                text = "hello",
                otid = otid.captured,
            )
        }
    }

    @Test
    fun `send false is surfaced as busy without appending optimistic local`() = runTest {
        val wsChatBridge = mockBridge(sendAccepted = false)
        val timelineRepository = mockk<TimelineRepository>(relaxed = true)
        var cleared = false
        val uiState = MutableStateFlow(ChatUiState(agentName = "Agent"))
        val coordinator = WsChatSendCoordinator(
            scope = backgroundScope,
            agentId = "agent-1",
            activeConfig = settingsRepository(),
            wsChatBridge = wsChatBridge,
            timelineRepository = timelineRepository,
            uiState = uiState,
            clearComposerAfterSend = { cleared = true },
            activeConversationId = { "conv-1" },
            setActiveConversationId = {},
            startTimelineObserver = {},
        )

        coordinator.send("hello").join()

        assertEquals("A WebSocket chat turn is already in flight", uiState.value.error)
        assertEquals(false, cleared)
        coVerify(exactly = 0) { timelineRepository.appendExternalTransportLocal(any(), any(), any(), any()) }
    }

    @Test
    fun `turn done reconciles immediately against external default conversation id`() = runTest {
        val events = MutableSharedFlow<WsTimelineEvent>()
        val wsChatBridge = mockBridge(sendAccepted = true, eventFlow = events)
        val timelineRepository = mockk<TimelineRepository>(relaxed = true)
        val otid = slot<String>()
        coEvery {
            timelineRepository.appendExternalTransportLocal("conv-default-agent-1", "hello", capture(otid), emptyList())
        } answers { otid.captured }
        val coordinator = WsChatSendCoordinator(
            scope = backgroundScope,
            agentId = "agent-1",
            activeConfig = settingsRepository(),
            wsChatBridge = wsChatBridge,
            timelineRepository = timelineRepository,
            uiState = MutableStateFlow(ChatUiState(agentName = "Agent")),
            clearComposerAfterSend = {},
            activeConversationId = { null },
            setActiveConversationId = {},
            startTimelineObserver = {},
        )

        coordinator.send("hello").join()
        events.emit(WsTimelineEvent.TurnDone(turnId = "turn-1", runId = "run-1", status = "completed"))
        advanceUntilIdle()

        coVerify {
            timelineRepository.reconcileExternalTransportSend(
                conversationId = "conv-default-agent-1",
                agentId = "agent-1",
                externalConversationId = "conv-default-agent-1",
                otid = otid.captured,
            )
        }
    }

    private fun settingsRepository(): () -> LettaConfig? = {
        LettaConfig(
            id = "shim",
            mode = LettaConfig.Mode.SELF_HOSTED,
            serverUrl = "http://localhost:8291",
            accessToken = "token",
        )
    }

    private fun mockBridge(
        sendAccepted: Boolean,
        eventFlow: kotlinx.coroutines.flow.Flow<WsTimelineEvent> = emptyFlow(),
    ): WsChatBridge = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(
            ChannelTransport.State.Connected(
                serverId = "server-1",
                sessionId = "sess-1",
                deviceId = "android-letta-mobile",
            )
        )
        every { events } returns eventFlow
        every { send(any(), any(), any(), any()) } returns sendAccepted
    }
}
