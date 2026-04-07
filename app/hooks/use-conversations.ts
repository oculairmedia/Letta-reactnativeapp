import { useLettaClient } from "@/providers/LettaProvider"
import { Conversation } from "@letta-ai/letta-client/resources/conversations/conversations"
import { useMutation, useQuery, useQueryClient, UseQueryOptions } from "@tanstack/react-query"
import { Alert } from "react-native"
import { getAllConversationsQueryKey } from "./use-all-conversations"

export const getConversationsQueryKey = (agentId: string) => ["conversations", agentId]

export function useConversations(
  agentId: string,
  queryOptions?: Omit<UseQueryOptions<Conversation[]>, "queryKey" | "queryFn">,
) {
  const { lettaClient } = useLettaClient()
  return useQuery<Conversation[]>({
    queryKey: getConversationsQueryKey(agentId),
    queryFn: async () => {
      const conversations = await lettaClient.conversations.list({
        agent_id: agentId,
        order: "desc",
        order_by: "last_message_at",
      })
      return conversations
    },
    enabled: !!agentId && !!lettaClient,
    ...queryOptions,
  })
}

export function useCreateConversation() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ agentId, summary }: { agentId: string; summary?: string }) => {
      return lettaClient.conversations.create({
        agent_id: agentId,
        summary,
      })
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({
        queryKey: getConversationsQueryKey(data.agent_id),
      })
    },
  })
}

export function useDeleteConversation() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      conversationId,
      agentId,
    }: {
      conversationId: string
      agentId: string
    }) => {
      await lettaClient.conversations.delete(conversationId)
      return { conversationId, agentId }
    },
    onMutate: async (variables) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({
        queryKey: getConversationsQueryKey(variables.agentId),
      })
      await queryClient.cancelQueries({
        queryKey: getAllConversationsQueryKey(),
      })

      // Snapshot current data
      const previousConversations = queryClient.getQueryData<Conversation[]>(
        getConversationsQueryKey(variables.agentId),
      )
      const previousAllConversations = queryClient.getQueryData<{
        pages: Array<{ items: Conversation[]; nextCursor: string | null }>
        pageParams: unknown[]
      }>(getAllConversationsQueryKey())

      // Optimistically remove from per-agent cache
      if (previousConversations) {
        queryClient.setQueryData<Conversation[]>(
          getConversationsQueryKey(variables.agentId),
          previousConversations.filter((c) => c.id !== variables.conversationId),
        )
      }

      // Optimistically remove from all conversations infinite query cache
      if (previousAllConversations) {
        queryClient.setQueryData<{
          pages: Array<{ items: Conversation[]; nextCursor: string | null }>
          pageParams: unknown[]
        }>(getAllConversationsQueryKey(), {
          ...previousAllConversations,
          pages: previousAllConversations.pages.map((page) => ({
            ...page,
            items: page.items.filter((c) => c.id !== variables.conversationId),
          })),
        })
      }

      // Return context with snapshots for rollback
      return { previousConversations, previousAllConversations }
    },
    onError: (error: Error, variables, context) => {
      // Rollback to previous state
      if (context?.previousConversations) {
        queryClient.setQueryData<Conversation[]>(
          getConversationsQueryKey(variables.agentId),
          context.previousConversations,
        )
      }
      if (context?.previousAllConversations) {
        queryClient.setQueryData(getAllConversationsQueryKey(), context.previousAllConversations)
      }

      Alert.alert("Error", `Failed to delete conversation: ${error.message}`)
    },
    onSettled: (_, __, variables) => {
      // Invalidate to resync with server
      queryClient.invalidateQueries({
        queryKey: getConversationsQueryKey(variables.agentId),
      })
      queryClient.invalidateQueries({
        queryKey: getAllConversationsQueryKey(),
      })
    },
  })
}

export function useUpdateConversation() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      conversationId,
      agentId,
      summary,
    }: {
      conversationId: string
      agentId: string
      summary: string
    }) => {
      await lettaClient.conversations.update(conversationId, { summary })
      return { conversationId, agentId }
    },
    onMutate: async (variables) => {
      await queryClient.cancelQueries({
        queryKey: getConversationsQueryKey(variables.agentId),
      })
      await queryClient.cancelQueries({
        queryKey: getAllConversationsQueryKey(),
      })

      const previousConversations = queryClient.getQueryData<Conversation[]>(
        getConversationsQueryKey(variables.agentId),
      )
      const previousAllConversations = queryClient.getQueryData<{
        pages: Array<{ items: Conversation[]; nextCursor: string | null }>
        pageParams: unknown[]
      }>(getAllConversationsQueryKey())

      if (previousConversations) {
        queryClient.setQueryData<Conversation[]>(
          getConversationsQueryKey(variables.agentId),
          previousConversations.map((c) =>
            c.id === variables.conversationId ? { ...c, summary: variables.summary } : c,
          ),
        )
      }

      if (previousAllConversations) {
        queryClient.setQueryData<{
          pages: Array<{ items: Conversation[]; nextCursor: string | null }>
          pageParams: unknown[]
        }>(getAllConversationsQueryKey(), {
          ...previousAllConversations,
          pages: previousAllConversations.pages.map((page) => ({
            ...page,
            items: page.items.map((c) =>
              c.id === variables.conversationId ? { ...c, summary: variables.summary } : c,
            ),
          })),
        })
      }

      return { previousConversations, previousAllConversations }
    },
    onError: (error: Error, variables, context) => {
      if (context?.previousConversations) {
        queryClient.setQueryData<Conversation[]>(
          getConversationsQueryKey(variables.agentId),
          context.previousConversations,
        )
      }
      if (context?.previousAllConversations) {
        queryClient.setQueryData(getAllConversationsQueryKey(), context.previousAllConversations)
      }

      Alert.alert("Error", `Failed to update conversation: ${error.message}`)
    },
    onSettled: (_, __, variables) => {
      queryClient.invalidateQueries({
        queryKey: getConversationsQueryKey(variables.agentId),
      })
      queryClient.invalidateQueries({
        queryKey: getAllConversationsQueryKey(),
      })
    },
  })
}

export function useForkConversation() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      conversationId,
      agentId,
    }: {
      conversationId: string
      agentId: string
    }) => {
      // Fork creates a new conversation with the same history
      const forked = await lettaClient.conversations.fork(conversationId)
      return { forked, agentId }
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: getConversationsQueryKey(variables.agentId),
      })
      queryClient.invalidateQueries({
        queryKey: getAllConversationsQueryKey(),
      })
    },
    onError: (error: Error) => {
      Alert.alert("Error", `Failed to fork conversation: ${error.message}`)
    },
  })
}
