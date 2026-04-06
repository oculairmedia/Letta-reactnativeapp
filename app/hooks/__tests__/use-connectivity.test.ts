import { renderHook, act, waitFor } from "@testing-library/react-native"
import { useConnectivity } from "../use-connectivity"
import { useLettaClient } from "@/providers/LettaProvider"
import NetInfo from "@react-native-community/netinfo"

// Mock the LettaProvider
jest.mock("@/providers/LettaProvider", () => ({
  useLettaClient: jest.fn(),
}))

// Mock NetInfo
jest.mock("@react-native-community/netinfo", () => ({
  addEventListener: jest.fn(() => jest.fn()),
}))

const mockUseLettaClient = useLettaClient as jest.MockedFunction<typeof useLettaClient>
const mockAddEventListener = NetInfo.addEventListener as jest.MockedFunction<
  typeof NetInfo.addEventListener
>

describe("useConnectivity", () => {
  let netInfoCallback: ((state: any) => void) | null = null
  const mockUnsubscribe = jest.fn()

  beforeEach(() => {
    jest.clearAllMocks()
    netInfoCallback = null

    // Capture the callback passed to addEventListener
    mockAddEventListener.mockImplementation((callback) => {
      netInfoCallback = callback
      return mockUnsubscribe
    })

    // Default mock
    mockUseLettaClient.mockReturnValue({
      lettaClient: {
        agents: {
          list: jest.fn().mockResolvedValue([]),
        },
      },
    } as any)
  })

  it("should initialize with default connected state", () => {
    const { result } = renderHook(() => useConnectivity())

    expect(result.current.isConnected).toBe(true)
    expect(result.current.isInternetReachable).toBe(null)
    expect(result.current.isServerReachable).toBe(true)
    // isChecking may be true if server check starts immediately
    expect(typeof result.current.isChecking).toBe("boolean")
  })

  it("should subscribe to NetInfo on mount", () => {
    renderHook(() => useConnectivity())

    expect(mockAddEventListener).toHaveBeenCalled()
  })

  it("should unsubscribe from NetInfo on unmount", () => {
    const { unmount } = renderHook(() => useConnectivity())
    unmount()

    expect(mockUnsubscribe).toHaveBeenCalled()
  })

  it("should update isConnected when network changes to disconnected", () => {
    const { result } = renderHook(() => useConnectivity())

    act(() => {
      netInfoCallback?.({
        isConnected: false,
        isInternetReachable: false,
      })
    })

    expect(result.current.isConnected).toBe(false)
    expect(result.current.isInternetReachable).toBe(false)
  })

  it("should update isConnected when network changes to connected", () => {
    const { result } = renderHook(() => useConnectivity())

    // First disconnect
    act(() => {
      netInfoCallback?.({
        isConnected: false,
        isInternetReachable: false,
      })
    })

    // Then reconnect
    act(() => {
      netInfoCallback?.({
        isConnected: true,
        isInternetReachable: true,
      })
    })

    expect(result.current.isConnected).toBe(true)
    expect(result.current.isInternetReachable).toBe(true)
  })

  it("should return isOffline true when not connected", () => {
    const { result } = renderHook(() => useConnectivity())

    act(() => {
      netInfoCallback?.({
        isConnected: false,
        isInternetReachable: false,
      })
    })

    expect(result.current.isOffline).toBe(true)
  })

  it("should return isOffline true when internet is not reachable", () => {
    const { result } = renderHook(() => useConnectivity())

    act(() => {
      netInfoCallback?.({
        isConnected: true,
        isInternetReachable: false,
      })
    })

    expect(result.current.isOffline).toBe(true)
  })

  it("should return isOffline false when fully connected", () => {
    const { result } = renderHook(() => useConnectivity())

    // Initial state should be online (all defaults are true/null)
    expect(result.current.isOffline).toBe(false)
  })

  it("should set isServerReachable to false when lettaClient is null", async () => {
    mockUseLettaClient.mockReturnValue({
      lettaClient: null,
    } as any)

    const { result } = renderHook(() => useConnectivity())

    // Trigger network change to start server check
    act(() => {
      netInfoCallback?.({
        isConnected: true,
        isInternetReachable: true,
      })
    })

    await waitFor(() => {
      expect(result.current.isServerReachable).toBe(false)
    })
  })

  it("should compute isOffline correctly based on all connection states", () => {
    const { result } = renderHook(() => useConnectivity())

    // All connected - should not be offline
    expect(result.current.isOffline).toBe(false)

    // Only network disconnected
    act(() => {
      netInfoCallback?.({
        isConnected: false,
        isInternetReachable: true,
      })
    })
    expect(result.current.isOffline).toBe(true)

    // Network connected but internet not reachable
    act(() => {
      netInfoCallback?.({
        isConnected: true,
        isInternetReachable: false,
      })
    })
    expect(result.current.isOffline).toBe(true)
  })

  it("should handle null isConnected value from NetInfo", () => {
    const { result } = renderHook(() => useConnectivity())

    act(() => {
      netInfoCallback?.({
        isConnected: null,
        isInternetReachable: null,
      })
    })

    // Should default to true when null
    expect(result.current.isConnected).toBe(true)
  })
})
