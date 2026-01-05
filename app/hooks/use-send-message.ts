import { useMutation, useQueryClient } from "@tanstack/react-query"
import { AppMessage, MESSAGE_TYPE, ROLE_TYPE, useAssistantMessage } from "./types"
import { getAgentMessagesQueryKey } from "./use-agent-messages"
import { extractMessage, getMessageId } from "./utils"

import { useLettaClient } from "@/providers/LettaProvider"
import type { Letta } from "@letta-ai/letta-client"
import uuid from "react-native-uuid"
const uuidv4 = uuid.v4

export interface UseSendMessageType {
  agentId: string
  text: string
}

function updateMessageInQueryData(
  queryClient: ReturnType<typeof useQueryClient>,
  agentId: string,
  response: Letta.LettaMessageUnion,
  responseMessageId: string,
) {
  queryClient.setQueriesData<AppMessage[] | undefined>(
    {
      queryKey: getAgentMessagesQueryKey(agentId),
    },
    (_data) => {
      if (!_data) {
        _data = []
      }

      const data = _data.filter((message) => Boolean(message) && message.id !== "deleteme_")
      const processedMessage = extractMessage(response)

      if (!processedMessage) {
        return data
      }

      const existingMessage = data.find((message) => message.id === responseMessageId)

      if (existingMessage?.messageType === processedMessage.messageType) {
        return data.map((message) => {
          if (message.id === responseMessageId) {
            if (message.messageType === MESSAGE_TYPE.ASSISTANT_MESSAGE) {
              return {
                ...processedMessage,
                content: existingMessage.content + processedMessage.content,
              }
            }
            return processedMessage
          }
          return message
        })
      }

      const newData = [...data, processedMessage]

      // Sort messages with reasoning messages appearing before other messages with same timestamp
      return newData.sort((a, b) => {
        if (a.date.getTime() === b.date.getTime()) {
          if (a.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
            return -1
          }
          if (b.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
            return 1
          }
        }
        return a.date.getTime() - b.date.getTime()
      })
    },
  )
}

export function useSendMessageAsync() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()
  async function sendMessage(options: UseSendMessageType) {
    const { agentId, text } = options

    const userMessage: AppMessage = {
      id: uuidv4(),
      date: new Date(),
      messageType: MESSAGE_TYPE.USER_MESSAGE,
      content: text,
    }

    queryClient.setQueriesData<AppMessage[] | undefined>(
      {
        queryKey: getAgentMessagesQueryKey(agentId),
      },
      (_data) => {
        const data = _data ?? []
        return [...data, userMessage]
      },
    )

    const messagesResponse = await lettaClient.agents.messages
      .create(
        agentId,
        {
          useAssistantMessage,
          messages: [
            {
              role: ROLE_TYPE.USER,
              content: text,
            },
          ],
        },
        {
          timeoutInSeconds: 120,
        },
      )
      .catch((error) => {
        console.warn("Error sending message:", error)
      })

    if (!messagesResponse) {
      return
    }

    for await (const response of messagesResponse.messages) {
      const responseMessageId = getMessageId(response)
      updateMessageInQueryData(queryClient, agentId, response, responseMessageId)
    }
  }

  return useMutation<void, undefined, UseSendMessageType>({
    mutationFn: (options) =>
      sendMessage(options).catch((error) => {
        console.error("Error sending message:", error)
        throw error
      }),
  })
}
