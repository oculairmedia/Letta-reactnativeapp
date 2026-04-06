import { useLettaClient } from "@/providers/LettaProvider"
import { Conversation } from "@letta-ai/letta-client/resources/conversations/conversations"
import { useInfiniteQuery, UseInfiniteQueryOptions } from "@tanstack/react-query"

export const getAllConversationsQueryKey = () => ["allConversations"]

const PAGE_SIZE = 50

interface ConversationsPage {
  items: Conversation[]
  nextCursor: string | null
}

export function useAllConversations(
  queryOptions?: Omit<
    UseInfiniteQueryOptions<ConversationsPage, Error, Conversation[]>,
    "queryKey" | "queryFn" | "getNextPageParam" | "initialPageParam"
  >,
) {
  const { lettaClient } = useLettaClient()

  return useInfiniteQuery<ConversationsPage, Error, Conversation[]>({
    queryKey: getAllConversationsQueryKey(),
    queryFn: async ({ pageParam }) => {
      const response = await lettaClient.conversations.list({
        order: "desc",
        order_by: "last_message_at",
        limit: PAGE_SIZE,
        ...(pageParam ? { after: pageParam as string } : {}),
      })

      // The response is an array of conversations
      // Check if there are more pages by seeing if we got a full page
      const items = Array.isArray(response) ? response : []
      const hasMore = items.length === PAGE_SIZE
      const nextCursor = hasMore && items.length > 0 ? items[items.length - 1].id : null

      return {
        items,
        nextCursor,
      }
    },
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    select: (data) => data.pages.flatMap((page) => page.items),
    enabled: !!lettaClient,
    ...queryOptions,
  })
}
