import { ViewStyle } from "react-native"

import Animated, { withDelay, withTiming } from "react-native-reanimated"

import { useEffect } from "react"

import { Card } from "@/components/Card"
import { colors } from "@/theme"
import { useAnimatedStyle, useSharedValue, withRepeat, withSequence } from "react-native-reanimated"

export const Skeleton = ({ index = 0 }: { index?: number }) => {
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
    <Animated.View style={[animatedStyle, $skeletonContainer]}>
      <Card style={$skeletonCard} />
    </Animated.View>
  )
}

const $skeletonContainer: ViewStyle = {
  width: "100%",
}

const $skeletonCard: ViewStyle = {
  maxWidth: "100%",
  backgroundColor: colors.palette.neutral100,
  borderColor: colors.palette.neutral100,
}
