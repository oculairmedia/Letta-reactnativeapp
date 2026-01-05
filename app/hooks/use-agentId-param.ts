import { useAgentContext } from "@/providers/AgentProvider"

export function useAgentId() {
  const { agentId, setAgentId } = useAgentContext()

  return [agentId, setAgentId] as const
}
