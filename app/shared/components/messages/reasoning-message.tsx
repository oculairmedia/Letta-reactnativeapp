import { ViewStyle } from "react-native"
import { Card, CardProps } from "@/components/Card"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
interface ReasoningMessageProps extends CardProps {}
export function ReasoningMessage({ content, style, ...props }: ReasoningMessageProps) {
  const { themed } = useAppTheme()

  return <Card style={themed([$reasoningMessage, style])} content={"🤔 " + content} {...props} />
}

const $reasoningMessage: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flexWrap: "wrap",
  flexDirection: "row",
  maxWidth: "100%",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.sm,
  alignItems: "flex-start",
  minHeight: 0,
  backgroundColor: colors.elementColors.card.default.backgroundColor,
  borderLeftWidth: 4,
  borderLeftColor: colors.elementColors.card.default.content,
})
