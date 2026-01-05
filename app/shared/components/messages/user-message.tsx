"use client"
import { ViewStyle } from "react-native"
import { Card, CardProps } from "@/components/Card"
import { spacing } from "@/theme"

interface UserMessageProps extends CardProps {}
export function UserMessage({ content, style, ...props }: UserMessageProps) {
  return <Card style={[$userMessageContainer, style]} content={content} {...props} />
}

const $userMessageContainer: ViewStyle = {
  flexWrap: "wrap",
  flexDirection: "row",
  maxWidth: "100%",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.sm,
  alignItems: "flex-end",
  minHeight: 0,
}
