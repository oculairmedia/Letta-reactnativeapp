import { Text } from "@/components/Text"
import { useAgent } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { Accordion } from "@/shared/components/animated/Accordion"
import { FC, useCallback, useMemo, useState } from "react"
import { View, ViewStyle } from "react-native"
import { spacing } from "@/theme"

interface AgentBlocksProps {
  style?: ViewStyle
}

export const AgentBlocks: FC<AgentBlocksProps> = ({ style }) => {
  const [agentId] = useAgentId()
  const { data: agent } = useAgent(agentId)
  const [expandedBlocks, setExpandedBlocks] = useState<Record<string, boolean>>({})

  const blocks = useMemo(() => {
    if (!agent?.blocks) return []
    return agent.blocks
  }, [agent])

  const toggleBlock = useCallback((blockId: string) => {
    setExpandedBlocks((prev) => ({
      ...prev,
      [blockId]: !prev[blockId],
    }))
  }, [])

  return (
    <View style={[$blocksContainer, style]}>
      {blocks.map((block) => (
        <Accordion
          key={block.id}
          isExpanded={expandedBlocks[block.id ?? ""] ?? false}
          onToggle={() => block.id && toggleBlock(block.id)}
          text={block.label || "Unnamed Block"}
          preset="reversed"
        >
          <View style={$blockContent}>
            <Text text={block.value} />
            {block.description && (
              <Text preset="formHelper" text={block.description} style={$blockDescription} />
            )}
          </View>
        </Accordion>
      ))}
    </View>
  )
}

const $blocksContainer: ViewStyle = {
  flexDirection: "column",
  gap: spacing.sm,
}

const $blockContent: ViewStyle = {
  padding: spacing.sm,
  gap: spacing.xs,
}

const $blockDescription: ViewStyle = {
  marginTop: spacing.xs,
}
