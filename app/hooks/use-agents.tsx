import { useLettaClient } from "@/providers/LettaProvider"
import { AgentState } from "@letta-ai/letta-client/resources/agents/agents"
import { useQuery, UseQueryOptions } from "@tanstack/react-query"

export const AgentsQueryKey = ["agents"]
export const getAgentsQueryKey = () => AgentsQueryKey

const sortByLastCreated = (agents: AgentState[]) => {
  return agents.sort((a, b) => {
    const aCreatedAt = a.created_at ? new Date(a.created_at).getTime() : 0
    const bCreatedAt = b.created_at ? new Date(b.created_at).getTime() : 0
    return bCreatedAt - aCreatedAt
  })
}

export function useAgents(queryOptions?: UseQueryOptions<AgentState[]>) {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getAgentsQueryKey(),
    queryFn: () =>
      lettaClient.agents
        .list({ include: ["agent.blocks", "agent.tools", "agent.tags"] })
        .then((page) => page.getPaginatedItems()),
    select: sortByLastCreated,
    enabled: !!lettaClient,
    ...queryOptions,
  })
}
