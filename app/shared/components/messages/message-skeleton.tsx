import { ViewStyle } from "react-native"

import Animated, { withDelay, withTiming } from "react-native-reanimated"

import { useEffect } from "react"

import { useSharedValue, withSequence, withRepeat, useAnimatedStyle } from "react-native-reanimated"
import { View } from "react-native"
import { Card } from "@/components/Card"
import { colors, spacing } from "@/theme"

export const MessageSkeleton = ({
  isUser = false,
  index = 0,
}: {
  isUser?: boolean
  index?: number
}) => {
  const opacity = useSharedValue(0.3)

  useEffect(() => {
    opacity.value = withRepeat(
      withSequence(
        withDelay(index * 500, withTiming(0.5, { duration: 1000 })),
        withTiming(0.3, { duration: 1000 }),
      ),
      -1,
      true,
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const animatedStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
  }))

  return (
    <Animated.View
      style={[animatedStyle, $skeletonContainer, isUser ? $skeletonUser : $skeletonAssistant]}
    >
      <Card style={$skeletonCard}>
        <View style={$skeletonContent}>
          <View style={$skeletonLine} />
          <View style={$skeletonLineMedium} />
          <View style={$skeletonLineShort} />
        </View>
      </Card>
    </Animated.View>
  )
}

const $skeletonContainer: ViewStyle = {
  width: "100%",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.xs,
}

const $skeletonUser: ViewStyle = {
  alignItems: "flex-end",
}

const $skeletonAssistant: ViewStyle = {
  alignItems: "flex-start",
}

const $skeletonCard: ViewStyle = {
  maxWidth: "80%",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.sm,
  backgroundColor: colors.palette.neutral100,
  borderColor: colors.palette.neutral100,
}

const $skeletonContent: ViewStyle = {
  gap: spacing.xs,
}

const $skeletonLine: ViewStyle = {
  height: 12,
  backgroundColor: colors.border,
  borderRadius: 6,
  width: "100%",
}

const $skeletonLineMedium: ViewStyle = {
  ...$skeletonLine,
  width: "80%",
}

const $skeletonLineShort: ViewStyle = {
  ...$skeletonLine,
  width: "60%",
}
