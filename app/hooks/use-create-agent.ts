import { useLettaClient } from "@/providers/LettaProvider"
import defaultAgent from "@/utils/default-agent.json"
import { Letta } from "@letta-ai/letta-client"
import { useMutation, UseMutationOptions, useQueryClient } from "@tanstack/react-query"
import { getAgentsQueryKey } from "./use-agents"
import { useUserId } from "./use-user-id"
import { foramtToSlug } from "@/utils/agent-name-prompt"

export function useCreateAgent(
  mutationOptions: UseMutationOptions<Letta.AgentState, Error, Letta.AgentCreateParams> = {},
) {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()
  const { data: userId } = useUserId()
  return useMutation<Letta.AgentState, Error, Letta.AgentCreateParams>({
    mutationFn: async (data: Letta.AgentCreateParams = {}) => {
      if (data.tags) {
        data.tags = data.tags.map(foramtToSlug)
      }

      if (data.name) {
        data.name = foramtToSlug(data.name)
      }

      return await lettaClient.agents.create({
        memory_blocks: defaultAgent.DEFAULT_MEMORY_BLOCKS,
        model: defaultAgent.DEFAULT_LLM,
        embedding: defaultAgent.DEFAULT_EMBEDDING,
        description: defaultAgent.DEFAULT_DESCRIPTION,
        context_window_limit: defaultAgent.DEFAULT_CONTEXT_WINDOW_LIMIT,
        ...data,
        name: data.name,
        tags: [userId!, ...(data.tags || [])],
      })
    },

    ...mutationOptions,
    onSuccess: async (...args) => {
      queryClient.invalidateQueries({ queryKey: getAgentsQueryKey() })
      // reset chat messages
      await lettaClient.agents.messages.reset(args[0].id, {
        add_default_initial_messages: false,
      })
      mutationOptions?.onSuccess?.(...args)
    },
    onError: (error, variables, context) => {
      console.error(error)
      mutationOptions?.onError?.(error, variables, context)
    },
  })
}
