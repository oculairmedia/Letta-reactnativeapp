import { Text } from "@/components/Text"
import { useAgent } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { Accordion } from "@/shared/components/animated/Accordion"
import { FC, useState } from "react"
import { View, ViewStyle } from "react-native"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"

interface AgentTagsProps {
  style?: ViewStyle
}

export const AgentTags: FC<AgentTagsProps> = ({ style }) => {
  const { themed } = useAppTheme()
  const [agentId] = useAgentId()
  const { data: agent } = useAgent(agentId)
  const [isExpanded, setIsExpanded] = useState(false)

  if (!agent?.tags?.length) return null

  return (
    <Accordion
      isExpanded={isExpanded}
      onToggle={() => setIsExpanded(!isExpanded)}
      text="View Tags"
      preset="reversed"
      style={style}
    >
      <View style={themed($contentContainer)}>
        <View style={$tagsContainer}>
          {agent.tags.map((tag) => (
            <View key={tag} style={themed($tagContainer)}>
              <Text text={tag} />
            </View>
          ))}
        </View>
      </View>
    </Accordion>
  )
}

const $contentContainer: ThemedStyle<ViewStyle> = ({ colors }) => ({
  padding: spacing.sm,
  backgroundColor: colors.transparent50,
  borderRadius: spacing.xs,
})

const $tagsContainer: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  gap: spacing.xs,
}

const $tagContainer: ThemedStyle<ViewStyle> = ({ colors }) => ({
  paddingHorizontal: spacing.sm,
  paddingVertical: spacing.xs,
  backgroundColor: colors.transparent50,
  borderRadius: spacing.xs,
})
