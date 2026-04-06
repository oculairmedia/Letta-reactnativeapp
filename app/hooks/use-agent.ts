"use client"

import { useLettaClient } from "@/providers/LettaProvider"
import { AgentState, AgentUpdateParams } from "@letta-ai/letta-client/resources/agents/agents"
import {
  useMutation,
  UseMutationOptions,
  useQuery,
  useQueryClient,
  UseQueryOptions,
} from "@tanstack/react-query"
import { getAgentsQueryKey } from "./use-agents"

export const getUseAgentStateKey = (agentId: string) => ["agentState", agentId]

export function useAgent(
  agentId: string,
  queryOptions?: Omit<UseQueryOptions<AgentState>, "queryKey" | "queryFn">,
) {
  const { lettaClient } = useLettaClient()
  return useQuery<AgentState>({
    queryKey: getUseAgentStateKey(agentId),
    queryFn: () => lettaClient.agents.retrieve(agentId),
    enabled: !!lettaClient && !!agentId,
    refetchInterval: 3000,
    ...queryOptions,
  })
}

export function useModifyAgent(
  agentId: string,
  mutationOptions?: UseMutationOptions<AgentState, Error, AgentUpdateParams>,
) {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (state: AgentUpdateParams) => lettaClient.agents.update(agentId, state),
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: getUseAgentStateKey(agentId) })
      queryClient.invalidateQueries({ queryKey: getAgentsQueryKey() })
      mutationOptions?.onSuccess?.(...args)
    },
    ...mutationOptions,
  })
}
