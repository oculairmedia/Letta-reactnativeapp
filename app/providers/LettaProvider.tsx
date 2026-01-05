import Config from "@/config"
import { useStores } from "@/models"
import { Letta } from "@letta-ai/letta-client"
import { createContext, useCallback, useContext, useEffect, useState } from "react"

type LettaConfigContext = {
  lettaClient: Letta
  setConfig: (serverUrl: string, accessToken: string) => void
  resetConfig: () => void
}
const clientContext = createContext<LettaConfigContext>({} as never)
export const LettaClientProvider = ({ children }: { children: React.ReactNode }) => {
  const [lettaClient, setClient] = useState<Letta | null>(null)
  const setConfig = useCallback((serverUrl: string, accessToken: string) => {
    setClient(
      () =>
        new Letta({
          baseURL: serverUrl,
          apiKey: accessToken,
        }),
    )
  }, [])

  const resetConfig = useCallback(() => {
    setClient(null)
  }, [])

  const {
    lettaConfigStore: { save, reset, serverUrl, accessToken },
  } = useStores()

  useEffect(() => {
    if (lettaClient) {
      save()
    } else {
      reset()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lettaClient])

  // Auto-initialize client on mount if config exists
  useEffect(() => {
    if (!lettaClient && serverUrl && accessToken) {
      setConfig(Config.lettaBaseUrl, Config.lettaAccessToken)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <clientContext.Provider
      value={{
        lettaClient: lettaClient!,
        setConfig,
        resetConfig,
      }}
    >
      {children}
    </clientContext.Provider>
  )
}

export const useLettaClient = () => {
  const context = useContext(clientContext)

  if (!context) {
    throw new Error("LettaClient not found")
  }

  return context
}
