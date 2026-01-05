import { View, ViewStyle } from "react-native"
import { Text } from "@/components/Text"
import { spacing } from "@/theme"
import { ContextSegment } from "./types"
import { TextStyle } from "react-native"

interface ContextLegendProps {
  segments: ContextSegment[]
}

export function ContextLegend({ segments }: ContextLegendProps) {
  return (
    <View style={$container}>
      {segments.map((segment) => (
        <View key={segment.type} style={$item}>
          <View style={[$dot, { backgroundColor: segment.color }]} />
          <Text text={`${segment.type} (${segment.tokens})`} style={$text} />
        </View>
      ))}
    </View>
  )
}

const $container: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  gap: spacing.xs,
  marginTop: spacing.xs,
}

const $item: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.xxs,
}

const $dot: ViewStyle = {
  width: 8,
  height: 8,
  borderRadius: 4,
}

const $text: TextStyle = {
  fontSize: 10,
  textTransform: "capitalize",
}
