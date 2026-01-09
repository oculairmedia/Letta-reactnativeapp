import { useLettaClient } from "@/providers/LettaProvider"
import { useQuery } from "@tanstack/react-query"

export const getAgentChatContextKey = (agentId: string) => ["agentChatContext", agentId]

// TODO: ContextWindowOverview is removed. Returning agent state or null for now.
export function useAgentChatContext(agentId: string) {
  const { lettaClient } = useLettaClient()
  return useQuery<any>({
    queryKey: getAgentChatContextKey(agentId),
    queryFn: () => {
      return Promise.resolve({
        contextWindowSizeMax: 0,
        contextWindowSizeCurrent: 0,
        numTokensSystem: 0,
        numTokensFunctionsDefinitions: 0,
        numTokensExternalMemorySummary: 0,
        numTokensMessages: 0,
      })
    },
    enabled: !!lettaClient && !!agentId,
    refetchInterval: 3000,
  })
}
