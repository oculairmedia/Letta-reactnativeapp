import { Text, TextProps } from "@/components"
import { CardAccordion } from "@/shared/components/animated/CardAccordion"
import { normalizeName } from "@/shared/utils/normalizers"
import { colors, spacing } from "@/theme"
import { Wrench } from "lucide-react-native"
import { useState } from "react"
import { View, ViewStyle } from "react-native"
import { MemoizedCodeEditor } from "../letta-code-editor"
interface ToolReturnMessageProps {
  content?: string
  style?: ViewStyle
  footer?: string
}

export function ToolReturnMessage({ content, style, footer }: ToolReturnMessageProps) {
  const [isExpanded, setIsExpanded] = useState(false)

  if (!content) return null

  return (
    <View style={[$container, style]}>
      <CardAccordion
        isExpanded={isExpanded}
        onToggle={() => setIsExpanded(!isExpanded)}
        style={$accordion}
        LeftAccessory={({ style: accessoryStyle }) => (
          <View style={[$toolCallFooter, accessoryStyle]}>
            <Wrench size={16} color={colors.tint} style={$toolIcon} />
            <Text text={normalizeName(footer)} style={$toolName as TextProps["style"]} />
          </View>
        )}
      >
        <MemoizedCodeEditor content={content} />
      </CardAccordion>
    </View>
  )
}

const $container: ViewStyle = {
  flexDirection: "row",
  alignItems: "flex-start",
  gap: spacing.xs,
}

const $toolIcon: ViewStyle = {
  marginTop: spacing.xs,
}

const $accordion: ViewStyle = {
  flex: 1,
}

const $toolName: TextProps["style"] = {
  color: colors.tint,
  fontWeight: "600",
}

const $toolCallFooter: ViewStyle = {
  flexDirection: "row",
  alignItems: "flex-start",
  gap: spacing.xs,
}
