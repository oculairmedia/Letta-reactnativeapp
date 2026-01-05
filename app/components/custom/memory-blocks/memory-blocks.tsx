import { View, ViewStyle } from "react-native"
import { Text } from "@/components/Text"
import { spacing } from "@/theme"
import { getUseAgentStateKey, useAgent } from "@/hooks/use-agent"
import { useModifyBlock } from "@/hooks/use-blocks"
import { MemoryBlockItem } from "./memory-block"
import { MemoryBlockProps } from "./types"
import { useMemo } from "react"
import { useQueryClient } from "@tanstack/react-query"

export function MemoryBlocks({ agentId }: MemoryBlockProps) {
  const { data: agent, isLoading: isAgentLoading } = useAgent(agentId)
  const queryClient = useQueryClient()
  const { mutate: modifyBlock } = useModifyBlock({
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getUseAgentStateKey(agentId) })
    },
  })

  const { personaBlock, humanBlock } = useMemo(() => {
    const blocks = agent?.memory?.blocks || []
    const personaBlock = blocks.find((block) => block.label === "persona")
    const humanBlock = blocks.find((block) => block.label === "human")
    return { personaBlock, humanBlock }
  }, [agent])

  const handleModifyBlock = (blockId: string, value: string) => {
    modifyBlock({
      id: blockId,
      block: { value },
    })
  }

  return (
    <View style={$container}>
      <Text preset="bold" text="Memory Blocks" style={$title} />

      <MemoryBlockItem
        label="Persona"
        value={personaBlock?.value || ""}
        isLoading={isAgentLoading}
        onSave={(value) => personaBlock?.id && handleModifyBlock(personaBlock.id, value)}
      />

      <MemoryBlockItem
        label="Human"
        value={humanBlock?.value || ""}
        isLoading={isAgentLoading}
        onSave={(value) => humanBlock?.id && handleModifyBlock(humanBlock.id, value)}
      />
    </View>
  )
}

const $container: ViewStyle = {
  gap: spacing.xxs,
}

const $title: ViewStyle = {
  marginBottom: spacing.xs,
}
