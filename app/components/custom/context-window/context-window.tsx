import { Text } from "@/components/Text"
import { spacing } from "@/theme"
import { TextStyle, View, ViewStyle } from "react-native"
import { ContextBar } from "./context-bar"
import { ContextLegend } from "./context-legend"
import { ContextData } from "./types"
import { useContextSegments } from "./use-context-segments"

interface ContextWindowProps {
  contextData: ContextData | undefined
}

export function ContextWindow({ contextData }: ContextWindowProps) {
  const { sum, segments } = useContextSegments(contextData)

  return (
    <View style={$container}>
      <Text text={`${sum}/${contextData?.contextWindowSizeMax}`} style={$text} />
      <ContextBar segments={segments} />
      <ContextLegend segments={segments} />
    </View>
  )
}

const $container: ViewStyle = {
  gap: spacing.xs,
}

const $text: TextStyle = {
  textAlign: "right",
}
