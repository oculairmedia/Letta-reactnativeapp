import { Text } from "@/components/Text"
import { useAgent } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { Accordion } from "@/shared/components/animated/Accordion"
import { FC, useCallback, useMemo, useState } from "react"
import { TextStyle, View, ViewStyle } from "react-native"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"

interface AgentBlocksProps {
  style?: ViewStyle
}

export const AgentBlocks: FC<AgentBlocksProps> = ({ style }) => {
  const { themed } = useAppTheme()
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

  if (!blocks.length) {
    return (
      <View style={[$blocksContainer, style]}>
        <Text preset="formHelper" text="No memory blocks configured" style={themed($emptyText)} />
      </View>
    )
  }

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

const $emptyText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})
