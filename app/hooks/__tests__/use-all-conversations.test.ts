import { renderHook, waitFor } from "@testing-library/react-native"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { useAllConversations } from "../use-all-conversations"
import { useLettaClient } from "@/providers/LettaProvider"
import { createElement, type ReactNode } from "react"

// Mock the LettaProvider
jest.mock("@/providers/LettaProvider", () => ({
  useLettaClient: jest.fn(),
}))

const mockUseLettaClient = useLettaClient as jest.MockedFunction<typeof useLettaClient>

describe("useAllConversations", () => {
  let queryClient: QueryClient

  const createWrapper = () => {
    return ({ children }: { children: ReactNode }) =>
      createElement(QueryClientProvider, { client: queryClient }, children)
  }

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    })
    jest.clearAllMocks()
  })

  afterEach(() => {
    queryClient.clear()
  })

  it("should fetch conversations with default parameters", async () => {
    const mockConversations = [
      { id: "conv-1", agent_id: "agent-1", summary: "Conversation 1" },
      { id: "conv-2", agent_id: "agent-2", summary: "Conversation 2" },
    ]

    const mockList = jest.fn().mockResolvedValue(mockConversations)

    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        conversations: {
          list: mockList,
        },
      },
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })

    expect(mockList).toHaveBeenCalledWith({
      order: "desc",
      order_by: "last_message_at",
      limit: 50,
    })

    // Data should be flattened from pages
    expect(result.current.data).toEqual(mockConversations)
  })

  it("should return empty array when no conversations exist", async () => {
    const mockList = jest.fn().mockResolvedValue([])

    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        conversations: {
          list: mockList,
        },
      },
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })

    expect(result.current.data).toEqual([])
  })

  it("should not fetch when lettaClient is not available", async () => {
    mockUseLettaClient.mockReturnValue({
      lettaClient: null,
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    expect(result.current.isFetching).toBe(false)
    expect(result.current.data).toBeUndefined()
  })

  it("should indicate hasNextPage when page is full", async () => {
    // Create 50 conversations (full page)
    const mockConversations = Array.from({ length: 50 }, (_, i) => ({
      id: `conv-${i}`,
      agent_id: "agent-1",
      summary: `Conversation ${i}`,
    }))

    const mockList = jest.fn().mockResolvedValue(mockConversations)

    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        conversations: {
          list: mockList,
        },
      },
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })

    expect(result.current.hasNextPage).toBe(true)
  })

  it("should indicate no next page when page is not full", async () => {
    // Create less than 50 conversations
    const mockConversations = [
      { id: "conv-1", agent_id: "agent-1", summary: "Conversation 1" },
      { id: "conv-2", agent_id: "agent-2", summary: "Conversation 2" },
    ]

    const mockList = jest.fn().mockResolvedValue(mockConversations)

    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        conversations: {
          list: mockList,
        },
      },
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })

    expect(result.current.hasNextPage).toBe(false)
  })

  it("should fetch next page with cursor", async () => {
    const firstPage = Array.from({ length: 50 }, (_, i) => ({
      id: `conv-${i}`,
      agent_id: "agent-1",
      summary: `Conversation ${i}`,
    }))

    const secondPage = [
      { id: "conv-50", agent_id: "agent-1", summary: "Conversation 50" },
      { id: "conv-51", agent_id: "agent-1", summary: "Conversation 51" },
    ]

    const mockList = jest
      .fn()
      .mockResolvedValueOnce(firstPage)
      .mockResolvedValueOnce(secondPage)

    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        conversations: {
          list: mockList,
        },
      },
    } as any)

    const { result } = renderHook(() => useAllConversations(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => {
      expect(result.current.data).toBeDefined()
    })

    // Fetch next page
    await result.current.fetchNextPage()

    await waitFor(() => {
      expect(result.current.data?.length).toBe(52)
    })

    // Second call should include the cursor (after parameter)
    expect(mockList).toHaveBeenCalledTimes(2)
    expect(mockList).toHaveBeenLastCalledWith({
      order: "desc",
      order_by: "last_message_at",
      limit: 50,
      after: "conv-49",
    })
  })
})
