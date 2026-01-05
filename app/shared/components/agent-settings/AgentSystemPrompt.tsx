import { Text } from "@/components/Text"
import { useAgent } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { Accordion } from "@/shared/components/animated/Accordion"
import { FC, useState } from "react"
import { View, ViewStyle } from "react-native"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"

interface AgentSystemPromptProps {
  style?: ViewStyle
}

export const AgentSystemPrompt: FC<AgentSystemPromptProps> = ({ style }) => {
  const { themed } = useAppTheme()
  const [agentId] = useAgentId()
  const { data: agent } = useAgent(agentId)
  const [isExpanded, setIsExpanded] = useState(false)

  if (!agent?.system) return null

  return (
    <Accordion
      isExpanded={isExpanded}
      onToggle={() => setIsExpanded(!isExpanded)}
      text="View System Prompt"
      preset="reversed"
      style={style}
    >
      <View style={themed($contentContainer)}>
        <Text text={agent.system} />
      </View>
    </Accordion>
  )
}

const $contentContainer: ThemedStyle<ViewStyle> = ({ colors }) => ({
  padding: spacing.sm,
  backgroundColor: colors.transparent50,
  borderRadius: spacing.xs,
})
