package com.letta.mobile.data.mapper

import com.letta.mobile.data.model.ApprovalRequestMessage
import com.letta.mobile.data.model.ApprovalResponseMessage
import com.letta.mobile.data.model.ApprovalResult
import com.letta.mobile.data.model.MessageType
import com.letta.mobile.data.model.ToolCall
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import com.letta.mobile.testutil.TestData
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class MessageMapperTest : WordSpec({
    "AppMessage.toUiMessage" should {
        "map user messages to role user" {
            val uiMsg = TestData.appMessage(messageType = MessageType.USER, content = "Hello").toUiMessage()
            uiMsg.role shouldBe "user"
            uiMsg.content shouldBe "Hello"
            uiMsg.isReasoning shouldBe false
            uiMsg.toolCalls.shouldBeNull()
        }

        "map assistant messages to role assistant" {
            val uiMsg = TestData.appMessage(messageType = MessageType.ASSISTANT, content = "Hi there").toUiMessage()
            uiMsg.role shouldBe "assistant"
            uiMsg.content shouldBe "Hi there"
        }

        "map reasoning messages to assistant role with isReasoning" {
            val uiMsg = TestData.appMessage(messageType = MessageType.REASONING, content = "Thinking...").toUiMessage()
            uiMsg.role shouldBe "assistant"
            uiMsg.isReasoning shouldBe true
        }

        "map tool call messages to tool role with tool calls" {
            val uiMsg = TestData.appMessage(
                messageType = MessageType.TOOL_CALL,
                content = "{\"query\": \"test\"}",
                toolName = "web_search",
                toolCallId = "tc-1"
            ).toUiMessage()
            uiMsg.role shouldBe "tool"
            uiMsg.toolCalls?.size shouldBe 1
            uiMsg.toolCalls?.first()?.name shouldBe "web_search"
        }

        "map tool return messages to tool role with tool result details" {
            val uiMsg = TestData.appMessage(
                messageType = MessageType.TOOL_RETURN,
                content = "Search results...",
                toolName = "web_search",
                toolCallId = "tc-1"
            ).toUiMessage()
            uiMsg.role shouldBe "tool"
            uiMsg.content shouldBe ""
            uiMsg.toolCalls?.size shouldBe 1
            uiMsg.toolCalls?.first()?.name shouldBe "web_search"
            uiMsg.toolCalls?.first()?.result shouldBe "Search results..."
        }

        "preserve timestamp" {
            TestData.appMessage(id = "m1").toUiMessage().timestamp.shouldNotBeBlank()
        }

        "preserve id" {
            TestData.appMessage(id = "unique-id-123").toUiMessage().id shouldBe "unique-id-123"
        }

        "propagate pending state to ui message" {
            val uiMsg = TestData.appMessage(
                id = "pending-local",
                content = "Queued",
                isPending = true,
                localId = "local-1",
            ).toUiMessage()

            uiMsg.isPending shouldBe true
        }

        "always produce toolCalls for TOOL_CALL even without name" {
            val uiMsg = TestData.appMessage(
                messageType = MessageType.TOOL_CALL,
                content = "{}",
                toolName = null,
                toolCallId = "tc-x"
            ).toUiMessage()
            uiMsg.toolCalls.shouldNotBeNull()
            uiMsg.toolCalls!! shouldHaveSize 1
            uiMsg.toolCalls!!.first().name shouldBe "tool"
        }

        "always produce toolCalls for TOOL_RETURN even without name" {
            val uiMsg = TestData.appMessage(
                messageType = MessageType.TOOL_RETURN,
                content = "result data",
                toolName = null,
                toolCallId = "tc-y"
            ).toUiMessage()
            uiMsg.toolCalls.shouldNotBeNull()
            uiMsg.toolCalls!! shouldHaveSize 1
            uiMsg.toolCalls!!.first().name shouldBe "tool"
            uiMsg.toolCalls!!.first().result shouldBe "result data"
        }
    }

    "List<AppMessage>.toUiMessages" should {
        "merge TOOL_CALL and matching TOOL_RETURN into single card" {
            val messages = listOf(
                TestData.appMessage(id = "m1", messageType = MessageType.USER, content = "search for cats"),
                TestData.appMessage(
                    id = "m2",
                    messageType = MessageType.TOOL_CALL,
                    content = "{\"query\": \"cats\"}",
                    toolName = "web_search",
                    toolCallId = "tc-1"
                ),
                TestData.appMessage(
                    id = "m3",
                    messageType = MessageType.TOOL_RETURN,
                    content = "Found 10 results about cats",
                    toolName = "web_search",
                    toolCallId = "tc-1"
                ),
                TestData.appMessage(id = "m4", messageType = MessageType.ASSISTANT, content = "Here are cat results"),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 3
            ui[0].role shouldBe "user"
            ui[1].role shouldBe "tool"
            ui[1].toolCalls.shouldNotBeNull()
            ui[1].toolCalls!! shouldHaveSize 1
            val tc = ui[1].toolCalls!!.first()
            tc.name shouldBe "web_search"
            tc.arguments shouldBe "{\"query\": \"cats\"}"
            tc.result shouldBe "Found 10 results about cats"
            ui[2].role shouldBe "assistant"
        }

        "promote send_message tool to assistant bubble" {
            val messages = listOf(
                TestData.appMessage(id = "m1", messageType = MessageType.USER, content = "Hello"),
                TestData.appMessage(
                    id = "m2",
                    messageType = MessageType.TOOL_CALL,
                    content = "{\"message\": \"Hi there, how can I help?\"}",
                    toolName = "send_message",
                    toolCallId = "tc-sm"
                ),
                TestData.appMessage(
                    id = "m3",
                    messageType = MessageType.TOOL_RETURN,
                    content = "None",
                    toolName = "send_message",
                    toolCallId = "tc-sm"
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 2
            ui[0].role shouldBe "user"
            ui[1].role shouldBe "assistant"
            ui[1].content shouldBe "Hi there, how can I help?"
        }

        "render orphaned TOOL_RETURN as standalone tool card" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1",
                    messageType = MessageType.TOOL_RETURN,
                    content = "orphan result",
                    toolName = "archival_memory_search",
                    toolCallId = "tc-orphan"
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 1
            ui[0].role shouldBe "tool"
            ui[0].toolCalls.shouldNotBeNull()
            ui[0].toolCalls!! shouldHaveSize 1
            ui[0].toolCalls!!.first().name shouldBe "archival_memory_search"
            ui[0].toolCalls!!.first().result shouldBe "orphan result"
        }

        "handle TOOL_CALL without matching TOOL_RETURN" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1",
                    messageType = MessageType.TOOL_CALL,
                    content = "{\"key\": \"val\"}",
                    toolName = "core_memory_append",
                    toolCallId = "tc-no-return"
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 1
            ui[0].role shouldBe "tool"
            val tc = ui[0].toolCalls!!.first()
            tc.name shouldBe "core_memory_append"
            tc.arguments shouldBe "{\"key\": \"val\"}"
            tc.result.shouldBeNull()
        }

        "pass through user, assistant, reasoning unchanged" {
            val messages = listOf(
                TestData.appMessage(id = "m1", messageType = MessageType.USER, content = "hello"),
                TestData.appMessage(id = "m2", messageType = MessageType.ASSISTANT, content = "hi"),
                TestData.appMessage(id = "m3", messageType = MessageType.REASONING, content = "thinking"),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 3
            ui[0].role shouldBe "user"
            ui[1].role shouldBe "assistant"
            ui[2].role shouldBe "assistant"
            ui[2].isReasoning shouldBe true
        }

        "handle orphaned send_message TOOL_RETURN as assistant message" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1",
                    messageType = MessageType.TOOL_RETURN,
                    content = "Hi from agent",
                    toolName = "send_message",
                    toolCallId = "tc-sm-orphan"
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 1
            ui[0].role shouldBe "assistant"
            ui[0].content shouldBe "Hi from agent"
        }

        "handle multiple tool call-return pairs" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1", messageType = MessageType.TOOL_CALL,
                    content = "{}", toolName = "archival_memory_search", toolCallId = "tc-1"
                ),
                TestData.appMessage(
                    id = "m2", messageType = MessageType.TOOL_RETURN,
                    content = "results A", toolName = "archival_memory_search", toolCallId = "tc-1"
                ),
                TestData.appMessage(
                    id = "m3", messageType = MessageType.TOOL_CALL,
                    content = "{}", toolName = "core_memory_append", toolCallId = "tc-2"
                ),
                TestData.appMessage(
                    id = "m4", messageType = MessageType.TOOL_RETURN,
                    content = "OK", toolName = "core_memory_append", toolCallId = "tc-2"
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 2
            ui[0].toolCalls!!.first().name shouldBe "archival_memory_search"
            ui[0].toolCalls!!.first().result shouldBe "results A"
            ui[1].toolCalls!!.first().name shouldBe "core_memory_append"
            ui[1].toolCalls!!.first().result shouldBe "OK"
        }

        "promote generated ui tool results into assistant generated ui messages" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1",
                    messageType = MessageType.TOOL_CALL,
                    content = "{\"title\":\"Today\"}",
                    toolName = "render_summary_card",
                    toolCallId = "tc-ui-1",
                ),
                TestData.appMessage(
                    id = "m2",
                    messageType = MessageType.TOOL_RETURN,
                    content = "{\"type\":\"generated_ui\",\"component\":\"summary_card\",\"props\":{\"title\":\"Today\",\"body\":\"3 tasks pending\"},\"text\":\"Here is your summary\"}",
                    toolName = "render_summary_card",
                    toolCallId = "tc-ui-1",
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 1
            ui[0].role shouldBe "assistant"
            ui[0].content shouldBe "Here is your summary"
            ui[0].generatedUi.shouldNotBeNull()
            ui[0].generatedUi!!.name shouldBe "summary_card"
            ui[0].generatedUi!!.propsJson shouldBe "{\"title\":\"Today\",\"body\":\"3 tasks pending\"}"
        }

        "promote suggestion chip tool results into assistant generated ui messages" {
            val messages = listOf(
                TestData.appMessage(
                    id = "m1",
                    messageType = MessageType.TOOL_CALL,
                    content = "{\"title\":\"Next steps\"}",
                    toolName = "render_suggestion_chips",
                    toolCallId = "tc-ui-2",
                ),
                TestData.appMessage(
                    id = "m2",
                    messageType = MessageType.TOOL_RETURN,
                    content = "{\"type\":\"generated_ui\",\"component\":\"suggestion_chips\",\"props\":{\"title\":\"Next steps\",\"suggestions\":[{\"label\":\"Explain coroutines\",\"message\":\"Explain Kotlin coroutines\"}]},\"text\":\"Choose a follow-up\"}",
                    toolName = "render_suggestion_chips",
                    toolCallId = "tc-ui-2",
                ),
            )

            val ui = messages.toUiMessages()
            ui shouldHaveSize 1
            ui[0].role shouldBe "assistant"
            ui[0].generatedUi.shouldNotBeNull()
            ui[0].generatedUi!!.name shouldBe "suggestion_chips"
            ui[0].content shouldBe "Choose a follow-up"
        }

        "map approval requests to dedicated approval ui messages" {
            val messages = listOf(
                ApprovalRequestMessage(
                    id = "approval-1",
                    toolCalls = listOf(
                        ToolCall(
                            toolCallId = "tool-call-1",
                            name = "Bash",
                            arguments = "{\"command\":\"rm -rf /tmp/demo\"}",
                        )
                    ),
                ).toAppMessage()!!,
            )

            val ui = messages.toUiMessages()

            ui shouldHaveSize 1
            ui[0].role shouldBe "approval"
            ui[0].approvalRequest.shouldNotBeNull()
            ui[0].approvalRequest!!.requestId shouldBe "approval-1"
            ui[0].approvalRequest!!.toolCalls.single().name shouldBe "Bash"
        }

        "map approval responses to dedicated approval ui messages" {
            val messages = listOf(
                ApprovalResponseMessage(
                    id = "approval-response-1",
                    approvalRequestId = "approval-1",
                    approve = false,
                    reason = "Unsafe command",
                    approvals = listOf(
                        ApprovalResult(
                            toolCallId = "tool-call-1",
                            approve = false,
                            status = "rejected",
                            reason = "Unsafe command",
                        )
                    ),
                ).toAppMessage()!!,
            )

            val ui = messages.toUiMessages()

            ui shouldHaveSize 1
            ui[0].role shouldBe "approval"
            ui[0].approvalResponse.shouldNotBeNull()
            ui[0].approvalResponse!!.approved shouldBe false
            ui[0].approvalResponse!!.reason shouldBe "Unsafe command"
        }
    }

    "LettaMessage.toAppMessage" should {
        "extract generated ui payloads from assistant content objects" {
            val message = com.letta.mobile.data.model.AssistantMessage(
                id = "assistant-ui-1",
                contentRaw = buildJsonObject {
                    put("type", "generated_ui")
                    put("component", "summary_card")
                    putJsonObject("props") {
                        put("title", "Daily summary")
                        put("body", "3 tasks need attention")
                    }
                    put("text", "Here is your daily summary")
                },
            )

            val appMessage = message.toAppMessage()

            appMessage.shouldNotBeNull()
            appMessage.messageType shouldBe MessageType.ASSISTANT
            appMessage.content shouldBe "Here is your daily summary"
            appMessage.generatedUi.shouldNotBeNull()
            appMessage.generatedUi!!.component shouldBe "summary_card"
            appMessage.generatedUi!!.propsJson shouldBe "{\"title\":\"Daily summary\",\"body\":\"3 tasks need attention\"}"
            appMessage.generatedUi!!.fallbackText shouldBe "Here is your daily summary"
        }

        "suppress raw generated ui json when no fallback text exists" {
            val message = com.letta.mobile.data.model.AssistantMessage(
                id = "assistant-ui-2",
                contentRaw = buildJsonObject {
                    put("type", "generated_ui")
                    put("component", "metric_card")
                    putJsonObject("props") {
                        put("label", "Tasks")
                        put("value", "3")
                    }
                },
            )

            val appMessage = message.toAppMessage()

            appMessage.shouldNotBeNull()
            appMessage.content shouldBe ""
            appMessage.generatedUi.shouldNotBeNull()
            appMessage.generatedUi!!.component shouldBe "metric_card"
        }
    }

    "AppMessage.toUiMessage" should {
        "carry generated ui payloads into chat messages" {
            val uiMsg = TestData.appMessage(
                messageType = MessageType.ASSISTANT,
                content = "Here is your daily summary",
            ).copy(
                generatedUi = com.letta.mobile.data.model.GeneratedUiPayload(
                    component = "summary_card",
                    propsJson = "{\"title\":\"Daily summary\"}",
                    fallbackText = "Here is your daily summary",
                ),
            ).toUiMessage()

            uiMsg.role shouldBe "assistant"
            uiMsg.generatedUi.shouldNotBeNull()
            uiMsg.generatedUi!!.name shouldBe "summary_card"
            uiMsg.generatedUi!!.propsJson shouldBe "{\"title\":\"Daily summary\"}"
        }
    }
})
