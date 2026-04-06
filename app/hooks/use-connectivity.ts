import NetInfo, { NetInfoState } from "@react-native-community/netinfo"
import { useEffect, useState } from "react"
import { useLettaClient } from "@/providers/LettaProvider"

export interface ConnectivityState {
  isConnected: boolean
  isInternetReachable: boolean | null
  isServerReachable: boolean
  isChecking: boolean
}

export function useConnectivity() {
  const { lettaClient } = useLettaClient()
  const [state, setState] = useState<ConnectivityState>({
    isConnected: true,
    isInternetReachable: null,
    isServerReachable: true,
    isChecking: false,
  })

  useEffect(() => {
    // Subscribe to network state updates
    const unsubscribe = NetInfo.addEventListener((netState: NetInfoState) => {
      setState((prev) => ({
        ...prev,
        isConnected: netState.isConnected ?? true,
        isInternetReachable: netState.isInternetReachable,
      }))
    })

    return () => unsubscribe()
  }, [])

  useEffect(() => {
    // Check server reachability when network changes
    const checkServer = async () => {
      if (!lettaClient || !state.isConnected) {
        setState((prev) => ({ ...prev, isServerReachable: false }))
        return
      }

      setState((prev) => ({ ...prev, isChecking: true }))
      try {
        // Try to list agents as a health check
        await lettaClient.agents.list({ limit: 1 })
        setState((prev) => ({ ...prev, isServerReachable: true, isChecking: false }))
      } catch {
        setState((prev) => ({ ...prev, isServerReachable: false, isChecking: false }))
      }
    }

    // Only check if we have internet connectivity
    if (state.isConnected && state.isInternetReachable !== false) {
      checkServer()
    }
  }, [lettaClient, state.isConnected, state.isInternetReachable])

  return {
    ...state,
    isOffline:
      !state.isConnected || state.isInternetReachable === false || !state.isServerReachable,
  }
}
