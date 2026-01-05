// import { useMutation, useQueryClient } from "@tanstack/react-query"
// import { AppMessage, MESSAGE_TYPE, ROLE_TYPE } from "./types"
// import { getAgentMessagesQueryKey } from "./use-agent-messages"
// import { extractMessageText, getMessageId } from "./utils"

// import { useLettaClient } from "@/providers/LettaProvider"
// import uuid from "react-native-uuid"
// const uuidv4 = uuid.v4

// export interface UseSendMessageType {
//   agentId: string
//   text: string
// }

// function updateMessageInQueryData(
//   queryClient: ReturnType<typeof useQueryClient>,
//   agentId: string,
//   response: any,
//   responseMessageId: string,
// ) {
//   queryClient.setQueriesData<AppMessage[] | undefined>(
//     {
//       queryKey: getAgentMessagesQueryKey(agentId),
//     },
//     (_data) => {
//       if (!_data) {
//         _data = []
//       }

//       const data = _data.filter((message) => Boolean(message) && message.id !== "deleteme_")
//       const existingMessage = data.find((message) => message.id === responseMessageId)

//       if (response.messageType === MESSAGE_TYPE.ASSISTANT_MESSAGE) {
//         if (existingMessage?.messageType === MESSAGE_TYPE.ASSISTANT_MESSAGE) {
//           return data.map((message) => {
//             if (message.id === responseMessageId) {
//               return {
//                 id: responseMessageId,
//                 date: new Date(response.date),
//                 messageType: MESSAGE_TYPE.ASSISTANT_MESSAGE,
//                 content: existingMessage.content + extractMessageText(response.content),
//               }
//             }
//             return message
//           })
//         }

//         return [
//           ...data,
//           {
//             id: responseMessageId,
//             date: new Date(response.date),
//             messageType: MESSAGE_TYPE.ASSISTANT_MESSAGE,
//             content: extractMessageText(response.content),
//           },
//         ]
//       }

//       if (response.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
//         if (existingMessage?.messageType === MESSAGE_TYPE.REASONING_MESSAGE) {
//           return data.map((message) => {
//             if (message.id === responseMessageId) {
//               return {
//                 id: responseMessageId,
//                 date: new Date(response.date),
//                 messageType: MESSAGE_TYPE.REASONING_MESSAGE,
//                 content: existingMessage.content + extractMessageText(response.reasoning),
//               }
//             }
//             return message
//           })
//         }

//         return [
//           ...data,
//           {
//             id: responseMessageId,
//             date: new Date(response.date),
//             messageType: MESSAGE_TYPE.REASONING_MESSAGE,
//             content: extractMessageText(response.reasoning),
//           },
//         ]
//       }

//       if (response.messageType === MESSAGE_TYPE.TOOL_CALL_MESSAGE) {
//         return data.map((message) => {
//           if (message.id === responseMessageId) {
//             return {
//               ...message,
//               messageType: MESSAGE_TYPE.TOOL_CALL_MESSAGE,
//               toolName: response.toolName,
//             }
//           }
//           return message
//         })
//       }

//       if (response.messageType === MESSAGE_TYPE.TOOL_RETURN_MESSAGE) {
//         return data.map((message) => {
//           if (message.id === responseMessageId) {
//             return {
//               ...message,
//               messageType: MESSAGE_TYPE.TOOL_RETURN_MESSAGE,
//               toolName: response.toolName,
//             }
//           }
//           return message
//         })
//       }

//       return data
//     },
//   )
// }

// export function useSendMessageSSE() {
//   const { lettaClient } = useLettaClient()
//   const queryClient = useQueryClient()
//   async function sendMessage(options: UseSendMessageType) {
//     const { agentId, text } = options

//     const userMessage: AppMessage = {
//       id: uuidv4(),
//       date: new Date(),
//       messageType: MESSAGE_TYPE.USER_MESSAGE,
//       content: text,
//     }

//     queryClient.setQueriesData<AppMessage[] | undefined>(
//       {
//         queryKey: getAgentMessagesQueryKey(agentId),
//       },
//       (_data) => {
//         const data = _data ?? []
//         return [...data, userMessage]
//       },
//     )

//     const streamResponse = await lettaClient.agents.messages
//       .createStream(agentId, {
//         streamTokens: true,
//         messages: [
//           {
//             role: ROLE_TYPE.USER,
//             content: text,
//           },
//         ],
//       })
//       .catch((error) => {
//         console.error("Error sending message:", error)
//       })

//     if (!streamResponse) {
//       return
//     }

//     for await (const response of streamResponse) {
//       const responseMessageId = getMessageId(response)
//       updateMessageInQueryData(queryClient, agentId, response, responseMessageId)
//     }
//   }

//   return useMutation<void, undefined, UseSendMessageType>({
//     mutationFn: (options) =>
//       sendMessage(options).catch((error) => {
//         console.error("Error sending message:", error)
//         throw error
//       }),
//   })
// }
