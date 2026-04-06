import { Text } from "@/components/Text"
import { TextField } from "@/components/TextField"
import { useAgent, getUseAgentStateKey } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { useUpdateBlock } from "@/hooks/use-blocks"
import { Accordion } from "@/shared/components/animated/Accordion"
import { FC, useCallback, useEffect, useMemo, useState } from "react"
import { TextStyle, TouchableOpacity, View, ViewStyle } from "react-native"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { useQueryClient } from "@tanstack/react-query"
import { Icon } from "@/components"

interface AgentBlocksProps {
  style?: ViewStyle
}

export const AgentBlocks: FC<AgentBlocksProps> = ({ style }) => {
  const {
    themed,
    theme: { colors },
  } = useAppTheme()
  const [agentId] = useAgentId()
  const { data: agent, isLoading } = useAgent(agentId)
  const [expandedBlocks, setExpandedBlocks] = useState<Record<string, boolean>>({})
  const [editValues, setEditValues] = useState<Record<string, string>>({})
  const queryClient = useQueryClient()

  const { mutate: updateBlock, isPending } = useUpdateBlock({
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getUseAgentStateKey(agentId) })
    },
  })

  const blocks = useMemo(() => {
    if (!agent?.blocks) return []
    return agent.blocks
  }, [agent])

  // Initialize edit values when blocks load
  useEffect(() => {
    const initialValues: Record<string, string> = {}
    for (const block of blocks) {
      if (block.id) {
        initialValues[block.id] = block.value || ""
      }
    }
    setEditValues(initialValues)
  }, [blocks])

  const toggleBlock = useCallback((blockId: string) => {
    setExpandedBlocks((prev) => ({
      ...prev,
      [blockId]: !prev[blockId],
    }))
  }, [])

  const handleValueChange = useCallback((blockId: string, value: string) => {
    setEditValues((prev) => ({
      ...prev,
      [blockId]: value,
    }))
  }, [])

  const handleSave = useCallback(
    (blockId: string) => {
      const newValue = editValues[blockId]
      if (newValue !== undefined) {
        updateBlock({ id: blockId, block: { value: newValue } })
      }
    },
    [editValues, updateBlock],
  )

  const handleReset = useCallback(
    (blockId: string) => {
      const originalBlock = blocks.find((b) => b.id === blockId)
      if (originalBlock) {
        setEditValues((prev) => ({
          ...prev,
          [blockId]: originalBlock.value || "",
        }))
      }
    },
    [blocks],
  )

  const isModified = useCallback(
    (blockId: string) => {
      const originalBlock = blocks.find((b) => b.id === blockId)
      return editValues[blockId] !== (originalBlock?.value || "")
    },
    [blocks, editValues],
  )

  if (isLoading) {
    return (
      <View style={[$blocksContainer, style]}>
        <Text preset="formHelper" text="Loading memory blocks..." style={themed($emptyText)} />
      </View>
    )
  }

  if (!blocks.length) {
    return (
      <View style={[$blocksContainer, style]}>
        <Text preset="formHelper" text="No memory blocks configured" style={themed($emptyText)} />
      </View>
    )
  }

  return (
    <View style={[$blocksContainer, style]}>
      {blocks.map((block) => {
        const blockId = block.id ?? ""
        const blockModified = isModified(blockId)

        return (
          <Accordion
            key={blockId}
            isExpanded={expandedBlocks[blockId] ?? false}
            onToggle={() => blockId && toggleBlock(blockId)}
            text={block.label || "Unnamed Block"}
            preset="reversed"
          >
            <View style={$blockContent}>
              <TextField
                value={editValues[blockId] ?? ""}
                onChangeText={(text) => handleValueChange(blockId, text)}
                multiline
                style={$blockValueInput}
                editable={!isPending}
              />
              {blockModified && (
                <View style={$actionsRow}>
                  <TouchableOpacity
                    onPress={() => handleSave(blockId)}
                    style={[themed($actionButton), themed($saveButton)]}
                    disabled={isPending}
                  >
                    <Icon icon="Check" size={16} color={colors.palette.neutral100} />
                    <Text size="xs" style={{ color: colors.palette.neutral100 }}>
                      Save
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => handleReset(blockId)}
                    style={themed($actionButton)}
                    disabled={isPending}
                  >
                    <Icon icon="X" size={16} color={colors.textDim} />
                    <Text size="xs" style={{ color: colors.textDim }}>
                      Reset
                    </Text>
                  </TouchableOpacity>
                </View>
              )}
              {block.description && (
                <Text preset="formHelper" text={block.description} style={$blockDescription} />
              )}
            </View>
          </Accordion>
        )
      })}
    </View>
  )
}

const $blocksContainer: ViewStyle = {
  flexDirection: "column",
  gap: spacing.sm,
}

const $blockContent: ViewStyle = {
  padding: spacing.sm,
  gap: spacing.sm,
}

const $blockDescription: ViewStyle = {
  marginTop: spacing.xs,
}

const $emptyText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $actionsRow: ViewStyle = {
  flexDirection: "row",
  gap: spacing.sm,
  justifyContent: "flex-end",
}

const $actionButton: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.xxs,
  paddingVertical: spacing.xs,
  paddingHorizontal: spacing.sm,
  borderRadius: spacing.xs,
  borderWidth: 1,
  borderColor: colors.palette.overlay20,
})

const $saveButton: ThemedStyle<ViewStyle> = ({ colors }) => ({
  backgroundColor: colors.tint,
  borderColor: colors.tint,
})

const $blockValueInput: TextStyle = {
  fontSize: 14,
  lineHeight: 20,
  minHeight: 80,
}
