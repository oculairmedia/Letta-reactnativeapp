import { useLettaClient } from "@/providers/LettaProvider"
import { useQuery } from "@tanstack/react-query"
import type { Letta } from "@letta-ai/letta-client"

export const getAgentChatContextKey = (agentId: string) => ["agentChatContext", agentId]

export function useAgentChatContext(agentId: string) {
  const { lettaClient } = useLettaClient()
  return useQuery<Letta.ContextWindowOverview>({
    queryKey: getAgentChatContextKey(agentId),
    queryFn: () => lettaClient.agents.context.retrieve(agentId),
    enabled: !!lettaClient && !!agentId,
    refetchInterval: 3000,
  })
}
