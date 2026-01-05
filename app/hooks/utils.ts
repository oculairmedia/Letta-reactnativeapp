import { Letta } from "@letta-ai/letta-client"
import { AppMessage, MESSAGE_TYPE } from "./types"
import { AssistantMessage } from "@letta-ai/letta-client/resources/agents/messages.mjs"

export const getMessageId = (message: Letta.LettaMessageContentUnion): string => {
  if ("id" in message) {
    return message.type + message.id
  }

  return ""
}

export const extractMessageText = (message: AssistantMessage) => {
  if (typeof message === "string") {
    return message
  } else if (Array.isArray(message)) {
    return message
      .map((content) => {
        if (typeof content === "string") {
          return content
        }

        return content.text
      })
      .join(" ")
  }

  return ""
}

const isHeartbeatMessage = (message: string) => {
  try {
    const parsed = JSON.parse(message)
    if (parsed.type === "heartbeat") {
      return true
    }
    return null
  } catch {
    return null
  }
}

export function extractMessage(item: Letta.LettaMessageContentUnion): AppMessage | null {
  const { type } = item

  if (type === "text") {
    if (!item.text) {
      return null
    }
    const message = item.text
    if (!message) {
      return null
    }
    if (isHeartbeatMessage(message)) {
      return null
    }
    return {
      id: getMessageId(item),
      date: new Date(), // TODO: add date
      content: message,
      messageType: MESSAGE_TYPE.USER_MESSAGE,
    }
  }

  if (type === "tool_call") {
    if (!item.input) {
      return null
    }
    return {
      id: getMessageId(item),
      date: new Date(), // TODO: add date
      toolName: item.name,
      toolCallId: item.id,
      content: JSON.stringify(item.input),
      messageType: MESSAGE_TYPE.TOOL_CALL_MESSAGE,
    }
  }

  if (type === "tool_return") {
    if (!item.content) {
      return null
    }
    return {
      id: getMessageId(item),
      date: new Date(), // TODO: add date
      toolCallId: item.tool_call_id,
      content: JSON.stringify(item.content),
      messageType: MESSAGE_TYPE.TOOL_RETURN_MESSAGE,
      status: item.is_error ? "error" : "success",
      stdout: [], // TODO: add stdout
      stderr: [], // TODO: add stderr
    }
  }

  // if (messageType === MESSAGE_TYPE.ASSISTANT_MESSAGE) {
  //   if (!item.content) {
  //     return null
  //   }
  //   return {
  //     id: getMessageId(item),
  //     date: new Date(item.date),
  //     content: extractMessageText(item.content),
  //     messageType: MESSAGE_TYPE.ASSISTANT_MESSAGE,
  //   }
  // }

  // if (messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
  //   if (!item.reasoning) {
  //     return null
  //   }

  //   return {
  //     id: getMessageId(item),
  //     date: new Date(item.date),
  //     content: item.reasoning,
  //     messageType: MESSAGE_TYPE.REASONING_MESSAGE,
  //   }
  // }

  return null
}

export function filterMessages(data: Letta.LettaMessageContentUnion[]): AppMessage[] {
  return data
    .map((item) => extractMessage(item))
    .filter(Boolean)
    .sort((a, b) => {
      // place reasoning_message always infront of the user message if they are in the same second
      if (a.date === b.date) {
        if (a.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
          return -1
        }

        if (b.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
          return 1
        }
      }

      // otherwise sort by date
      return a.date.getTime() - b.date.getTime()
    })
}
