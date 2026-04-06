import NetInfo, { NetInfoState } from "@react-native-community/netinfo"
import { useEffect, useRef, useState } from "react"
import { useLettaClient } from "@/providers/LettaProvider"

export interface ConnectivityState {
  isConnected: boolean
  isInternetReachable: boolean | null
  isServerReachable: boolean
  isChecking: boolean
}

const HEALTH_CHECK_DEBOUNCE_MS = 15000 // 15 seconds between checks

export function useConnectivity() {
  const { lettaClient } = useLettaClient()
  const [state, setState] = useState<ConnectivityState>({
    isConnected: true,
    isInternetReachable: null,
    isServerReachable: true,
    isChecking: false,
  })
  const lastCheckRef = useRef<number>(0)
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

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
    // Check server reachability when network changes (debounced)
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
      lastCheckRef.current = Date.now()
    }

    // Only check if we have internet connectivity
    if (state.isConnected && state.isInternetReachable !== false) {
      const timeSinceLastCheck = Date.now() - lastCheckRef.current

      // Clear any pending debounce timer
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }

      if (timeSinceLastCheck >= HEALTH_CHECK_DEBOUNCE_MS) {
        // Enough time has passed, check immediately
        checkServer()
      } else {
        // Schedule a check after the debounce period
        debounceTimerRef.current = setTimeout(
          checkServer,
          HEALTH_CHECK_DEBOUNCE_MS - timeSinceLastCheck,
        )
      }
    }

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, [lettaClient, state.isConnected, state.isInternetReachable])

  return {
    ...state,
    isOffline:
      !state.isConnected || state.isInternetReachable === false || !state.isServerReachable,
  }
}
