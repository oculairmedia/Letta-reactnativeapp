import { View, ViewStyle } from "react-native"
import { colors, spacing } from "@/theme"
import { ContextSegment } from "./types"

interface ContextBarProps {
  segments: ContextSegment[]
}

export function ContextBar({ segments }: ContextBarProps) {
  return (
    <View style={$container}>
      {segments.map((segment) => (
        <View
          key={segment.type}
          style={[
            $segment,
            {
              flex: segment.tokens,
              backgroundColor: segment.color,
            },
          ]}
        />
      ))}
    </View>
  )
}

const $container: ViewStyle = {
  flexDirection: "row",
  height: 10,
  backgroundColor: colors.textDim,
  borderRadius: spacing.sm,
  overflow: "hidden",
}

const $segment: ViewStyle = {
  height: "100%",
}
