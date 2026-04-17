package com.letta.mobile.chat

import com.letta.mobile.chat.client.LettaChatClient
import com.letta.mobile.chat.model.MessageRole
import com.letta.mobile.chat.model.StreamEvent
import com.letta.mobile.data.model.AppMessage
import com.letta.mobile.data.model.Conversation
import com.letta.mobile.data.model.MessageType
import com.letta.mobile.data.repository.ConversationManager
import com.letta.mobile.data.repository.ConversationRepository
import com.letta.mobile.data.repository.MessageRepository
import com.letta.mobile.data.repository.StreamState
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.time.Instant

class LettaChatClientTest : WordSpec({

    fun fakeMessage(id: String, type: MessageType, content: String, toolName: String? = null) =
        AppMessage(id = id, date = Instant.now(), messageType = type, content = content, toolName = toolName)

    fun fakeConversation(id: String, agentId: String) =
        Conversation(id = id, agentId = agentId, summary = "test")

    fun createClient(
        messages: List<AppMessage> = emptyList(),
        streamStates: List<StreamState> = emptyList(),
        conversations: List<Conversation> = listOf(fakeConversation("conv-1", "agent-1")),
        agentId: String = "agent-1",
        conversationId: String? = "conv-1",
    ): LettaChatClient {
        val messageRepo = object : MessageRepository(mockk(relaxed = true)) {
            override suspend fun fetchMessages(
                agentId: String,
                conversationId: String,
                targetMessageId: String?,
            ): List<AppMessage> = messages
        }
        val conversationRepo = object : ConversationRepository(mockk(relaxed = true)) {
            override fun getConversations(agentId: String): Flow<List<Conversation>> = flowOf(conversations)
            override suspend fun refreshConversations(agentId: String) {}
            override fun getCachedConversations(agentId: String): List<Conversation> =
                conversations.filter { it.agentId == agentId }

            override suspend fun getConversation(id: String): Conversation =
                conversations.first { it.id == id }

            override suspend fun createConversation(agentId: String, summary: String?): Conversation = fakeConversation("new-conv", agentId)
            override suspend fun updateConversation(id: String, agentId: String, summary: String) {}
            override suspend fun deleteConversation(id: String, agentId: String) {}
            override suspend fun forkConversation(id: String, agentId: String): Conversation = fakeConversation("fork-$id", agentId)
        }
        val conversationManager = ConversationManager(conversationRepo)
        val timelineRepo = mockk<com.letta.mobile.data.timeline.TimelineRepository>(relaxed = true)
        return LettaChatClient(messageRepo, timelineRepo, conversationManager, conversationRepo, agentId, conversationId)
    }

    "connect" should {
        "load existing messages" {
            runTest {
                val client = createClient(
                    messages = listOf(
                        fakeMessage("1", MessageType.USER, "Hello"),
                        fakeMessage("2", MessageType.ASSISTANT, "Hi"),
                    )
                )
                client.connect()
                client.messages.value shouldHaveSize 2
                client.isLoading.value shouldBe false
            }
        }

        "set isLoading false after connect" {
            runTest {
                val client = createClient()
                client.connect()
                client.isLoading.value shouldBe false
            }
        }

        "map approval request and response message roles correctly" {
            runTest {
                val client = createClient(
                    messages = listOf(
                        fakeMessage("approval-request", MessageType.APPROVAL_REQUEST, "Please approve"),
                        fakeMessage("approval-response", MessageType.APPROVAL_RESPONSE, "Approved"),
                    )
                )

                client.connect()

                client.messages.value.map { it.role } shouldBe listOf(
                    MessageRole.Assistant,
                    MessageRole.User,
                )
                client.messages.value.map { it.content } shouldBe listOf(
                    "Please approve",
                    "Approved",
                )
            }
        }
    }

    "sendMessage" should {
        "add user message immediately to list" {
            runTest {
                val client = createClient(streamStates = listOf(StreamState.Sending))
                client.connect()
                client.sendMessage("Hello").first()
                client.messages.value.any { it.content == "Hello" && it.role == MessageRole.User } shouldBe true
                client.messages.value.any { it.content == "Hello" && it.isPending } shouldBe true
            }
        }


        "replace pending user message when reload returns matching server message" {
            runTest {
                val history = listOf(
                    fakeMessage("old-1", MessageType.USER, "Q1"),
                    fakeMessage("old-2", MessageType.ASSISTANT, "A1"),
                )
                val streamResponse = listOf(
                    fakeMessage("reply-1", MessageType.ASSISTANT, "Fresh reply"),
                )
                val persistedServerSnapshot = listOf(
                    fakeMessage("old-1", MessageType.USER, "Q1"),
                    fakeMessage("server-user-2", MessageType.USER, "Q2"),
                    fakeMessage("old-2", MessageType.ASSISTANT, "A1"),
                    fakeMessage("reply-1", MessageType.ASSISTANT, "Fresh reply"),
                )
                val fetchResponses = ArrayDeque(listOf(history, persistedServerSnapshot))
                val messageRepo = object : MessageRepository(mockk(relaxed = true)) {
                    override suspend fun fetchMessages(
                        agentId: String,
                        conversationId: String,
                        targetMessageId: String?,
                    ): List<AppMessage> = fetchResponses.removeFirstOrNull() ?: persistedServerSnapshot
                }
                val conversationRepo = object : ConversationRepository(mockk(relaxed = true)) {
                    override fun getConversations(agentId: String): Flow<List<Conversation>> = flowOf(listOf(fakeConversation("conv-1", "agent-1")))
                    override suspend fun refreshConversations(agentId: String) {}
                    override fun getCachedConversations(agentId: String): List<Conversation> = listOf(fakeConversation("conv-1", "agent-1"))
                    override suspend fun getConversation(id: String): Conversation = fakeConversation(id, "agent-1")
                    override suspend fun createConversation(agentId: String, summary: String?): Conversation = fakeConversation("new-conv", agentId)
                    override suspend fun updateConversation(id: String, agentId: String, summary: String) {}
                    override suspend fun deleteConversation(id: String, agentId: String) {}
                    override suspend fun forkConversation(id: String, agentId: String): Conversation = fakeConversation("fork-$id", agentId)
                }
                val conversationManager = ConversationManager(conversationRepo)
                val timelineRepo = mockk<com.letta.mobile.data.timeline.TimelineRepository>(relaxed = true)
                val client = LettaChatClient(messageRepo, timelineRepo, conversationManager, conversationRepo, "agent-1", "conv-1")

                client.connect()
                client.sendMessage("Q2").toList()

                val matchingMessages = client.messages.value.filter { it.content == "Q2" }
                matchingMessages shouldHaveSize 1
                matchingMessages.single().id shouldBe "server-user-2"
                matchingMessages.single().isPending shouldBe false
            }
        }

        "emit Complete event on stream finish" {
            runTest {
                val client = createClient(
                    streamStates = listOf(StreamState.Complete(emptyList())),
                )
                client.connect()
                val events = client.sendMessage("test").toList()
                events.last().shouldBeInstanceOf<StreamEvent.Complete>()
                client.isStreaming.value shouldBe false
            }
        }

        "deduplicate messages by ID" {
            runTest {
                val msg = fakeMessage("dup-1", MessageType.ASSISTANT, "Same message")
                val client = createClient(
                    messages = listOf(msg),
                    streamStates = listOf(StreamState.Streaming(listOf(msg))),
                )
                client.connect()
                client.sendMessage("test").toList()
                val withId = client.messages.value.filter { it.id == "dup-1" }
                withId shouldHaveSize 1
            }
        }
    }

})
