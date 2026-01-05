import { useLettaClient } from "@/providers/LettaProvider"
import { Letta } from "@letta-ai/letta-client"
import { useQuery, UseQueryOptions } from "@tanstack/react-query"
import { useUserId } from "./use-user-id"

export const AgentsQueryKey = ["agents"]
export const getAgentsQueryKey = () => AgentsQueryKey

const sortByLastCreated = (agents: Letta.AgentState[]) => {
  return agents.sort((a, b) => {
    const aCreatedAt = a.created_at ? new Date(a.created_at).getTime() : 0
    const bCreatedAt = b.created_at ? new Date(b.created_at).getTime() : 0
    return bCreatedAt - aCreatedAt
  })
}

export function useAgents(queryOptions?: UseQueryOptions<Letta.AgentState[]>) {
  const { lettaClient } = useLettaClient()
  const { data: userId } = useUserId()
  return useQuery({
    queryKey: getAgentsQueryKey(),
    queryFn: async () => {
      const result = await lettaClient.agents.list({
        tags: [userId!],
      })
      return Array.from(result)
    },
    select: sortByLastCreated,
    enabled: !!lettaClient && !!userId,
    ...queryOptions,
  })
}
