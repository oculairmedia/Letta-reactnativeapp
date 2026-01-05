import { colors, spacing } from "@/theme"
import { Loader } from "lucide-react-native"
import { useMemo } from "react"
import { ImageStyle } from "react-native"
import Animated, {
  SharedValue,
  useAnimatedStyle,
  useDerivedValue,
  useSharedValue,
  withRepeat,
  withTiming,
} from "react-native-reanimated"

const AnimatedLoader = ({ isVisible }: { isVisible: SharedValue<boolean> }) => {
  const rotation = useDerivedValue(() => {
    if (isVisible.value) {
      return withRepeat(withTiming(360, { duration: 1000 }), -1, false)
    }
    return withTiming(0, { duration: 200 })
  })

  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        { translateY: withTiming(isVisible.value ? -0.25 : 0, { duration: 200 }) },
        { rotate: `${rotation.value}deg` },
      ],
      opacity: withTiming(isVisible.value ? 1 : 0, { duration: 200 }),
    }
  })

  return (
    <Animated.View style={[animatedStyle, $loaderStyle]}>
      <Loader size={24} color={colors.tint} />
    </Animated.View>
  )
}

const $loaderStyle: ImageStyle = {
  position: "absolute",
  bottom: spacing.xxxl,
  end: spacing.md,
}

export const useAnimatedLoader = () => {
  const AnimatedIsVisible = useSharedValue(false)
  return useMemo(
    () =>
      [
        AnimatedIsVisible,
        () => {
          return <AnimatedLoader isVisible={AnimatedIsVisible} />
        },
      ] as const,
    [AnimatedIsVisible],
  )
}
