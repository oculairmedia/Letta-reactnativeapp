import { renderHook, waitFor } from "@testing-library/react-native"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { useUpdateBlock } from "../use-blocks"
import { useLettaClient } from "@/providers/LettaProvider"
import { createElement, type ReactNode } from "react"

// Mock the LettaProvider
jest.mock("@/providers/LettaProvider", () => ({
  useLettaClient: jest.fn(),
}))

const mockUseLettaClient = useLettaClient as jest.MockedFunction<typeof useLettaClient>

describe("use-blocks hooks", () => {
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
        mutations: {
          retry: false,
        },
      },
    })
    jest.clearAllMocks()
  })

  afterEach(() => {
    queryClient.clear()
  })

  describe("useUpdateBlock", () => {
    it("should update a block value", async () => {
      const mockUpdate = jest.fn().mockResolvedValue({
        id: "block-1",
        value: "Updated value",
      })

      mockUseLettaClient.mockReturnValue({
        lettaClient: {
          blocks: {
            update: mockUpdate,
          },
        },
      } as any)

      const { result } = renderHook(() => useUpdateBlock(), {
        wrapper: createWrapper(),
      })

      await result.current.mutateAsync({
        id: "block-1",
        block: { value: "Updated value" },
      })

      expect(mockUpdate).toHaveBeenCalledWith("block-1", { value: "Updated value" })
    })

    it("should update block with multiple fields", async () => {
      const mockUpdate = jest.fn().mockResolvedValue({
        id: "block-1",
        value: "New value",
        label: "New label",
      })

      mockUseLettaClient.mockReturnValue({
        lettaClient: {
          blocks: {
            update: mockUpdate,
          },
        },
      } as any)

      const { result } = renderHook(() => useUpdateBlock(), {
        wrapper: createWrapper(),
      })

      await result.current.mutateAsync({
        id: "block-1",
        block: { value: "New value", label: "New label" },
      })

      expect(mockUpdate).toHaveBeenCalledWith("block-1", {
        value: "New value",
        label: "New label",
      })
    })

    it("should call onSuccess callback when provided", async () => {
      const mockUpdate = jest.fn().mockResolvedValue({
        id: "block-1",
        value: "Updated value",
      })

      mockUseLettaClient.mockReturnValue({
        lettaClient: {
          blocks: {
            update: mockUpdate,
          },
        },
      } as any)

      const onSuccessMock = jest.fn()

      const { result } = renderHook(() => useUpdateBlock({ onSuccess: onSuccessMock }), {
        wrapper: createWrapper(),
      })

      await result.current.mutateAsync({
        id: "block-1",
        block: { value: "Updated value" },
      })

      await waitFor(() => {
        expect(onSuccessMock).toHaveBeenCalled()
      })
    })

    it("should handle update errors", async () => {
      const mockError = new Error("Update failed")
      const mockUpdate = jest.fn().mockRejectedValue(mockError)

      mockUseLettaClient.mockReturnValue({
        lettaClient: {
          blocks: {
            update: mockUpdate,
          },
        },
      } as any)

      const { result } = renderHook(() => useUpdateBlock(), {
        wrapper: createWrapper(),
      })

      await expect(
        result.current.mutateAsync({
          id: "block-1",
          block: { value: "Updated value" },
        }),
      ).rejects.toThrow("Update failed")
    })
  })
})
