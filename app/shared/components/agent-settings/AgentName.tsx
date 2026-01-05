import { Text } from "@/components/Text"
import { useAgent } from "@/hooks/use-agent"
import { useAgentId } from "@/hooks/use-agentId-param"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { FC } from "react"
import { View, ViewStyle } from "react-native"

interface AgentNameProps {
  style?: ViewStyle
}

export const AgentName: FC<AgentNameProps> = ({ style }) => {
  const { themed } = useAppTheme()
  const [agentId] = useAgentId()
  const { data: agent } = useAgent(agentId)

  if (!agent?.name) return null

  return (
    <View style={themed([$contentContainer, style])}>
      <Text text={agent.name} />
    </View>
  )
}

const $contentContainer: ThemedStyle<ViewStyle> = ({ colors }) => ({
  padding: spacing.sm,
  backgroundColor: colors.transparent50,
  borderRadius: spacing.xs,
})
